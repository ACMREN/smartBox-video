package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.DeviceUtils;
import com.yangjie.JGB28181.common.utils.StreamUtils;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.media.server.remux.RtmpRecorder;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.ICameraRecordService;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.sip.SipException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CameraRecordServiceImpl implements ICameraRecordService {

    @Autowired
    private CameraInfoService cameraInfoService;

    @Autowired
    private SipLayer sipLayer;

    @Override
    public GBResult startCameraRecord(List<Integer> deviceIds) {
        GBResult result = null;
        for (Integer deviceId : deviceIds) {
            CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
            // 国标设备开启录像
            if (cameraInfo.getLinkType().intValue() == LinkTypeEnum.GB28181.getCode()) {
                result = cameraInfoService.gbPlay(cameraInfo.getIp(), deviceId, 1, 1, 0, 0);
                return judgeRecordIsOpen(result, deviceId);
            }
            // rtsp设备开启录像
            if (cameraInfo.getLinkType().intValue() == LinkTypeEnum.RTSP.getCode()) {
                String rtspLink = cameraInfo.getRtspLink();
                CameraPojo cameraPojo = DeviceUtils.parseRtspLinkToCameraPojo(rtspLink);
                cameraPojo.setToHls(1);
                cameraPojo.setDeviceId(deviceId.toString());
                cameraPojo.setIsRecord(1);
                cameraPojo.setIsSwitch(1);
                cameraPojo.setToFlv(0);
                result = cameraInfoService.rtspDevicePlay(cameraPojo);
                return judgeRecordIsOpen(result, deviceId);
            }
        }

        return result;
    }

    @Override
    public List<Integer> stopRecordStream(List<Integer> deviceBaseIds) throws SipException {
        List<Integer> failDeviceIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(deviceBaseIds)) {
            for (Integer deviceBaseId : deviceBaseIds) {
                JSONObject streamJson = CacheUtil.baseDeviceIdCallIdMap.get(deviceBaseId);
                JSONObject typeStreamJson = streamJson.getJSONObject(BaseConstants.PUSH_STREAM_RECORD);
                String callId = typeStreamJson.getString("callId");

                RtmpRecorder gbRecorder = (RtmpRecorder) CacheUtil.gbPushObserver.get(callId);
                CameraThread.MyRunnable rtspRecorder = CacheUtil.jobMap.get(callId);
                String deviceProtocolKey = deviceBaseId.toString() + "_tcp_record";
                if (gbRecorder != null) {
                    gbRecorder.stopRemux();
                    CacheUtil.gbPushObserver.remove(callId);
                    CacheUtil.deviceRecordMap.remove(deviceBaseId);
                    CacheUtil.gbServerMap.remove(deviceProtocolKey);
                    continue;
                }
                if (rtspRecorder != null) {
                    rtspRecorder.setInterrupted();
                    continue;
                }
                failDeviceIds.add(deviceBaseId);
            }
        }
        return failDeviceIds;
    }

    /**
     * 判断录像是否成功开启
     * @param result
     * @param deviceId
     * @return
     */
    private GBResult judgeRecordIsOpen(GBResult result, Integer deviceId) {
        int resultCode = result.getCode();
        if (200 == resultCode) {
            MediaData mediaData = (MediaData) result.getData();
            String callId = mediaData.getCallId();
            StreamUtils.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_RECORD);
            return GBResult.ok();
        } else {
            return result;
        }
    }
}
