package com.yangjie.JGB28181.message.helper;

import java.util.Date;


import com.yangjie.JGB28181.common.utils.DateUtils;

import gov.nist.javax.sip.header.SIPHeader;

public class CustomSIPDateHeader extends SIPHeader{

	@Override
	protected String encodeBody() {
		return null;
	}
}
