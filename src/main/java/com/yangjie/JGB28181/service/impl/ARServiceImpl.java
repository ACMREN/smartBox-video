package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.yangjie.JGB28181.bean.WebSocketServer;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.IARService;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_imgproc.circle;

@Service("arService")
public class ARServiceImpl implements IARService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Config config;

    @Autowired
    private CameraInfoService cameraInfoService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    WebSocketServer webSocketServer = new WebSocketServer();

    @Value("${config.streamMediaIp}")
    private String streamMediaIp;

    private FFmpegFrameRecorder recorder = null;
    private FFmpegFrameGrabber grabber = null;

    private static Map<String, Object> posMap = new HashMap<>(20);

    private Long startTime = 0L;
    private Long useTime = 0L;

    private NativeLong lUserID = new NativeLong(0);

    private static List<Scalar> scalarList = new ArrayList<>();

    private static List<Integer> colorList = new ArrayList<>(10);

    private static String colorStr = "";

    static {
        scalarList.add(getColor(0,255,255));
        scalarList.add(getColor(255,0,0));
        scalarList.add(getColor(255,0,255));
        scalarList.add(getColor(255,255,0));
        scalarList.add(getColor(255,255,255));
        scalarList.add(getColor(0,0,0));
        scalarList.add(getColor(0,255,0));
        scalarList.add(getColor(0,0,255));
    }

    private static Scalar getColor(int r,int g, int b){
        Scalar s = new Scalar();
        s.red(r);
        s.green(g);
        s.blue(b);
        return s;
    }

    private static Point point(int left, int top) {
        Point point = new Point();
        point.x(left);
        point.y(top);
        return point;
    }

    @Override
    public GBResult playARStream(Integer deviceBaseId) {
        CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceBaseId);
        // 只有rtsp设备才能进行ar设置
        if (LinkTypeEnum.RTSP.getCode() == cameraInfo.getLinkType().intValue()) {
            String rtspLink = cameraInfo.getRtspLink();
            CameraPojo cameraPojo = DeviceUtils.parseRtspLinkToCameraPojo(rtspLink);
            cameraPojo.setToHls(0);
            cameraPojo.setToFlv(1);
            cameraPojo.setDeviceId(deviceBaseId.toString());
            cameraPojo.setIsRecord(0);
            cameraPojo.setIsSwitch(0);

            GBResult result = this.rtspDevicePlay(cameraPojo);
            int code = result.getCode();
            if (code == 200) {
                List<JSONObject> resultList = new ArrayList<>();
                MediaData mediaData = (MediaData) result.getData();

                JSONObject resultJson = new JSONObject();
                String address = mediaData.getAddress();
                resultJson.put("deviceId", deviceBaseId);
                resultJson.put("source", address);

                resultList.add(resultJson);
                return GBResult.ok(resultList);
            }
        }

        return GBResult.fail();
    }

    /**
     * rtsp设备播放视频
     * @param pojo
     * @return
     */
    private GBResult rtspDevicePlay(CameraPojo pojo) {
        GBResult result = null;
        // 校验参数
        if (this.verifyRtspPlayParameters(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(), pojo.getStream())) {
            // 处理AR推流
            result = this.startOpenStream(pojo);
        }
        return result;
    }

    /**
     * 参数验证
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

    /**
     * 开始视频流
     * @param pojo
     * @return
     */
    private GBResult startOpenStream(CameraPojo pojo) {
        GBResult result = null;
        CameraPojo cameraPojo = this.openStream(pojo);
        String url = cameraPojo.getFlv();
        result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
        logger.info("打开：" + cameraPojo.getRtsp());

        return result;
    }

    /**
     * 开启视频流
     * @param pojo
     * @return
     */
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
        token = "ar_" + sb.toString();
        String url = "";

        if (pojo.getToHls() == 1) {
            // 开启清理过期的TS索引文件的定时器
            StreamUtils.cleanUpTempTsFile(pojo.getDeviceId(), "1", 1);
        }
        rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/h264/ch" + pojo.getChannel() + "/" + pojo.getStream()
                + "/av_stream";
        rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/live/" + token;
        flv	= BaseConstants.flvBaseUrl + token;
        if (config.getHost_extra().equals("127.0.0.1")) {
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
        new Thread(() -> {
            try {
                this.pushARStream(cameraPojo);
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }).start();

        return cameraPojo;
    }

    private void pushARStream(CameraPojo cameraPojo) throws FrameGrabber.Exception, FrameRecorder.Exception {
        String rtspLink = cameraPojo.getRtsp();
        String rtmpLink = cameraPojo.getRtmp();
        String ip = cameraPojo.getIp();
        String username = cameraPojo.getUsername();
        String password = cameraPojo.getPassword();

        this.setUpGrabber(rtspLink);
        this.setUpRecorder(rtmpLink);

        int interval = 0;
        boolean isKeyFrame = false;
        startTime = System.currentTimeMillis();

        new Thread(() -> this.getPTZPos(ip, username, password)).start();

        Frame frame = null;
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        List<Point> pointList = this.setUpPointList();
        try {
            while (true) {
                Boolean isGetKeyFrame = WebSocketServer.deviceKeyFrameMap.get(Integer.valueOf(cameraPojo.getDeviceId()));

                frame = grabber.grab();
                if (null == frame) {
                    continue;
                }
                if (null != isGetKeyFrame && isGetKeyFrame) {
                    if(interval == 50){
                        isKeyFrame = this.packageKeyFrameInfo(converter, frame, pointList);
                    }
                    if(interval > 50){
                        interval = 0;
                        colorStr = "";
                        colorList = new ArrayList<>(10);
                    }
                }

                recorder.record(frame);

                if (null != isGetKeyFrame && isGetKeyFrame) {
                    useTime = recorder.getTimestamp();

                    isKeyFrame = this.sendPTZPos(isKeyFrame);

                    interval++;
                }
            }
        } catch (Exception e) {
            recorder.stop();
            recorder.close();
            grabber.stop();
            grabber.close();
            e.printStackTrace();
        }
    }

    /**
     * 设置grabber
     * @param rtspLink
     * @throws FrameGrabber.Exception
     */
    private void setUpGrabber(String rtspLink) throws FrameGrabber.Exception {
        grabber = new FFmpegFrameGrabber(rtspLink);
        grabber.setOption("hwaccel", "cuda");
        grabber.setVideoCodecName("h264_cuvid");
        grabber.setVideoOption("rtsp_transport", "tcp");
        grabber.start();
    }

    /**
     * 设置recorder
     * @param rtmpLink
     * @throws FrameRecorder.Exception
     */
    private void setUpRecorder(String rtmpLink) throws FrameRecorder.Exception {
        recorder = new FFmpegFrameRecorder(rtmpLink, grabber.getImageWidth(), grabber.getImageHeight(), 0);
        recorder.setFormat("flv");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodecName("h264_nvenc");
        recorder.start();
    }

    /**
     * 设置关键帧的点位位置
     * @return
     */
    private List<Point> setUpPointList() {
        Point tl = point(0, 0);
        Point tr = point(grabber.getImageWidth(), 0);
        Point bl = point(0, grabber.getImageHeight());
        Point br = point(grabber.getImageWidth(), grabber.getImageHeight());
        List<Point> pointList = new ArrayList<>(10);
        pointList.add(tl);
        pointList.add(tr);
        pointList.add(bl);
        pointList.add(br);

        return pointList;
    }

    /**
     * 通过sdk获取当前摄像头角度
     * @param ip
     * @param username
     * @param password
     */
    private void getPTZPos(String ip, String username, String password) {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        if (lUserID.intValue() <= 0) {
            lUserID = hcNetSDK.NET_DVR_Login_V30(ip, (short) 8000, username, password, null);
        }

        while (true) {
            HCNetSDK.NET_DVR_PTZPOS net_dvr_ptzpos = new HCNetSDK.NET_DVR_PTZPOS();
            Pointer pointer = net_dvr_ptzpos.getPointer();

            hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PTZPOS, new NativeLong(1), pointer, net_dvr_ptzpos.size(), new IntByReference(0));
            net_dvr_ptzpos.read();

            posMap.put("\"p\"", "\"" + HexToDecMa(net_dvr_ptzpos.wPanPos).toString()+"\"");
            posMap.put("\"t\"", "\"" + HexToDecMa(net_dvr_ptzpos.wTiltPos).toString()+"\"");
            posMap.put("\"z\"", "\"" + HexToDecMa(net_dvr_ptzpos.wZoomPos).toString()+"\"");
        }
    }

    /**
     * 包装关键帧的数据
     * @param converter
     * @param frame
     * @param pointList
     * @return
     */
    private boolean packageKeyFrameInfo(OpenCVFrameConverter.ToMat converter, Frame frame, List<Point> pointList) {
        Random random = new Random();
        colorList.add(random.nextInt(8));
        colorList.add(random.nextInt(8));
        colorList.add(random.nextInt(8));
        colorList.add(random.nextInt(8));

        for (Integer item : colorList) {
            colorStr += item.toString();
        }

        Mat mat = converter.convert(frame);
        for (int i =0; i < 4; i++) {
            Point point = pointList.get(i);
            Scalar scalar = scalarList.get(colorList.get(i));
            circle(mat, point, 1, scalar, 5, 0, 0);
        }
        return true;
    }

    /**
     * 发送当前角度以及关键帧的数据
     * @param isKeyFrame
     * @return
     */
    private boolean sendPTZPos(boolean isKeyFrame) {

        posMap.put("\"isKeyFrame\"", "\""+isKeyFrame +"\"");
        posMap.put("\"timestamp\"", "\""+useTime.toString() +"\"");
        posMap.put("\"colorStr\"", "\""+colorStr +"\"");

        isKeyFrame = false;

        System.out.println(posMap.toString());
        webSocketServer.onMessage(posMap.toString());

        return isKeyFrame;
    }

    /**
     * 十六进制转十进制
     * @param pos
     * @return
     */
    private static Double HexToDecMa(short pos) {
        System.out.println(pos);
        return Double.valueOf((pos / 4096) * 1000 +((pos % 4096) / 256) * 100 + ((pos % 256) / 16) * 10 + (pos % 16));
    }
}