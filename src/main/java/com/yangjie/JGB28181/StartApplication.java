package com.yangjie.JGB28181;

import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.media.server.remux.RtspToRtmpPusher;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Date;


@SpringBootApplication
@EnableScheduling
@MapperScan("com.yangjie.JGB28181.mapper")
public class StartApplication
{
	public static void main( String[] args )
	{
		// 将服务启动时间存入缓存
		CacheUtil.STARTTIME = new Date().getTime();
		// 将上下文传入CameraPush类中，用于检测tcp连接是否正常
		final ApplicationContext applicationContext = SpringApplication.run(StartApplication.class, args);
		RtspToRtmpPusher.setApplicationContext(applicationContext);
	}
}
