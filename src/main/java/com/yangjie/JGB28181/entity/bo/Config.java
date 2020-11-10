package com.yangjie.JGB28181.entity.bo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@PropertySource("classpath:config.properties")
public class Config {
    @Value("${config.keepalive}")
    private String keepalive;// 保活时长（分钟）
    @Value("${config.push_host}")
    private String push_host;// 推送地址
    @Value("${config.host_extra}")
    private String host_extra;// 额外地址
    @Value("${config.push_port}")
    private String push_port;// 推送端口
    @Value("${config.main_code}")
    private String main_code;// 主码流最大码率
    @Value("${config.sub_code}")
    private String sub_code;// 主码流最大码率
    @Value("${config.streamMediaIp}")
    private String streamMediaIp;// 推流地址ip
}
