package com.yangjie.JGB28181.message;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.thread.HeartbeatThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.GbClientInfo;
import com.yangjie.JGB28181.entity.GbServerInfo;
import com.yangjie.JGB28181.entity.bo.ServerInfoBo;
import com.yangjie.JGB28181.entity.condition.GBDevicePlayCondition;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetTypeEnum;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.handler.GBStreamHandler;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;
import com.yangjie.JGB28181.media.server.handler.UDPHandler;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.GbClientInfoService;
import com.yangjie.JGB28181.service.GbServerInfoService;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.bean.DeviceChannel;
import com.yangjie.JGB28181.bean.Host;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.message.helper.DigestServerAuthenticationHelper;
import com.yangjie.JGB28181.message.helper.SipContentHelper;
import com.yangjie.JGB28181.message.session.MessageManager;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Expires;

public class SipLayer implements SipListener{

	@Autowired
	private CameraInfoService cameraInfoService;

	@Autowired
	private GbServerInfoService gbServerInfoService;

	@Autowired
	private GbClientInfoService gbClientInfoService;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private SipStackImpl mSipStack;

	private AddressFactory mAddressFactory;

	private HeaderFactory mHeaderFactory;

	private MessageFactory mMessageFactory;

	private SipProvider mTCPSipProvider;
	private SipProvider mUDPSipProvider;

	private long mCseq = 1;
	private long mSN = 1;

	private String mLocalIp;
	private int mLocalPort;
	private String mSipId;
	private String mSipRealm;
	private String mPassword;
	private String mSsrcRealm;
	private String mStreamMediaIp;

	private DigestServerAuthenticationHelper mDigestServerAuthenticationHelper;
	public static final  String UDP = "UDP";
	public static final String TCP = "TCP";

	private static final String MESSAGE_CATALOG = "Catalog";
	private static final String MESSAGE_DEVICE_INFO = "DeviceInfo";
	private static final String MESSAGE_BROADCAST = "Broadcast";
	private static final String MESSAGE_DEVICE_STATUS = "DeviceStatus";
	private static final String MESSAGE_KEEP_ALIVE = "Keepalive";
	private static final String MESSAGE_MOBILE_POSITION = "MobilePosition";
	private static final String MESSAGE_MOBILE_POSITION_INTERVAL = "Interval";
	private static final String QNAME_QUERY = "Query";
	private static final String QNAME_RESPONSE = "Response";

	private static final String ELEMENT_DEVICE_ID = "DeviceID";
	private static final String ELEMENT_DEVICE_LIST = "DeviceList";
	private static final String ELEMENT_NAME = "Name";
	private static final String ELEMENT_STATUS = "Status";
	private static final String ELEMENT_ADDRESS = "Address";
	private static final String ELEMENT_LONGITUDE = "longitude";
	private static final String ELEMENT_LATITUDE = "latitude";
	private static final String ELEMENT_PROJECT = "project";

	public static final String CLIENT_DEVICE_PREFIX = "client_";
	public static final String SUB_DEVICE_PREFIX = "sub_";
	public static final String SERVER_DEVICE_PREFIX = "server_";

	private static final int STREAM_MEDIA_START_PORT = 20000;
	private static final int STREAM_MEDIA_END_PORT = 21000;
	public static  int mStreamPort = STREAM_MEDIA_START_PORT;


	private static final int SSRC_TYPE_PLAY = 0;
	private static final int SSRC_TYPE_HISTORY = 1;
	private static final int SSRC_MIN_VALUE = 0;
	private static final int SSRC_MAX_VALUE = 9999;
	private int mSsrc;

	public static final String SESSION_NAME_PLAY = "Play";
	public static final String SESSION_NAME_DOWNLOAD = "Download";
	public static final String SESSION_NAME_PLAY_BACK = "Playback";

	private int expiredTime = 180;

	private MessageManager mMessageManager = MessageManager.getInstance();
	private PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	private ServerInfoBo connectServerInfo = new ServerInfoBo();

	private static Map<String, String> deviceCatalogMap = new HashMap<>(20);

	private static Map<String, JSONObject> inviteCallIdMap = new HashMap<>(20);

	private static Map<String, String> keepAliveCallIdMap = new HashMap<>(20);

	public static Map<String, Integer> higherServerExpiredTimeMap = new HashMap<>(20);

	public static Map<String, Thread> higherServerHeartbeatMap = new HashMap<>(20);

	public SipLayer(String sipId,String sipRealm,String password,String localIp,int localPort,String streamMediaIp){
		this.mSipId = sipId;
		this.mLocalIp= localIp;
		this.mLocalPort = localPort;
		this.mSipRealm = sipRealm;
		this.mPassword = password;
		this.mSsrcRealm = mSipId.substring(3,8);
		this.mStreamMediaIp = streamMediaIp;
	}
	public boolean startServer(){
		return initSip();
	}
	@SuppressWarnings("deprecation")
	private boolean initSip() {
		try {
			SipFactory sipFactory = SipFactory.getInstance();
			Properties properties = new Properties();
			properties.setProperty("javax.sip.STACK_NAME", "GB28181_SIP");
			properties.setProperty("javax.sip.IP_ADDRESS", mLocalIp);
			mSipStack = (SipStackImpl) sipFactory.createSipStack(properties);

			mHeaderFactory = sipFactory.createHeaderFactory();
			mAddressFactory = sipFactory.createAddressFactory();
			mMessageFactory = sipFactory.createMessageFactory();
			mDigestServerAuthenticationHelper = new DigestServerAuthenticationHelper();
			//同时监听UDP和TCP
			try {
				ListeningPoint tcpListeningPoint = mSipStack.createListeningPoint(mLocalIp, mLocalPort,"TCP");

				ListeningPoint udpListeningPoint = mSipStack.createListeningPoint(mLocalIp, mLocalPort,"UDP");

				mTCPSipProvider = mSipStack.createSipProvider(tcpListeningPoint);
				mTCPSipProvider.addSipListener(this);

				mUDPSipProvider = mSipStack.createSipProvider(udpListeningPoint);
				mUDPSipProvider.addSipListener(this);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * 终端发送的Request信令
	 */
	public void processRequest(RequestEvent evt) {
		Request request = evt.getRequest();
		String method = request.getMethod();
		logger.info("processRequest >>> {}",request);
		try{
			if(method.equalsIgnoreCase(Request.REGISTER)){
				processRegister(evt);
			}else if(method.equalsIgnoreCase(Request.MESSAGE)){
				processMessage(evt);
			}else if(method.equalsIgnoreCase(Request.BYE)){
				processBye(evt);
			} else if (method.equalsIgnoreCase(Request.INVITE)) {
				processInvite(evt);
			} else if (method.equalsIgnoreCase(Request.ACK)) {
				CacheUtil.executor.execute(() -> {
					processAck(evt);
				});
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void processAck(RequestEvent event) {
		Request request = event.getRequest();
		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader("call-id");
		String callId = callIdHeader.getCallId();
		if (inviteCallIdMap.containsKey(callId)) {
			JSONObject inviteJson = inviteCallIdMap.get(callId);
			String deviceId = inviteJson.getString("deviceId");
			String ip = inviteJson.getString("ip");
			Integer port = inviteJson.getInteger("port");
			String protocol = inviteJson.getString("protocol");

			CameraInfo cameraInfo = cameraInfoService.getBaseMapper().selectOne(new QueryWrapper<CameraInfo>().eq("device_serial_num", deviceId));
			Integer deviceBaseId = cameraInfo.getDeviceBaseId();

			try {
				cameraInfoService.gbDevicePlay(new GBDevicePlayCondition(deviceBaseId, deviceId, deviceId, protocol, 0, null, 1, 0, 0, 0, 0, 1, ip, port, callId));
			} catch (Exception e) {
				e.printStackTrace();
			}
			inviteCallIdMap.remove(callId);
		}
	}

	private void processInvite(RequestEvent event) throws Exception {
		Request request = event.getRequest();

		ServerTransaction serverTransaction = sendTrying(event, request);
		sendInviteOK(event, request, serverTransaction);
	}


	private void processBye(RequestEvent evt) throws Exception{
		ServerTransaction serverTransaction = evt.getServerTransaction();
		Dialog dialog = serverTransaction != null ? serverTransaction.getDialog() : null;

		// 1. 获取请求头中的关键参数
		Request request = evt.getRequest();
		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
		String higherCallId = callIdHeader.getCallId();
		ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
		Address address = toHeader.getAddress();
		String uriStr = address.getURI().toString();
		String deviceId = uriStr.split(":")[1].split("@")[0];

		// 2. 根据参数断开级联平台的推送流
		CameraInfo cameraInfo = cameraInfoService.getBaseMapper().selectOne(new QueryWrapper<CameraInfo>().eq("device_serial_num", deviceId));
		Integer deviceBaseId = cameraInfo.getDeviceBaseId();
		String deviceTcpKey = deviceBaseId.toString() + "_tcp";
		String deviceUdpKey = deviceBaseId.toString() + "_udp";
		if (CacheUtil.gbServerMap.get(deviceTcpKey) != null) {
			Server server = CacheUtil.gbServerMap.get(deviceTcpKey);
			GBStreamHandler handler = CacheUtil.deviceHandlerMap.get(deviceTcpKey);
			this.stopPushStreamToHigherServer(server, handler, higherCallId, deviceTcpKey);
		}
		if (CacheUtil.gbServerMap.get(deviceUdpKey) != null) {
			Server server = CacheUtil.gbServerMap.get(deviceUdpKey);
			GBStreamHandler handler = CacheUtil.deviceHandlerMap.get(deviceUdpKey);
			this.stopPushStreamToHigherServer(server, handler, higherCallId, deviceUdpKey);
		}

		// 3. 通知上级平台关闭推送通道
		if(serverTransaction == null || dialog == null){
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		Response response = mMessageFactory.createResponse(Response.OK,request);
		serverTransaction.sendResponse(response);
	}

	private void stopPushStreamToHigherServer(Server server, GBStreamHandler handler, String higherCallId,
											  String deviceProtocolKey) throws SipException {
		Integer toPushStream = server.getToPushStream();
		if (null != toPushStream && toPushStream == 1) {
			if ((handler.callIdChannelMap.size() - 1) != 0) {
				handler.disconnectRemoteAddress(higherCallId);
			}
		} else {
			if ((handler.callIdChannelMap.size() - 1) != 0) {
				handler.disconnectRemoteAddress(higherCallId);
			} else {
				Dialog response = server.getResponse();
				CacheUtil.gbServerMap.remove(deviceProtocolKey);
				CacheUtil.deviceHandlerMap.remove(deviceProtocolKey);
				server.stopServer();
				Request byeRequest = response.createRequest(Request.BYE);
				ClientTransaction clientTransaction = (isTCP(byeRequest) ? mTCPSipProvider:mUDPSipProvider).getNewClientTransaction(byeRequest);
				response.sendRequest(clientTransaction);
				logger.info("sendRequest >>> {}",byeRequest);
			}
		}
	}

	private void processMessage(RequestEvent evt) throws Exception{
		Request request = evt.getRequest();
		SAXReader reader = new SAXReader();
		//reader.setEncoding("GB2312");
		ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
		Document xml = reader.read(new ByteArrayInputStream(request.getRawContent()));

		Element rootElement = xml.getRootElement();
		String name = rootElement.getQName().getName();
		String cmd = rootElement.element("CmdType").getStringValue();
		String sn = rootElement.element("SN").getStringValue();
		Element deviceIdElement = rootElement.element(ELEMENT_DEVICE_ID);
		if(deviceIdElement == null){
			return;
		}
		String deviceId = deviceIdElement.getText().toString();
		Response response= null;

		//心跳，
		//如果redis中设备在线，更新。
		//不在线回复400
		if(MESSAGE_KEEP_ALIVE.equals(cmd)){
			if(RedisUtil.checkExist(SUB_DEVICE_PREFIX + deviceId)){
				RedisUtil.expire(SUB_DEVICE_PREFIX + deviceId, expiredTime);
			}else {
				response = mMessageFactory.createResponse(Response.BAD_REQUEST,request);
			}
		}
		
		//目录响应，保存到redis
		else if(MESSAGE_CATALOG.equals(cmd) && QNAME_RESPONSE.equals(name)){
			FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
			String uri = fromHeader.getAddress().getURI().toString();
			String deviceSerialNum = uri.split("@")[0];
			String domain = uri.split("@")[1];
			Element parentSerialNumElement = rootElement.element(ELEMENT_DEVICE_ID);
			String parentSerialNum = parentSerialNumElement.getText().trim();

			Element deviceListElement = rootElement.element(ELEMENT_DEVICE_LIST);
			if(deviceListElement == null){
				return;
			}
			Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
			if(deviceListIterator != null){
				String deviceStr = RedisUtil.get(SUB_DEVICE_PREFIX + deviceId);
				if(StringUtils.isEmpty(deviceStr)){
					return ;
				}
				Device device = JSONObject.parseObject(deviceStr,Device.class);
				Map<String, DeviceChannel> channelMap = device.getChannelMap();
				if(channelMap == null){
					channelMap = new HashMap<String, DeviceChannel>(5);
					device.setChannelMap(channelMap);
				}

				ServerInfoBo clientInfo = DeviceManagerController.serverInfoBo;
				String clientId = clientInfo.getId();
				String subDeviceStr = RedisUtil.get(CLIENT_DEVICE_PREFIX + clientId);
				Map<String, Device> subDeviceMap = JSONObject.parseObject(subDeviceStr, HashMap.class);
				if (null == subDeviceMap) {
					subDeviceMap = new HashMap<>();
				}
				Map<String, String> channelCatalogMap = device.getChannelCatalogMap();
				if (null == channelCatalogMap) {
					channelCatalogMap = new HashMap<>();
					device.setChannelCatalogMap(channelCatalogMap);
				}
				//遍历DeviceList
				List<Device> subDeviceList = new ArrayList<>();
				while (deviceListIterator.hasNext()) {
					Device subDevice = new Device();
					Element itemDevice = deviceListIterator.next();

					Element channelDeviceElement = itemDevice.element(ELEMENT_DEVICE_ID);

					if(channelDeviceElement == null){
						continue;
					}
					String channelDeviceId = channelDeviceElement.getText();
					Element channelNameElement = itemDevice.element(ELEMENT_NAME);
					String channelName = channelNameElement != null ? channelNameElement.getText():"";
					Element statusElement = itemDevice.element(ELEMENT_STATUS);
					String status = statusElement != null?statusElement.getText():"ON";
					Element addressElement = itemDevice.element(ELEMENT_ADDRESS);
					String address = addressElement != null? addressElement.getText() : "";
					Element lngElement = itemDevice.element(ELEMENT_LONGITUDE);
					String lng = lngElement != null? lngElement.getText() : "";
					Element latElement = itemDevice.element(ELEMENT_LATITUDE);
					String lat = latElement != null? latElement.getText() : "";

					DeviceChannel deviceChannel = channelMap.containsKey(channelDeviceId)?channelMap.get(channelDeviceId):new DeviceChannel();
					deviceChannel.setName(channelName);
					deviceChannel.setDeviceId(channelDeviceId);
					deviceChannel.setStatus(status.equals("ON")?DeviceConstants.ON_LINE:DeviceConstants.OFF_LINE);

					// 把下属设备的item属性放入map中
					String itemContent = itemDevice.asXML();
					channelCatalogMap.put(channelDeviceId, itemContent);

					channelMap.put(channelDeviceId, deviceChannel);

					Host host = new Host();
					host.setWanPort(viaHeader.getPort());
					host.setWanIp(viaHeader.getHost());
					host.setAddress(viaHeader.getHost() + ":" + viaHeader.getPort());
					subDevice.setHost(host);
					subDevice.setChannelId(itemContent);
					subDevice.setParentSerialNum(parentSerialNum);
					subDevice.setDeviceId(channelDeviceId);
					subDeviceList.add(subDevice);
				}
				device.setSubDeviceList(subDeviceList);
				subDeviceMap.put(deviceId, device);

				GbClientInfo gbClientInfo = gbClientInfoService.getOne(new QueryWrapper<GbClientInfo>().eq("device_serial_num", deviceSerialNum));
				if (gbClientInfo == null) {
					gbClientInfo = new GbClientInfo();
					gbClientInfo.setCreateTime(LocalDateTime.now());
				}
				gbClientInfo.setIp(viaHeader.getHost());
				gbClientInfo.setAddress(null);
				gbClientInfo.setDeviceSerialNum(deviceSerialNum);
				gbClientInfo.setDomain(domain);
				gbClientInfo.setLinkType(LinkTypeEnum.GB28181.getCode());
				gbClientInfo.setNetType(NetTypeEnum.IT.getCode());
				gbClientInfo.setLastUpdateTime(LocalDateTime.now());
				gbClientInfoService.saveOrUpdate(gbClientInfo);

				//更新Redis
				RedisUtil.set(SUB_DEVICE_PREFIX + deviceId, JSONObject.toJSONString(device));
				RedisUtil.set(CLIENT_DEVICE_PREFIX + clientId, JSONObject.toJSONString(subDeviceMap));
			}
		} else if (MESSAGE_CATALOG.equals(cmd) && QNAME_QUERY.equals(name)) {
			String callId = IDUtils.id();
			String fromTag = IDUtils.id();
			sendResponseCatalog(connectServerInfo.getId(), connectServerInfo.getDomain(), connectServerInfo.getHost(),
					connectServerInfo.getPort(), connectServerInfo.getPw(), callId, fromTag, sn, 1);
		}
		if(response == null){
			response = mMessageFactory.createResponse(Response.OK,request);
		}
		sendResponse(response, getServerTransaction(evt));

	}
	private ServerTransaction getServerTransaction(RequestEvent evt) throws Exception{
		ServerTransaction serverTransaction = evt.getServerTransaction();
		if(serverTransaction == null){
			Request request = evt.getRequest();
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		return serverTransaction;

	}
	private void processRegister(RequestEvent evt) throws Exception{
		Request request = evt.getRequest();
		ServerTransaction serverTransaction = evt.getServerTransaction();
		boolean isTcp = isTCP(request);
		if(serverTransaction == null){
			serverTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewServerTransaction(request);
		}
		Response response = null;
		boolean passwordCorrect = false;
		boolean isRegisterSuceess = false;
		Device device = null;
		Header header = request.getHeader(AuthorizationHeader.NAME);
		//携带授权头
		//校验密码是否正确
		if(header != null){
			passwordCorrect = mDigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request,mPassword);
			if(!passwordCorrect){
				logger.info("密码错误");
			}
		}

		//未携带授权头或者密码错误 回复401
		if(header == null || !passwordCorrect){
			response = mMessageFactory.createResponse(Response.UNAUTHORIZED, request);
			mDigestServerAuthenticationHelper.generateChallenge(mHeaderFactory, response, mSipRealm);
		}
		//携带授权头并且密码正确
		else if (header != null && passwordCorrect){
			response = mMessageFactory.createResponse(Response.OK, request);
			//添加date头
			response.addHeader(mHeaderFactory.createDateHeader(Calendar.getInstance(Locale.ENGLISH)));
			ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(Expires.NAME);
			expiredTime = expiresHeader.getExpires();
			//添加Contact头
			response.addHeader(request.getHeader(ContactHeader.NAME));
			//添加Expires头
			response.addHeader(request.getExpires());
			//注销成功
			if(expiresHeader != null && expiresHeader.getExpires() == 0){

			}
			//注册成功
			else{
				isRegisterSuceess = true;
				//1.获取到通信地址等信息，保存到Redis
				FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
				ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
				CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
				Host host = getHost(viaHeader);
				String deviceId = getDeviceId(fromHeader);
				String callId = callIdHeader.getCallId();
				device = new Device();
				device.setDeviceId(deviceId);
				device.setHost(host);
				device.setProtocol(isTcp?TCP:UDP);
				// 设备类型默认为摄像头
				if (callId.contains ("platform")) {
					// 如果含有platform标识，则为平台
					device.setDeviceType("platform");
				} else {
					device.setDeviceType("camera");
				}
			}
		}
		sendResponse(response,serverTransaction);
		//注册成功
		//1.保存到redis
		//2.下发catelog查询目录
		if(isRegisterSuceess && device != null){
			String callId = IDUtils.id();
			String fromTag = IDUtils.id();
			RedisUtil.set(SipLayer.SUB_DEVICE_PREFIX + device.getDeviceId(), expiredTime, JSONObject.toJSONString(device));
			sendCatalog(device, callId, fromTag, mCseq, String.valueOf(mSN));
		}
	}

	public void sendCatalog(Device device,String callId,String fromTag,long cseq,String sn) throws Exception{
		Host host = device.getHost();
		String deviceId = device.getDeviceId();
		Request request = createRequest(deviceId,host.getAddress(),host.getWanIp(),host.getWanPort(),device.getProtocol(),
				mSipId,mSipRealm,fromTag,
				deviceId,mSipRealm,null,
				callId,cseq,Request.MESSAGE);
		String catalogContent = SipContentHelper.generateCatalogContent(deviceId, sn);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(catalogContent, contentTypeHeader);
		request.addHeader(contentTypeHeader);

		sendRequest(request);
	}

	public void sendInvite(Device device,String sessionName,String callId,String channelId,int port,String ssrc,
			boolean isTcp, String receiverSerialNum, String receiverAddress) throws Exception{
		String fromTag = IDUtils.id();
		Host host = device.getHost();
		String realm = channelId.substring(0,8);
		Request request = createRequest(receiverSerialNum,receiverAddress,host.getWanIp(),host.getWanPort(),device.getProtocol(),
				mSipId,mSipRealm,fromTag,
				channelId,realm,null,
				callId,20,Request.INVITE);
		//添加Concat头
		Address concatAddress = mAddressFactory.createAddress(mAddressFactory.createSipURI(mSipId, mLocalIp.concat(":").concat(String.valueOf(mLocalPort))));
		request.addHeader(mHeaderFactory.createContactHeader(concatAddress));
		//添加消息体
		String content = SipContentHelper.generateRealTimeMeidaStreamInviteContent(mSipId,mStreamMediaIp,port,isTcp,false,sessionName,ssrc);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "SDP");
		request.setContent(content, contentTypeHeader);
		//request.addHeader(contentTypeHeader);
		sendRequest(request);
	}

	public void sendRegister(String serverId, String serverDomain, String serverIp, String serverPort, String password,
							 String callId, String fromTag, String responseParam, String nonce, String uri, long cseq)
			throws SipException, ParseException, InvalidArgumentException {
		ServerInfoBo clientInfo = DeviceManagerController.serverInfoBo;
		String serverAddress = serverIp + ":" + serverPort;
		Request request = createRequest(serverId, serverAddress, clientInfo.getHost(), Integer.valueOf(clientInfo.getPort()), "UDP",
				clientInfo.getId(), clientInfo.getDomain(), fromTag, serverId, clientInfo.getDomain(), null,
				callId, cseq, Request.REGISTER);
		if (!StringUtils.isEmpty(responseParam)) {
			// 如果失败，每一秒发一次，避免太快
			// 设置认证头
			AuthorizationHeader authorizationHeader = mHeaderFactory.createAuthorizationHeader(AuthorizationHeader.NAME);
			authorizationHeader.setScheme("Digest");
			authorizationHeader.setUsername(connectServerInfo.getId());
			authorizationHeader.setRealm(connectServerInfo.getDomain());
			authorizationHeader.setNonce(nonce);
			authorizationHeader.setURI(mAddressFactory.createURI(uri));
			authorizationHeader.setResponse(responseParam);
			authorizationHeader.setAlgorithm("MD5");

			// 设置关联头
			Address contactAddress = mAddressFactory.createAddress(clientInfo.getId(), mAddressFactory.createURI(uri));
			ContactHeader contactHeader = mHeaderFactory.createContactHeader(contactAddress);

			// 设置过期头，一小时后过期
			ExpiresHeader expiresHeader = mHeaderFactory.createExpiresHeader(3600);

			// 把头部信息放到请求中
			request.addHeader(authorizationHeader);
			request.addHeader(contactHeader);
			request.addHeader(expiresHeader);
		}
		connectServerInfo.setHost(serverIp);
		connectServerInfo.setPort(serverPort);
		connectServerInfo.setId(serverId);
		connectServerInfo.setDomain(serverDomain);
		connectServerInfo.setPw(password);

		// 把级联的服务器放入到redis中
		Device device = new Device();
		device.setDeviceId(serverId);
		Host host = new Host();
		host.setWanIp(serverIp);
		host.setWanPort(Integer.valueOf(serverPort));
		host.setAddress(serverIp + ":" + serverPort);
		device.setHost(host);
		device.setProtocol("UDP");
		String connectServerJson = JSONObject.toJSONString(device);
		String key = SipLayer.SERVER_DEVICE_PREFIX + serverId;
		Integer expireTime = higherServerExpiredTimeMap.get(SERVER_DEVICE_PREFIX + serverId);
		RedisUtil.set(key, expireTime, connectServerJson);
		SipLayer.higherServerExpiredTimeMap.put(key, expireTime);

		sendRequest(request);
	}

	public void sendResponseCatalog(String serverId, String serverDomain, String serverIp, String serverPort, String password,
									String callId, String fromTag, String sn, long cseq) throws ParseException, InvalidArgumentException, SipException {
		ServerInfoBo clientInfo = DeviceManagerController.serverInfoBo;
		// 把该设备的下属设备catalog信息拿出来
		Set<String> subDeviceCatalogSet = new HashSet<>();

		GbServerInfo gbServerInfo = gbServerInfoService.getOne(new QueryWrapper<GbServerInfo>().eq("device_serial_num", serverId));
		String subDevicesStr = RedisUtil.get(CLIENT_DEVICE_PREFIX + clientInfo.getId());
		JSONObject subDevicesJson = JSONObject.parseObject(subDevicesStr);
		String cameraList = "85";
		String[] cameraArr = cameraList.split(",");
		if (null != callId && cameraArr.length > 0) {
			for (String item : cameraArr) {
				if (!StringUtils.isEmpty(item)) {
					CameraInfo cameraInfo = cameraInfoService.getOne(new QueryWrapper<CameraInfo>().eq("device_base_id", Integer.valueOf(item)));
					String parentSerialNum = cameraInfo.getParentSerialNum();
					String deviceSerialNum = cameraInfo.getDeviceSerialNum();
					String subDeviceStr = subDevicesJson.getString(parentSerialNum);
					Device subDevice = JSONObject.parseObject(subDeviceStr, Device.class);
					Map<String, String> channelCatalogMap = subDevice.getChannelCatalogMap();
					if (channelCatalogMap.containsKey(deviceSerialNum)) {
						subDeviceCatalogSet.add(channelCatalogMap.get(deviceSerialNum));
					}
				}
			}
		}

		String serverAddress = serverIp + ":" + serverPort;
		Request request = createRequest(serverId, serverAddress, clientInfo.getHost(), Integer.valueOf(clientInfo.getPort()), "UDP",
				clientInfo.getId(), clientInfo.getDomain(), fromTag, serverId, serverDomain, null, callId, cseq, Request.MESSAGE);
		String responseCatalogContent = SipContentHelper.generateResponseCatalogContent(clientInfo.getId(), sn, false, subDeviceCatalogSet);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(responseCatalogContent, contentTypeHeader);

		sendRequest(request);
	}

	/**
	 * 发送存活的消息包
	 * @param cseq
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void sendKeepAlive(long cseq) {
		try {
			String callId = IDUtils.id();
			String fromTag = IDUtils.id();
			keepAliveCallIdMap.put(callId, "keepAlive");
			ServerInfoBo clientInfo = DeviceManagerController.serverInfoBo;
			// 获取级联的上级服务器的参数
			String serverAddress = connectServerInfo.getHost() + ":" + connectServerInfo.getPort();
			String serverId = connectServerInfo.getId();
			Request request = createRequest(serverId, serverAddress, clientInfo.getHost(), Integer.valueOf(clientInfo.getPort()), "UDP",
					clientInfo.getId(), clientInfo.getDomain(), fromTag, serverId, clientInfo.getDomain(), null,
					callId, cseq, Request.MESSAGE);
			// 设置存活的消息体
			String keepAliveContent = SipContentHelper.generateKeepAliveContent(clientInfo.getId(), "20");
			// 设置请求头类型
			ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "MANSCDP+xml");
			request.setContent(keepAliveContent, contentTypeHeader);
			request.addHeader(contentTypeHeader);

			sendRequest(request);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}
	}

	public ServerTransaction sendTrying(RequestEvent event, Request request) throws Exception {
		Response response = mMessageFactory.createResponse(Response.TRYING, request);
		ServerTransaction serverTransaction = getServerTransaction(event);
		sendResponse(response, serverTransaction);
		return serverTransaction;
	}

	public void sendInviteOK(RequestEvent event, Request request, ServerTransaction serverTransaction) throws Exception {
		// 1. 取出设备编号id
		ToHeader toHeader = (ToHeader) request.getHeader("to");
		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader("call-id");
		String callId = callIdHeader.getCallId();
		Address address = toHeader.getAddress();
		String uriStr = address.getURI().toString();
		String deviceId = uriStr.split(":")[1].split("@")[0];

		// 2. 取出ip地址和端口号
		String content = new String(request.getRawContent());
		String ip = content.split("c=IN IP4 ")[1].split("\r\n")[0];
		Integer port = Integer.valueOf(content.split("video ")[1].split(" ")[0]);
		String ssrc = content.split("y=")[1].trim();
		Pattern pattern = Pattern.compile("TCP");
		Matcher matcher = pattern.matcher(content);
		Boolean isTcp = matcher.find();

		// 3. 把invite的关键信息放入到map中
		JSONObject inviteInfo = new JSONObject();
		inviteInfo.put("deviceId", deviceId);
		inviteInfo.put("ip", ip);
		inviteInfo.put("port", port);
		inviteInfo.put("protocol", isTcp? "TCP" : "UDP");
		inviteCallIdMap.put(callId, inviteInfo);

		// 4. 设置ok中的返回内容
		String okContent = SipContentHelper.generateInviteMediaOKContent(connectServerInfo.getId(), connectServerInfo.getHost(),
				Integer.valueOf(connectServerInfo.getPort()), connectServerInfo.getPw(), SESSION_NAME_PLAY, ssrc);
		ContentTypeHeader contentTypeHeader = mHeaderFactory.createContentTypeHeader("Application", "SDP");

		// 5. 创建response
		Response response = mMessageFactory.createResponse(Response.OK, request);
		Address concatAddress = mAddressFactory.createAddress(mAddressFactory.createSipURI(connectServerInfo.getId(),
				connectServerInfo.getHost().concat(":").concat(String.valueOf(connectServerInfo.getPort()))));
		ContactHeader contactHeader = mHeaderFactory.createContactHeader(concatAddress);
		response.setHeader(contactHeader);
		response.setContent(okContent, contentTypeHeader);

		sendResponse(response, serverTransaction);
	}

	private Response createResponse(String deviceId,String address,String targetIp,int targetPort,String protocol,
									String fromUserInfo, String fromHostPort, String fromTag,
									String toUserInfo, String toHostPort, String toTag,
									String callId, long cseqNo, Integer response, String method) throws ParseException, InvalidArgumentException {
		//请求行
		SipURI requestLine = mAddressFactory.createSipURI(deviceId, address);
		//Via头
		ArrayList viaHeaderList = new ArrayList();
		ViaHeader viaHeader = mHeaderFactory.createViaHeader(targetIp, targetPort, protocol, null);
		viaHeader.setRPort();
		viaHeaderList.add(viaHeader);

		//To头
		SipURI toAddress = mAddressFactory.createSipURI(toUserInfo, toHostPort);
		Address toNameAddress = mAddressFactory.createAddress(toAddress);
		ToHeader toHeader = mHeaderFactory.createToHeader(toNameAddress, toTag);

		//From头
		SipURI from = mAddressFactory.createSipURI(fromUserInfo, fromHostPort);
		Address fromNameAddress = mAddressFactory.createAddress(from);
		FromHeader fromHeader = mHeaderFactory.createFromHeader(fromNameAddress, fromTag);

		//callId
		CallIdHeader callIdHeader = protocol.equals(TCP)?mTCPSipProvider.getNewCallId():mUDPSipProvider.getNewCallId();
		callIdHeader.setCallId(callId);

		//Cseq
		CSeqHeader cSeqHeader = mHeaderFactory.createCSeqHeader(cseqNo, method);

		//Max_forward
		MaxForwardsHeader maxForwardsHeader = mHeaderFactory.createMaxForwardsHeader(70);

		return mMessageFactory.createResponse(response, callIdHeader, cSeqHeader, fromHeader, toHeader, null, maxForwardsHeader);
	}

	private Request createRequest(String deviceId,String address,String targetIp,int targetPort,String protocol,
			String fromUserInfo, String fromHostPort, String fromTag,
			String toUserInfo, String toHostPort, String toTag,
			String callId,
			long cseqNo, String method) throws ParseException, InvalidArgumentException {
		//请求行
		SipURI requestLine = mAddressFactory.createSipURI(deviceId, address);
		//Via头
		ArrayList viaHeaderList = new ArrayList();
		ViaHeader viaHeader = mHeaderFactory.createViaHeader(targetIp, targetPort, protocol, null);
		viaHeader.setRPort();
		viaHeaderList.add(viaHeader);

		//To头
		SipURI toAddress = mAddressFactory.createSipURI(toUserInfo, toHostPort);
		Address toNameAddress = mAddressFactory.createAddress(toAddress);
		ToHeader toHeader = mHeaderFactory.createToHeader(toNameAddress, toTag);

		//From头
		SipURI from = mAddressFactory.createSipURI(fromUserInfo, fromHostPort);
		Address fromNameAddress = mAddressFactory.createAddress(from);
		FromHeader fromHeader = mHeaderFactory.createFromHeader(fromNameAddress, fromTag);

		//callId
		CallIdHeader callIdHeader = protocol.equals(TCP)?mTCPSipProvider.getNewCallId():mUDPSipProvider.getNewCallId();;
		callIdHeader.setCallId(callId);

		//Cseq
		CSeqHeader cSeqHeader = mHeaderFactory.createCSeqHeader(cseqNo, method);

		//Max_forward
		MaxForwardsHeader maxForwardsHeader = mHeaderFactory.createMaxForwardsHeader(70);

		return mMessageFactory.createRequest(requestLine, method, callIdHeader, cSeqHeader, fromHeader, toHeader,
				viaHeaderList, maxForwardsHeader);

	}

	private boolean isTCP(Request request){
		return isTCP((ViaHeader)request.getHeader(ViaHeader.NAME));
	}
	private boolean isTCP(Response response){
		return isTCP((ViaHeader)response.getHeader(ViaHeader.NAME));
	}
	private boolean isTCP(ViaHeader viaHeader){
		String protocol = viaHeader.getProtocol();
		return protocol.equals("TCP");
	}
	private String getDeviceId(FromHeader fromHeader){
		AddressImpl address = (AddressImpl) fromHeader.getAddress();
		SipUri uri = (SipUri) address.getURI();
		String user = uri.getUser();
		return user;
	}
	private Host getHost(ViaHeader viaHeader){
		String received = viaHeader.getReceived();
		int rPort = viaHeader.getRPort();
		//本地模拟设备 received 为空 rPort 为 -1
		//解析本地地址替代
		if(StringUtils.isEmpty(received) || rPort == -1){
			received = viaHeader.getHost();
			rPort = viaHeader.getPort();
		}
		Host host =new Host();
		host.setWanIp(received);
		host.setWanPort(rPort);
		host.setAddress(received.concat(":").concat(String.valueOf(rPort)));
		return host; 
	}
	private void sendResponse(Response response,ServerTransaction serverTransaction) throws Exception{
		logger.info("sendResponse >>> {}",response);
		serverTransaction.sendResponse(response);
	}
	private void sendRequest(Request request) throws SipException{
		logger.info("sendRequest >>> {}",request);
		ClientTransaction clientTransaction = (isTCP(request)?mTCPSipProvider:mUDPSipProvider).getNewClientTransaction(request);
		clientTransaction.sendRequest();
	}

	/**
	 * 终端发送的Response信令
	 * 服务器下发给终端的Request得到响应
	 */
	public void processResponse(ResponseEvent evt) {
		Response response = evt.getResponse();
		logger.info("processResponse >>> {}",response);
		CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
		String method = cseqHeader.getMethod();
		try{
			//实时流请求响应
			if(Request.INVITE.equals(method)){
				int statusCode = response.getStatusCode();
				//trying不会回复
				if(statusCode == Response.TRYING){

				}
				//成功响应
				//下发ack
				if(statusCode == Response.OK){
					ClientTransaction clientTransaction = evt.getClientTransaction();
					if(clientTransaction == null){
						logger.error("回复ACK时，clientTransaction为null >>> {}",response);
						return;
					}
					Dialog clientDialog = clientTransaction.getDialog();

					CSeqHeader clientCSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
					long cseqId = clientCSeqHeader.getSeqNumber();
					/*
					createAck函数，创建的ackRequest，会采用Invite响应的200OK，中的contact字段中的地址，作为目标地址。
					有的终端传上来的可能还是内网地址，会造成ack发送不出去。接受不到音视频流
					所以在此处统一替换地址。和响应消息的Via头中的地址保持一致。
					 */
					Request ackRequest = clientDialog.createAck(cseqId);
					SipURI requestURI = (SipURI) ackRequest.getRequestURI();
					ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
					requestURI.setHost(viaHeader.getHost());
					requestURI.setPort(viaHeader.getPort());
					clientDialog.sendAck(ackRequest);
					CallIdHeader callIdHeader = (CallIdHeader) ackRequest.getHeader(CallIdHeader.NAME);
					//写入消息管理器
					mMessageManager.put(callIdHeader.getCallId(),clientDialog);
					logger.info("sendAck >>> {}",ackRequest);
				}
			} else if (Request.REGISTER.equals(method)) {
				int statusCode = response.getStatusCode();
				// 如果是未认证的状态
				if (statusCode == Response.UNAUTHORIZED) {
					String clientId = DeviceManagerController.serverInfoBo.getId();
					String clientDomain = DeviceManagerController.serverInfoBo.getDomain();

					String nonce = ((WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME)).getNonce();
					String uri = "sip:" + clientId + "@" + clientDomain;
					String callId = ((CallIdHeader) response.getHeader("Call-ID")).getCallId();
					String tag = ((FromHeader) response.getHeader("From")).getTag();

					String responseParam = mDigestServerAuthenticationHelper.MD5AuthenticateHeader(response, connectServerInfo.getId(), Request.REGISTER, uri, connectServerInfo.getPw());

					// 每一秒发一次，避免请求过多
					Thread.sleep(1000);
					this.sendRegister(connectServerInfo.getId(), connectServerInfo.getDomain(), connectServerInfo.getHost(),
							connectServerInfo.getPort(), connectServerInfo.getPw(), callId, tag, responseParam, nonce, uri, 1);
				}
				if (statusCode == Response.OK) {
					// 更新redis的时间
					ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
					String uri = toHeader.getAddress().getURI().toString();
					String serverSerialNum = uri.split(":")[1].split("@")[0];
					String key = SipLayer.SERVER_DEVICE_PREFIX + serverSerialNum;
					Integer expiredTime = higherServerExpiredTimeMap.get(key);
					RedisUtil.expire(key, expiredTime);

					// 开启心跳包发送线程
					GbServerInfo higherServerInfo = gbServerInfoService.getOne(new QueryWrapper<GbServerInfo>()
							.eq("device_serial_num", serverSerialNum));
					HeartbeatThread heartbeatThread = new HeartbeatThread();
					heartbeatThread.setSipLayer(this);
					heartbeatThread.startSendKeepAlive(higherServerInfo);
				}
			} else if (Request.MESSAGE.equals(method)) {
				int statusCode = response.getStatusCode();
				// 如果注册状态已经离线，则进行重新注册
				if (statusCode == Response.BAD_REQUEST) {
					String callId = IDUtils.id();
					String fromTag = IDUtils.id();
					sendRegister(connectServerInfo.getId(), connectServerInfo.getDomain(), connectServerInfo.getHost(), connectServerInfo.getPort(),
							connectServerInfo.getPw(), callId, fromTag, null, null, null, mCseq);
				}
				// 如果是keepAlive消息有回应，则进行数据的更新
				if (statusCode == Response.OK) {
					CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
					ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
					String uri = toHeader.getAddress().getURI().toString();
					String serverId = uri.split(":")[1].split("@")[0];
					String callId = callIdHeader.getCallId();
					// 如果keepAliveMap中含有这个callId，则说明服务器还在线
					// 更新redis中的时间
					if (keepAliveCallIdMap.containsKey(callId)) {
						Integer expiredTime = higherServerExpiredTimeMap.get(SERVER_DEVICE_PREFIX + serverId);
						RedisUtil.expire(SERVER_DEVICE_PREFIX + serverId, expiredTime);
						keepAliveCallIdMap.remove(callId);
					}
				}
			}

		}catch(Exception e){
			e.printStackTrace();
			logger.error(e.toString());
		}
	}

	/**
	 * 停止推流，删除session，断开通道
	 * @throws SipException
	 */
	private void close(PushStreamDevice pushStreamDevice) throws SipException{
		if(pushStreamDevice != null){
			pushStreamDevice.getServer().stopServer();
			pushStreamDevice.getObserver().stopRemux();
		}
	}

	private void closeServer(String serverKey) {
		Server server = CacheUtil.gbServerMap.get(serverKey);
		if (null != server) {
			if (null != server.getToHigherServer() && server.getToHigherServer() == 1) {
				ChannelInboundHandlerAdapter adapter = CacheUtil.deviceHandlerMap.get(serverKey);
				if (serverKey.contains("tcp")) {
					TCPHandler handler = (TCPHandler) adapter;
					handler.setToPushStream(0);
				} else if (serverKey.contains("udp")){
					UDPHandler handler = (UDPHandler) adapter;
					handler.setToPushStream(0);
				}
			} else {
				CacheUtil.gbServerMap.remove(serverKey);
			}
		}
	}

	public void sendBye(String callId) throws SipException{
		PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.removeByCallId(callId);
		Integer deviceBaseId = null;
		for (Map.Entry<Integer, JSONObject> entry : CacheUtil.baseDeviceIdCallIdMap.entrySet()) {
			JSONObject typeStreamJson = entry.getValue();
			String rtmpCallId = typeStreamJson.getJSONObject("rtmp")==null? "" : typeStreamJson.getJSONObject("rtmp").getString("callId");
			String hlsCallId = typeStreamJson.getJSONObject("hls")==null? "" : typeStreamJson.getJSONObject("hls").getString("callId");
			if (rtmpCallId.equals(callId) || hlsCallId.equals(callId)) {
				deviceBaseId = entry.getKey();
				break;
			}
		}
		if (null != deviceBaseId) {
			String tcpServerKey = deviceBaseId.toString() + "_tcp";
			String udpServerKey = deviceBaseId.toString() + "_udp";
			this.closeServer(tcpServerKey);
			this.closeServer(udpServerKey);
		}
		if(pushStreamDevice != null){
			close(pushStreamDevice);
			Dialog dialog = pushStreamDevice.getDialog();
			if(dialog != null){
				Request byeRequest = dialog.createRequest(Request.BYE);
				ClientTransaction clientTransaction = (isTCP(byeRequest) ? mTCPSipProvider:mUDPSipProvider).getNewClientTransaction(byeRequest);
				dialog.sendRequest(clientTransaction);
				logger.info("sendRequest >>> {}",byeRequest);
			}
		}

	}
	public String getSsrc(boolean isRealTime){
		StringBuffer buffer = new StringBuffer(15);
		buffer.append(String.valueOf(isRealTime?0:1));
		buffer.append(mSsrcRealm);
		if(mSsrc >= SSRC_MAX_VALUE){
			mSsrc = SSRC_MIN_VALUE;
		}
		String ssrcStr = String.valueOf(mSsrc);
		int length = ssrcStr.length();
		for(int i =length;i<4;i++){
			buffer.append("0");
		}
		buffer.append(String.valueOf(ssrcStr));

		return buffer.toString();
	}
	public int getPort(boolean isTcp){
		int resultPort = 0;
		if(PortUtils.isUsing(isTcp, mStreamPort)){
			resultPort = PortUtils.findAvailablePortRange(STREAM_MEDIA_START_PORT, STREAM_MEDIA_END_PORT, isTcp);
		}
		resultPort = mStreamPort;
		mStreamPort++;
		if(mStreamPort>STREAM_MEDIA_END_PORT){
			mStreamPort = STREAM_MEDIA_START_PORT;
		}
		return resultPort;
	}
	/**
	 * 会话结束
	 */
	public void processDialogTerminated(DialogTerminatedEvent evt) {

	}

	/**
	 * UDPSocket通道异常
	 */
	public void processIOException(IOExceptionEvent evt) {

	}

	/**
	 * 超时
	 * 下发的Request未能得到响应
	 */
	public void processTimeout(TimeoutEvent evt) {

	}

	/**
	 * 事务结束
	 */
	public void processTransactionTerminated(TransactionTerminatedEvent evt) {

	}

}
