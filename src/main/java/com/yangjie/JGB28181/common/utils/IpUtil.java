package com.yangjie.JGB28181.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtil {
	public static String IpConvert(String domainName) {
		String ip = domainName;
		try {
			ip = InetAddress.getByName(domainName).getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return domainName;
		}
		return ip;
	}
}
