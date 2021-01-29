package com.yangjie.JGB28181.common.result;


import static com.yangjie.JGB28181.common.constants.ResultConstants.*;

public class GBResult {
	private static final long serialVersionUID = 1L;
	private int code;
	private String msg;
	private Object data;

	public static GBResult build(int status, String msg, Object data) {
		return new GBResult(status, msg, data);
	}

	public static GBResult ok(Object data) {
		return new GBResult(SUCCESS_CODE, SUCCESS, data);
	}

	public static GBResult ok() {
		return new GBResult(SUCCESS_CODE, SUCCESS);
	}

	public static GBResult fail() {
		return new GBResult(SYSTEM_ERROR_CODE, SYSTEM_ERROR);
	}

	public GBResult() {

	}

	public static GBResult build(int status, String msg) {
		return new GBResult(status, msg, null);
	}

	public GBResult(int code, String msg, Object data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public GBResult(Object data) {
		this.code = 200;
		this.msg = "OK";
		this.data = data;
	}
	public GBResult(String msg,Object data){
		this.code = 200;
		this.msg = msg;
		this.data = data;
	}
	public GBResult(int code,String msg){
		this.code = code;
		this.msg = msg;
		this.data = null;
	} 

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "GBResult{" +
				"code=" + code +
				", msg='" + msg + '\'' +
				", data=" + data.toString() +
				'}';
	}
}
