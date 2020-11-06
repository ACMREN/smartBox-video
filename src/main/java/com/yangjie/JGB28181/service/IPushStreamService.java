package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.common.result.GBResult;

public interface IPushStreamService {

    GBResult pushStream(String deviceId, String channelId);

    GBResult rtspPushStream(String deviceId, String channelId, String rtspLink);

    GBResult closeStream(String callId);
}
