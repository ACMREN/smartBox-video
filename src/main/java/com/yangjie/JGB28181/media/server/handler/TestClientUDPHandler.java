package com.yangjie.JGB28181.media.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class TestClientUDPHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    public void channelActive(ChannelHandlerContext context) {
        context.writeAndFlush(Unpooled.copiedBuffer("Netty rocks", CharsetUtil.UTF_8));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        System.out.println("Client received: UDP");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }
}
