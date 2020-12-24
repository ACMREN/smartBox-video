package com.yangjie.JGB28181.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.IpUtil;
import com.yangjie.JGB28181.common.utils.RecordNameUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.mapper.CameraInfoMapper;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-10-28
 */
@Service
public class CameraInfoServiceImpl extends ServiceImpl<CameraInfoMapper, CameraInfo> implements CameraInfoService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Config config;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${config.streamMediaIp}")
    private String streamMediaIp;

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
        // 历史流
        if (!StringUtils.isEmpty(pojo.getStartTime())) {
            if (!StringUtils.isEmpty(pojo.getEndTime())) {
                rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/Streaming/tracks/" + pojo.getChannel()
                        + "01?starttime=" + pojo.getStartTime().substring(0, 8) + "t" + pojo.getStartTime().substring(8) + "z'&'endtime="
                        + pojo.getEndTime().substring(0, 8) + "t" + pojo.getEndTime().substring(8) + "z";
                cameraPojo.setStartTime(pojo.getStartTime());
                cameraPojo.setEndTime(pojo.getEndTime());
            } else {
                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    String startTime = df.format(df.parse(pojo.getStartTime()).getTime() - 60 * 1000);
                    String endTime = df.format(df.parse(pojo.getEndTime()).getTime() + 60 * 1000);
                    rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/Streaming/tracks/" + pojo.getChannel()
                            + "01?starttime=" + startTime.substring(0, 8) + "t" + startTime.substring(8)
                            + "z'&'endtime=" + endTime.substring(0, 8) + "t" + endTime.substring(8) + "z";
                    cameraPojo.setStartTime(startTime);
                    cameraPojo.setEndTime(endTime);
                } catch (ParseException e) {
                    logger.error("时间格式化错误！", e);
                }
            }
            rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/history/"
                    + token;
            if (config.getHost_extra().equals("127.0.0.1")) {
                url = rtmp;
            } else {
                url = "rtmp://" + IpUtil.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/history/"
                        + token;
            }
        } else {// 直播流
            if (pojo.getToHls() == 1) {
                // 开启清理过期的TS索引文件的定时器
                PushHlsStreamServiceImpl.cleanUpTempTsFile(pojo.getDeviceId(), "1", 1);
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
        ActionController.jobMap.put(token, job);
        if (cameraPojo.getIsRecord() == 0) {
            Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
            Integer expiredTime = Math.toIntExact(expiredMs / 1000);
            // 设置5分钟的过期时间
            RedisUtil.set(token, expiredTime, "keepStreaming");
        }

        return cameraPojo;
    }
}
