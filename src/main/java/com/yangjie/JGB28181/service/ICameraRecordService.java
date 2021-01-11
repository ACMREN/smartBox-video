package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.common.result.GBResult;

import javax.sip.SipException;
import java.util.List;

public interface ICameraRecordService {

    GBResult startCameraRecord(List<Integer> deviceIds);

    List<Integer> stopRecordStream(List<Integer> deviceBaseIds) throws SipException;
}
