package com.yangjie.JGB28181.common.thread;

import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.utils.IDUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.entity.GbServerInfo;
import com.yangjie.JGB28181.message.SipLayer;
import org.springframework.util.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

public class HeartbeatThread extends Thread {

    private boolean sendKeepAlive = false;

    private boolean registering = false;

    private SipLayer sipLayer;

    private GbServerInfo gbServerInfo;

    @Override
    public void run() {
        String higherServerSerialNum = gbServerInfo.getDeviceSerialNum();
        try {
            while (sendKeepAlive || registering) {
                String value = RedisUtil.get(SipLayer.SERVER_DEVICE_PREFIX + higherServerSerialNum);
                if (sendKeepAlive) {
                    Thread.sleep(BaseConstants.MS_OF_MINUTE);
                    // 判断redis中的上级级联平台是否已经过期
                    if (!StringUtils.isEmpty(value)) {
                        sipLayer.sendKeepAlive(20);
                    } else {
                        this.sendKeepAlive = false;
                        this.registering = true;
                    }
                }

                // 如果keepAlive没有回应，则进行注册操作
                if (registering) {
                    Thread.sleep(BaseConstants.MS_OF_MINUTE);
                    String callId = IDUtils.id();
                    String fromTag = IDUtils.id();
                    sipLayer.sendRegister(gbServerInfo.getDeviceSerialNum(), gbServerInfo.getDomain(), gbServerInfo.getIp(),
                            gbServerInfo.getPort().toString(), gbServerInfo.getPassword(),
                            callId, fromTag, null, null, null, 1);
                    if (!StringUtils.isEmpty(value)) {
                        this.sendKeepAlive = true;
                        this.registering = false;
                    }
                }
            }
        } catch (SipException e) {
            e.printStackTrace();
        } catch (ParseException e1) {
            e1.printStackTrace();
        } catch (InvalidArgumentException e2) {
            e2.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setSipLayer(SipLayer sipLayer) {
        this.sipLayer = sipLayer;
    }

    public void stopSendKeepAlive() {
        this.sendKeepAlive = false;
        this.registering = false;
    }

    public void startSendKeepAlive(GbServerInfo gbServerInfo) {
        this.sendKeepAlive = true;
        this.gbServerInfo = gbServerInfo;
        SipLayer.higherServerHeartbeatMap.put(gbServerInfo.getDeviceSerialNum(), this);
        this.start();
    }
}
