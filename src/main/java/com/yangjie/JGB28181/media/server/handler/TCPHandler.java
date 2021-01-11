package com.yangjie.JGB28181.media.server.handler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.media.server.TCPServer;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.web.controller.ActionController;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yangjie.JGB28181.common.utils.BitUtils;
import com.yangjie.JGB28181.common.utils.HexStringUtils;
import com.yangjie.JGB28181.media.callback.OnChannelStatusListener;
import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.codec.Parser;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.springframework.util.CollectionUtils;

/**
 * 接受TCP音视频媒体流
 * @author yangjie
 * 2020年3月13日
 */
public class TCPHandler extends ChannelInboundHandlerAdapter{

	private Logger log = LoggerFactory.getLogger(getClass());

	private ConcurrentLinkedDeque<Frame> mFrameDeque = null;

	private boolean mIsCheckSsrc = false;

	private int mSsrc;

	private String deviceId;

	/**
	 * 第一帧是否为I帧
	 * 不为I帧，先丢弃
	 */
	private boolean mIsFirstI;

	private OnChannelStatusListener onChannelStatusListener;

	private Parser mParser;

	private String higherServerIp;

	private Integer higherServerPort;

	private Integer toHigherServer = 0;

	private Integer toPushStream = 0;

	private Bootstrap b = null;


	public void setOnChannelStatusListener(OnChannelStatusListener onChannelStatusListener) {
		this.onChannelStatusListener = onChannelStatusListener;
	}
	public TCPHandler(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc, boolean checkSsrc, String deviceId,
					  Integer deviceBaseId, Integer toPushStream, Integer toHigherServer, String higherServerIp, Integer higherServerPort,
			Parser parser) {
		this.mFrameDeque =frameDeque;
		this.mSsrc = ssrc;
		this.mParser = parser;
		this.deviceId = deviceId;
		this.toPushStream = toPushStream;
		this.toHigherServer = toHigherServer;
		this.higherServerIp = higherServerIp;
		this.higherServerPort = higherServerPort;
		String deviceProtocolKey = deviceBaseId.toString() + "_tcp";
		CacheUtil.deviceHandlerMap.put(deviceProtocolKey, this);
	}

	private List<Channel> channels = new ArrayList<>();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(mFrameDeque == null){
			log.error("frame deque can not null");
			return;
		}

		ByteBuf byteBuf = (ByteBuf) msg;
		// 1. 判断是否开启了推送到其它平台
		if (null != toHigherServer && toHigherServer == 1) {
			ByteBuf byteBuf1 = byteBuf.copy();
			// 1.1 判断通道是否开启
			if (CollectionUtils.isEmpty(channels)) {
				// 1.2 判断线程组是否已经开启
				if (null == b) {
					EventLoopGroup group = new NioEventLoopGroup();
					b = new Bootstrap();
					b.group(group);
					b.channel(NioSocketChannel.class);
					b.remoteAddress(new InetSocketAddress(higherServerIp, higherServerPort));
					b.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel socketChannel) {
							socketChannel.pipeline().addLast(new TestClientHandler());
						}
					});
				}
				ChannelFuture future = b.connect().sync();
				Channel channel = future.channel();
				channels.add(channel);
			}

			for (Channel item : channels) {
				item.writeAndFlush(byteBuf1);
			}
		}


		// 2. 判断是否开启了推流
		if (null != toPushStream && toPushStream == 1) {
			// 2.1 读取byteBuf数据到字节数组中
			int readableBytes = byteBuf.readableBytes();
			if(readableBytes <=0){
				return;
			}
			byte[] copyData = new byte[readableBytes];
			byteBuf.readBytes(copyData);
			try{
				if(mIsCheckSsrc){
					int uploadSsrc = BitUtils.byte4ToInt(copyData[10],copyData[11],copyData[12],copyData[13]);
					if(uploadSsrc != mSsrc){
						return;
					}
				}
				int seq = BitUtils.byte2ToInt(copyData[4],copyData[5]);
				Frame frame;
				//有ps头,判断是否是i帧或者p帧
				if(readableBytes > 18 && copyData[14] == 0 &&copyData[15] ==0 &&copyData[16] ==01 && (copyData[17]&0xff) == 0xba){
					//pack_stuffing_length
					int stuffingLength =  copyData[27] & 7;
					int startIndex = 27+stuffingLength+1;

					//有ps系统头为i帧
					if(copyData[startIndex] == 0 && copyData[startIndex+1] == 0&&copyData[startIndex+2] == 01&&(copyData[startIndex+3]&0xff) == 0xbb ){
						frame = new Frame(Frame.I);
						mIsFirstI = true;
					}
					//p帧
					else{
						if(!mIsFirstI){
							return;
						}
						frame = new Frame(Frame.P);
					}
					frame.addPacket(seq, copyData);
					frame.setFirstSeq(seq);
					mFrameDeque.add(frame);
				}
				//音频数据
				else if(readableBytes > 18 && copyData[14] == 0 &&copyData[15] ==0 &&copyData[16] ==01 && (copyData[17]&0xff) == 0xc0){
					if(!mIsFirstI){
						return;
					}
					frame = new Frame(Frame.AUDIO);
					frame.addPacket(seq, copyData);
					frame.setFirstSeq(seq);
					mFrameDeque.add(frame);
				}
				//分包数据
				else{
					if(mFrameDeque.size() >0 && mIsFirstI){
						frame = mFrameDeque.getLast();
						if(frame != null){
							frame .addPacket(seq, copyData);
							frame.setEndSeq(seq);
						}

						if(mFrameDeque.size() >1){
							Frame pop = mFrameDeque.pop();
							mParser.parseTcp(pop);
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
				if (!(e instanceof ArrayIndexOutOfBoundsException)) {
					PushStreamDeviceManager.mainMap.remove(deviceId);
					ctx.close();
				}
				log.error("TCPHandler 异常 >>> {}",HexStringUtils.toHexString(copyData));
			}finally {
				copyData = null;
				release(msg);
			}
		}
	}
	private void release(Object msg){
		try{
			ReferenceCountUtil.release(msg);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * TCP建立连接
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		if(onChannelStatusListener!= null){
			onChannelStatusListener.onConnect();
		}
	}
	/**
	 * TCP连接断开
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if(onChannelStatusListener!= null){
			onChannelStatusListener.onDisconnect();
		}
		ctx.close();
	}

	public void setToPushStream(Integer toPushStream) {
		this.toPushStream = toPushStream;
	}

	public Integer getToHigherServer() {
		return toHigherServer;
	}

	public void setToHigherServer(Integer toHigherServer) {
		this.toHigherServer = toHigherServer;
	}

	public void setHigherServerIp(String higherServerIp) {
		this.higherServerIp = higherServerIp;
	}

	public void setHigherServerPort(Integer higherServerPort) {
		this.higherServerPort = higherServerPort;
	}

	/**
	 * 注册到新的服务器进行视频流推送
	 * @param remoteIp
	 * @param remotePort
	 */
	public void connectNewRemoteAddress(String remoteIp, Integer remotePort) {
		if (null != b) {
			try {
				ChannelFuture future = b.connect(new InetSocketAddress(remoteIp, remotePort)).sync();
				Channel channel = future.channel();
				channels.add(channel);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
