package com.yangjie.JGB28181.service.impl;

import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.service.IPushStreamService;
import org.springframework.stereotype.Component;

@Component
public class PushRtmpStreamServiceImpl implements IPushStreamService {
    @Override
    public GBResult pushStream(String deviceId, String channelId) {
        return null;
    }

    @Override
    public GBResult rtspPushStream(String deviceId, String channelId, String rtspLink) {
        return null;
    }

    @Override
    public GBResult closeStream(String callId) {
        return null;
    }
}
