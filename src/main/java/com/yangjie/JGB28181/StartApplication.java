package com.yangjie.JGB28181;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@MapperScan("com.yangjie.JGB28181.mapper")
public class StartApplication
{
	public static void main( String[] args )
	{
		SpringApplication.run(StartApplication.class, args);
	}
}
