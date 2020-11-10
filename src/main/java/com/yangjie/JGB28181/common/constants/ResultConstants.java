package com.yangjie.JGB28181.common.constants;

public interface ResultConstants {

	int SUCCESS_CODE = 200;
	String SUCCESS = "成功";


	int DEVICE_OFF_LINE_CODE = 311;
	String DEVICE_OFF_LINE = "设备离线";

	int SYSTEM_ERROR_CODE = 500;
	String SYSTEM_ERROR = "系统异常";

	int NOT_FOUND_DEVICE_CODE = 4041;
	String NOT_FOUND_DEVICE = "找不到设备";

	int COMMAND_NO_RESPONSE_CODE = 313;
	String COMMAND_NO_RESPONSE = "指令未响应";
	
	int CHANNEL_NO_EXIST_CODE = 314;
	String CHANNEL_NO_EXIST = "通道不存在";

	int DEVICE_NO_EXIST_CODE = 315;
	String DEVICE_NO_EXIST = "设备不存在";

	int DEVICE_NO_PUSH_STREAM_CODE = 316;
	String DEVICE_NO_PUSH_STREAM = "设备不在推流";

}
