package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.bean.DeviceChannel;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.bean.RecordStreamDevice;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.ResultConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.condition.GBDevicePlayCondition;
import com.yangjie.JGB28181.mapper.CameraInfoMapper;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.TCPServer;
import com.yangjie.JGB28181.media.server.UDPServer;
import com.yangjie.JGB28181.media.server.handler.GBStreamHandler;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;
import com.yangjie.JGB28181.media.server.handler.UDPHandler;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.media.server.remux.RtmpPusher;
import com.yangjie.JGB28181.media.server.remux.RtmpRecorder;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.message.session.MessageManager;
import com.yangjie.JGB28181.message.session.SyncFuture;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sip.Dialog;
import javax.sip.SipException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-10-28
 */
@Service
public class CameraInfoServiceImpl extends ServiceImpl<CameraInfoMapper, CameraInfo> implements CameraInfoService, OnProcessListener {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Config config;

    @Autowired
    private SipLayer mSipLayer;

    @Autowired
    private IDeviceManagerService deviceManagerService;

    public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

    private MessageManager mMessageManager = MessageManager.getInstance();

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${config.streamMediaIp}")
    private String streamMediaIp;

    @Value("${config.pushHlsAddress}")
    private String pushHlsAddress;


    @Value("${config.pushRtmpAddress}")
    private String pushRtmpAddress;

    @Override
    public List<CameraInfo> getAllData() {
        List<CameraInfo> cameraInfos = super.getBaseMapper().selectList(null);
        return cameraInfos;
    }

    @Override
    public CameraInfo getDataByDeviceBaseId(Integer deviceBaseId) {
        CameraInfo data = super.getBaseMapper().selectOne(new QueryWrapper<CameraInfo>().eq("device_base_id", deviceBaseId));
        return data;
    }

    @Override
    public GBResult rtspDevicePlay(CameraPojo pojo) {
        GBResult result = null;
        // 校验参数
        if (this.verifyRtspPlayParameters(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(), pojo.getStream())) {
            CameraPojo cameraPojo = new CameraPojo();
            cameraPojo.setToHls(pojo.getToHls());
            // 获取当前时间
            Set<String> keys = CacheUtil.STREAMMAP.keySet();
            Set<String> recordKeys = CacheUtil.rtspVideoRecordMap.keySet();
            // 处理推流/录像的逻辑
            if (pojo.getIsRecord() == 0) {
                result = this.handlePushStream(keys, pojo);
            }
            if (pojo.getIsRecord() == 1) {
                result = this.handleRecordStream(recordKeys, pojo);
            }
        }
        return result;
    }


    @Override
    public CameraPojo openStream(CameraPojo pojo) {
        CameraPojo cameraPojo = new CameraPojo();
        // 生成token
        String token = UUID.randomUUID().toString();
        String rtsp = "";
        String rtmp = "";
        String hls = "";
        String flv = "";
        String hlsUrl = "";
        String IP = IpUtil.IpConvert(pojo.getIp());
        StringBuilder sb = new StringBuilder();
        String[] ipArr = pojo.getIp().split("\\.");
        for (String item : ipArr) {
            sb.append(item);
        }
        sb.append(pojo.getChannel());
        sb.append(pojo.getStream());
        token = sb.toString();
        if (pojo.getIsRecord() == 1) {
            token = "record_" + sb.toString();
        }
        String url = "";

        if (pojo.getToHls() == 1) {
            // 开启清理过期的TS索引文件的定时器
            StreamUtils.cleanUpTempTsFile(pojo.getDeviceId(), "1", 1);
        }
        rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/h264/ch" + pojo.getChannel() + "/" + pojo.getStream()
                + "/av_stream";
        rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/live/" + token;
        hls = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/hls/" + StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1");
        flv	= BaseConstants.flvBaseUrl + token;
        if (config.getHost_extra().equals("127.0.0.1")) {
            hlsUrl = BaseConstants.hlsBaseUrl + StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1") + "/index.m3u8";
            hlsUrl = hlsUrl.replace("127.0.0.1", streamMediaIp);
            url = rtmp;
            url = url.replace("127.0.0.1", streamMediaIp);
            flv = flv.replace("127.0.0.1", streamMediaIp);
        } else {
            url = "rtmp://" + IpUtil.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/live/"
                    + token;
        }

        String recordDir = RecordNameUtils.recordVideoFileAddress(StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1"));
        cameraPojo.setUsername(pojo.getUsername());
        cameraPojo.setPassword(pojo.getPassword());
        cameraPojo.setIp(IP);
        cameraPojo.setChannel(pojo.getChannel());
        cameraPojo.setStream(pojo.getStream());
        cameraPojo.setRtsp(rtsp);
        cameraPojo.setRtmp(rtmp);
        cameraPojo.setHls(hls);
        cameraPojo.setUrl(url);
        cameraPojo.setFlv(flv);
        cameraPojo.setHlsUrl(hlsUrl);
        cameraPojo.setOpenTime(pojo.getOpenTime());
        cameraPojo.setCount(1);
        cameraPojo.setToken(token);
        cameraPojo.setCid(pojo.getCid());
        cameraPojo.setToHls(pojo.getToHls());
        cameraPojo.setToFlv(pojo.getToFlv());
        cameraPojo.setDeviceId(pojo.getDeviceId());
        cameraPojo.setIsRecord(pojo.getIsRecord());
        cameraPojo.setIsSwitch(pojo.getIsSwitch());
        cameraPojo.setRecordDir(recordDir);
        cameraPojo.setApplicationContext(applicationContext);

        // 执行任务
        CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
        CameraThread.MyRunnable.es.execute(job);
        CacheUtil.jobMap.put(token, job);
        if (cameraPojo.getIsRecord() == 0) {
            Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
            Integer expiredTime = Math.toIntExact(expiredMs / 1000);
            // 设置5分钟的过期时间
            RedisUtil.set(token, expiredTime, "keepStreaming");
        }

        return cameraPojo;
    }

    @Override
    public GBResult gbPlay(String cameraIp, Integer deviceId, Integer isRecord, Integer isSwitch, Integer toFlv,
                               Integer toHls) {
        JSONObject dataJson = DeviceUtils.getLiveCamInfoVoByMatchIp(cameraIp);
        String deviceStr = null;
        String pushStreamDeviceId = null;
        if (null != dataJson) {
            deviceStr = dataJson.getString("deviceStr");
            pushStreamDeviceId = dataJson.getString("pushStreamDeviceId");
        }
        // 如果redis上获取设备的信息成功，则进行推流
        if (!StringUtils.isEmpty(deviceStr)) {
            Device device = JSONObject.parseObject(deviceStr, Device.class);
            Map<String, DeviceChannel> channelMap = device.getChannelMap();
            String channelId = null;
            for (String key : channelMap.keySet()) {
                DeviceChannel deviceChannel = channelMap.get(key);
                if (null != deviceChannel) {
                    channelId = key;
                }
            }
            return this.gbDevicePlay(new GBDevicePlayCondition(deviceId, pushStreamDeviceId, channelId, "TCP", 0, null, 0, isRecord, 0, toFlv, 1, 0, null, null, null));
        }
        return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
    }

    /**
     * 处理录像的流
     * @param recordKeys
     * @param pojo
     * @return
     */
    private GBResult handleRecordStream(Set<String> recordKeys, CameraPojo pojo) {
        Boolean exist = false;
        GBResult result = null;
        for (String key : recordKeys) {
            if (pojo.getDeviceId().equals(CacheUtil.rtspVideoRecordMap.get(key).getDeviceId())) {
                exist = true;
            }
        }
        if (exist) {
            result = GBResult.build(201, "已经正在录像，请勿重复请求", null);
        } else {
            CameraPojo cameraPojo = this.openStream(pojo);
            String url = cameraPojo.getRecordDir();
            result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
            logger.info("打开：" + cameraPojo.getRtsp());
        }

        return result;
    }

    /**
     * 处理rtsp的推流逻辑
     * @param keys
     * @param pojo
     * @return
     */
    private GBResult handlePushStream(Set<String> keys, CameraPojo pojo) {
        Boolean exist = false;
        GBResult result = null;
        CameraPojo cameraPojo = null;
        for (String key : keys) {
            // 如果已经存在直播流，则直接返回
            if (pojo.getDeviceId().equals(CacheUtil.STREAMMAP.get(key).getDeviceId())) {
                cameraPojo = CacheUtil.STREAMMAP.get(key);
                exist = true;
            }
        }
        if (exist) {
            String url = this.getRtspPlayUrl(pojo.getToHls(), pojo.getIsRecord(), pojo.getToFlv(), cameraPojo);
            result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
            logger.info("打开：" + cameraPojo.getRtsp());
        } else {
            result = this.startOpenStream(pojo);
        }

        return result;
    }

    /**
     * 开始进行推流/录像
     * @param pojo
     * @return
     */
    private GBResult startOpenStream(CameraPojo pojo) {
        GBResult result = null;
        CameraPojo cameraPojo = this.openStream(pojo);
        String url = this.getRtspPlayUrl(pojo.getToHls(), pojo.getIsRecord(), pojo.getToFlv(), cameraPojo);
        result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
        logger.info("打开：" + cameraPojo.getRtsp());

        return result;
    }

    private String getRtspPlayUrl(Integer toHls, Integer isRecord, Integer toFlv, CameraPojo cameraPojo) {
        String url = null;
        if (toHls == 1) {
            url = cameraPojo.getHlsUrl();
        }
        if (isRecord == 1) {
            url = cameraPojo.getRecordDir();
        }
        if (toFlv == 1) {
            url = cameraPojo.getFlv();
        }
        return url;
    }

    /**
     * 验证rtsp连接播放视频的参数
     * @param ip
     * @param username
     * @param password
     * @param channel
     * @param stream
     * @return
     */
    private Boolean verifyRtspPlayParameters(String ip, String username, String password, String channel, String stream) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        if (StringUtils.isEmpty(username)) {
            return false;
        }
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        if (StringUtils.isEmpty(channel)) {
            return false;
        }
        if (StringUtils.isEmpty(stream)) {
            return false;
        }
        return true;
    }

    @Override
    public GBResult gbDevicePlay(GBDevicePlayCondition gbDevicePlayCondition) {
        GBResult result = null;
        Integer id = gbDevicePlayCondition.getId();
        String deviceId = gbDevicePlayCondition.getDeviceId();
        String channelId = gbDevicePlayCondition.getChannelId();
        String mediaProtocol = gbDevicePlayCondition.getProtocol();
        Integer isTest = gbDevicePlayCondition.getIsTest();
        Integer cid = gbDevicePlayCondition.getCid();
        Integer toHls = gbDevicePlayCondition.getToHls();
        Integer isRecord = gbDevicePlayCondition.getIsRecord();
        Integer isSwitch = gbDevicePlayCondition.getIsSwitch();
        Integer toFlv = gbDevicePlayCondition.getToFlv();
        Integer toPushStream = gbDevicePlayCondition.getToPushStream();
        Integer toHigherServer = gbDevicePlayCondition.getToHigherServer();
        String higherServerIp = gbDevicePlayCondition.getHigherServerIp();
        Integer higherServerPort = gbDevicePlayCondition.getHigherServerPort();
        String higherCallId = gbDevicePlayCondition.getHigherCallId();
        try {
            int pushPort = 1935;
            // 1. 从redis查找设备，如果不存在，返回离线
            String deviceStr = RedisUtil.get(SipLayer.SUB_DEVICE_PREFIX + deviceId);
            if (StringUtils.isEmpty(deviceStr)) {
                return GBResult.build(ResultConstants.DEVICE_OFF_LINE_CODE, ResultConstants.DEVICE_OFF_LINE);
            }

            // 2. 生成创建服务器的必要参数
            String streamName = StreamNameUtils.play(deviceId, channelId);
            PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.get(streamName);
            RecordStreamDevice recordStreamDevice = CacheUtil.deviceRecordMap.get(id);
            boolean isTcp = mediaProtocol.toUpperCase().equals(SipLayer.TCP);
            JSONObject serverParameters = this.createServerParameters(streamName, toHls, isRecord, isTcp);
            String callId = null;
            if (!StringUtils.isEmpty(higherCallId)) {
                callId = higherCallId;
            } else {
                callId = serverParameters.getString("callId");
            }
            String ssrc = serverParameters.getString("ssrc");
            Integer port = serverParameters.getInteger("port");
            String deviceIdProtocolKey = id + "_" + (isTcp? "tcp" : "udp");

            // 3. 检查通道是否存在
            Device device = JSONObject.parseObject(deviceStr, Device.class);
            Map<String, DeviceChannel> channelMap = device.getChannelMap();
            if (channelMap == null || !channelMap.containsKey(channelId)) {
                return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
            }

            // 4. 判断是否存在推流/级联平台推送/录像
            result = this.judgePushStreamRecordExist(pushStreamDevice, recordStreamDevice, isRecord, toPushStream, toHigherServer,
                    higherServerIp, higherServerPort, isTcp, deviceIdProtocolKey, toFlv, null);
            int existCode = result.getCode();
            if (existCode == 201) {
                return result;
            }

            // 5. 如果是udp协议，先开启udp服务器
            if (!isTcp) {
                result = this.createPushStream(id, deviceId, channelId, port, streamName,
                        ssrc, callId, cid, toHls, isTest, isRecord, isTcp, toHigherServer, toPushStream, higherServerIp, higherServerPort, null);
            }

            // 6. 下发invite指令
            mSipLayer.sendInvite(device, SipLayer.SESSION_NAME_PLAY, callId, channelId, port, ssrc, isTcp);

            // 7. 等待指令响应
            SyncFuture<?> receive = mMessageManager.receive(callId);
            Dialog response = (Dialog) receive.get(3, TimeUnit.SECONDS);
            if (null != pushStreamDevice && !isTcp) {
                Dialog response1 = (Dialog) receive.get(3, TimeUnit.SECONDS);
                pushStreamDevice = mPushStreamDeviceManager.get(streamName);
                pushStreamDevice.setDialog(response1);
            }

            // 8. 如果tcp协议请求并且指令得到响应，则开启tcp服务器
            if (isTcp && response != null) {
                result = this.createPushStream(id, deviceId, channelId, port, streamName,
                        ssrc, callId, cid, toHls, isTest, isRecord, isTcp, toHigherServer, toPushStream, higherServerIp, higherServerPort, response);
            } else if (isTcp && null == response) {
                System.out.println("响应失败，删除推流session");
                mMessageManager.remove(callId);
                result = GBResult.build(ResultConstants.COMMAND_NO_RESPONSE_CODE, ResultConstants.COMMAND_NO_RESPONSE);
            }
        } catch(Exception e){
            e.printStackTrace();
            result = GBResult.build(ResultConstants.SYSTEM_ERROR_CODE, ResultConstants.SYSTEM_ERROR);
        }
        return result;
    }

    /**
     * 判断是否存在推流/录像
     */
    private GBResult judgePushStreamRecordExist(PushStreamDevice pushStreamDevice, RecordStreamDevice recordStreamDevice,
                                            Integer isRecord, Integer toPushStream, Integer toHigherServer, String higherServerIp,
                                            Integer higherServerPort, Boolean isTcp, String deviceIdProtocolKey, Integer toFlv,
                                                String higherCallId) {
        GBResult result = GBResult.ok();
        // 1. 判断是否存在推流
        if (pushStreamDevice != null && toPushStream == 1) {
            if (toFlv == 0) {
                result = GBResult.build(201, "已经存在推流，请勿重复请求", new MediaData(pushStreamDevice.getPullRtmpAddress(), pushStreamDevice.getCallId()));
            } else {
                result = GBResult.build(201, "已经存在推流，请勿重复请求", new MediaData(pushStreamDevice.getPullFlvAddress(), pushStreamDevice.getCallId()));
            }
        }
        // 2. 判断是否存在级联平台的推送
        if (toHigherServer == 1) {
            GBStreamHandler handler = CacheUtil.deviceHandlerMap.get(deviceIdProtocolKey);
            Server server = CacheUtil.gbServerMap.get(deviceIdProtocolKey);
            // 设置推送参数
            if (null != handler) {
                server.setToHigherServer(1);
                handler.connectNewRemoteAddress(higherServerIp, higherServerPort, higherCallId);
            }
            return result = GBResult.ok();
        }
        // 3. 判断是否存在录像
        if (recordStreamDevice != null && isRecord == 1) {
            result = GBResult.build(201, "已经正在录像，请勿重复请求", null);
        }
        return result;
    }

    /**
     * 创建开启服务器的必要参数
     * @param streamName
     * @param toHls
     * @param isRecord
     * @param isTcp
     * @return
     */
    private JSONObject createServerParameters(String streamName, Integer toHls, Integer isRecord, Boolean isTcp) {
        // 4.2.1 创建推流地址
        String address = pushRtmpAddress.concat(streamName);
        if (toHls == 1) {
            address = pushHlsAddress.concat(streamName);
        }
        // 4.2.2 创建callId
        String callId = null;
        if (isRecord == 0) {
            callId = IDUtils.id();
        } else {
            callId = "record_" + IDUtils.id();
        }
        // 4.2.3 创建ssrc
        String ssrc = mSipLayer.getSsrc(true);
        // 4.2.4 创建端口号
        int port = mSipLayer.getPort(isTcp);

        JSONObject paramJson = new JSONObject();
        paramJson.put("address", address);
        paramJson.put("callId", callId);
        paramJson.put("ssrc", ssrc);
        paramJson.put("port", port);

        return paramJson;
    }

    /**
     * 创建服务器
     */
    private Server createServer(Boolean isTcp, String ssrc, String streamName, Integer id, Integer port, Integer toPushStream,
                                Integer toHigherServer, String higherServerIp, Integer higherServerPort, Integer isRecord,
                                String higherCallId) throws InterruptedException {
        String protocol = isTcp? "tcp" : "udp";
        String deviceIdProtocolKey = id.toString() + "_" + protocol;
        if (isRecord == 1) {
            deviceIdProtocolKey += "_record";
        }
        Server server = CacheUtil.gbServerMap.get(deviceIdProtocolKey);
        if (null != server) {
            return server;
        }

        server = isTcp ? new TCPServer() : new UDPServer();
        server.startServer(new ConcurrentLinkedDeque<>(),Integer.valueOf(ssrc),port,false, streamName,
                id, toPushStream, toHigherServer, higherServerIp, higherServerPort, higherCallId);
        CacheUtil.gbServerMap.put(deviceIdProtocolKey, server);
        Thread.sleep(1000);

        return server;
    }

    /**
     * 创建推流的容器
     */
    private JSONObject createObserver(Server server, String callId, String streamName, String deviceId, String ssrc,
                                      Integer id, Integer port, Integer isRecord, Boolean isTcp, Integer toHls,
                                      Integer isTest, Integer cid, Dialog response) {
        JSONObject jsonObject = new JSONObject();
        Observer observer;
        PushStreamDevice pushStreamDevice = null;
        RecordStreamDevice recordStreamDevice = null;
        String address = pushRtmpAddress.concat(streamName);
        // 如果是hls，则推到hls的地址
        if (toHls == 1) {
            address = pushHlsAddress.concat(streamName);
        }
        // 判断是推流还是录像
        if (isRecord == 0) {
            observer = new RtmpPusher(address, callId);
            ((RtmpPusher) observer).setDeviceId(streamName);
            String pullFlvAddress = BaseConstants.flvBaseUrl + streamName;
            // 保存推流信息
            pushStreamDevice = new PushStreamDevice(deviceId,Integer.valueOf(ssrc),callId,streamName,port,isTcp,server,
                    observer,address, pullFlvAddress);
            if (null != response) {
                pushStreamDevice.setDialog(response);
            }
            ActionController.mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
        } else {
            String recordAddress = RecordNameUtils.recordVideoFileAddress(streamName);
            logger.info("录像文件地址: " +  recordAddress);
            observer = new RtmpRecorder(recordAddress, callId);
            ((RtmpRecorder) observer).setDeviceId(streamName);
            recordStreamDevice = new RecordStreamDevice(deviceId, Integer.valueOf(ssrc), callId, streamName, port, isTcp, server,
                    observer, recordAddress);
            CacheUtil.deviceRecordMap.put(id, recordStreamDevice);
        }
        server.subscribe(observer);
        observer.startRemux(isTest, cid, toHls, id, streamName, applicationContext);
        CacheUtil.gbPushObserver.put(callId, observer);

        observer.setOnProcessListener(this);

        jsonObject.put("observer", observer);
        jsonObject.put("pushStreamDevice", pushStreamDevice);
        jsonObject.put("recordStreamDevice", recordStreamDevice);

        return jsonObject;
    }

    /**
     * 处理推流后的一些配置
     * @throws IOException
     */
    private GBResult handlePushStreamConfig(Integer id, Integer isRecord, Integer toHls, PushStreamDevice pushStreamDevice,
                                            RecordStreamDevice recordStreamDevice, String deviceId, String channelId,
                                            String callId, String streamName) throws IOException {
        GBResult result;
        if (isRecord == 0) {
            Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
            Integer expiredTime = Math.toIntExact(expiredMs / 1000);
            // 设置5分钟的过期时间
            RedisUtil.set(callId, expiredTime, "keepStreaming");
            // 如果推流的id不为空且已经注册到数据库中，则保存在推流设备map中
            if (null != id && deviceManagerService.judgeCameraIsRegistered(id)) {
                CacheUtil.streamingDeviceMap.put(id, pushStreamDevice);
            }

            String mediaIp = StreamUtils.getStreamMediaIp();
            if (toHls == 1) {
                // 开启清理过期的TS索引文件的定时器
                StreamUtils.cleanUpTempTsFile(deviceId, channelId, 0);

                String pushHlsStreamAddress = BaseConstants.hlsBaseUrl.replace("127.0.0.1", mediaIp);
                String hlsPlayFile = pushHlsStreamAddress + streamName + "/index.m3u8";
                pushStreamDevice.setPullRtmpAddress(hlsPlayFile);
                result = GBResult.ok(new MediaData(hlsPlayFile, pushStreamDevice.getCallId()));
            } else {
                String pushRtmpStreamAddress = BaseConstants.rtmpBaseUrl.replace("127.0.0.1", mediaIp);
                String rtmpPlayFile = pushRtmpStreamAddress + streamName;
                pushStreamDevice.setPullRtmpAddress(rtmpPlayFile);
                result = GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
            }
        } else {
            result = GBResult.ok(new MediaData(recordStreamDevice.getPullRtmpAddress(), recordStreamDevice.getCallId()));
        }

        return result;
    }

    /**
     * 创建推流
     * @throws IOException
     */
    private GBResult createPushStream(Integer id, String deviceId, String channelId, Integer port, String streamName, String ssrc, String callId, Integer cid,
                                  Integer toHls, Integer isTest, Integer isRecord, Boolean isTcp, Integer toHigherServer, Integer toPushStream,
                                  String higherServerIp, Integer higherServerPort, Dialog response) throws IOException, InterruptedException {
        GBResult result = GBResult.ok();

        // 1. 创建服务器
        Server server = this.createServer(isTcp, ssrc, streamName, id, port, toPushStream, toHigherServer, higherServerIp,
                higherServerPort, isRecord, callId);
        server.setResponse(response);

        if (toPushStream == 1) {
            // 2. 创建推流容器并进行推流/录像
            JSONObject observerJson = this.createObserver(server, callId, streamName, deviceId, ssrc, id, port, isRecord, isTcp, toHls, isTest, cid, response);
            Observer observer = (Observer) observerJson.get("observer");
            PushStreamDevice pushStreamDevice = (PushStreamDevice) observerJson.get("pushStreamDevice");
            RecordStreamDevice recordStreamDevice = (RecordStreamDevice) observerJson.get("recordStreamDevice");

            // 3. 处理推流的配置
            result = handlePushStreamConfig(id, isRecord, toHls, pushStreamDevice, recordStreamDevice, deviceId, channelId, callId, streamName);
        }
        return result;
    }

    @Override
    public void onError(String callId) {
        try {
            mSipLayer.sendBye(callId);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }
}
