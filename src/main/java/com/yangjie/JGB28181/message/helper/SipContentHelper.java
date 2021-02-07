 package com.yangjie.JGB28181.message.helper;

 import java.util.Map;
 import java.util.Set;

 public class SipContentHelper {

	public static String generateCatalogContent(String deviceId,String sn){
        StringBuffer content = new StringBuffer(200);
        content.append("<?xml version=\"1.0\"?>\r\n");
        content.append("<Query>\r\n");
        content.append("<CmdType>Catalog</CmdType>\r\n");
        content.append("<SN>"+sn+"</SN>\r\n");
        content.append("<DeviceID>"+deviceId+"</DeviceID>\r\n");
        content.append("</Query>");
        return content.toString();
	}
	public static String generateRealTimeMeidaStreamInviteContent(String sessionId,String ip,int port,boolean isTcp,boolean isActive,String sessionName,String ssrc){
        StringBuffer content = new StringBuffer(200);
         content.append("v=0\r\n");
         content.append("o="+sessionId+" 0 0 IN IP4 "+ip+"\r\n");
         content.append("s="+sessionName+"\r\n");
         content.append("c=IN IP4 "+ip+"\r\n");
         content.append("t=0 0\r\n");
         content.append("m=video "+port+" "+(isTcp?"TCP/":"")+"RTP/AVP 96 98 97\r\n");
         content.append("a=sendrecv\r\n");
         content.append("a=rtpmap:96 PS/90000\r\n");
         content.append("a=rtpmap:98 H264/90000\r\n");
         content.append("a=rtpmap:97 MPEG4/90000\r\n");
         if(isTcp){
             content.append("a=setup:"+(isActive?"active\r\n":"passive\r\n"));
             content.append("a=connection:new\r\n");
         }
         content.append("y="+ssrc+"\r\n");
        return content.toString();
	}

	public static String generateInviteMediaOKContent(String serverId, String serverIp, int serverPort, String password, String sessionName, String ssrc) {
	    StringBuffer content = new StringBuffer(200);
	    content.append("v=0\r\n");
	    content.append("o=" + serverId + " 0 0 IN IP4 " + serverIp + "\r\n");
	    content.append("s=" + sessionName + "\r\n");
	    content.append("c=IN IP4 " + serverIp + "\r\n");
	    content.append("t=0 0" + "\r\n");
	    content.append("m=video " + serverPort + " RTP/AVP 96" + "\r\n");
	    content.append("a=setup:active" + "\r\n");
	    content.append("a=sendonly" + "\r\n");
	    content.append("a=rtpmap 96 PS/90000" + "\r\n");
	    content.append("a=username:" + serverId + "\r\n");
	    content.append("a=password" + password + "\r\n");
	    content.append("a=filesize:0" + "\r\n");
	    content.append("y=" + ssrc + "\r\n");
	    content.append("f=" + "\r\n");
	    return content.toString();
    }

	public static String generateKeepAliveContent(String clientId, String sn) {
        StringBuffer content = new StringBuffer(200);
        content.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
        content.append("<Notify>\r\n");
        content.append("<CmdType>Keepalive</CmdType>\r\n");
        content.append("<SN>"+sn+"</SN>\r\n");
        content.append("<Status>OK</Status>\r\n");
        content.append("<DeviceID>"+clientId+"</DeviceID>\r\n");
        content.append("<Info>\r\n");
        content.append("</Info>\r\n");
        content.append("</Notify>");
        return content.toString();
    }

    public static String generateResponseCatalogContent(String deviceId, String sn, boolean sendSelf, Set<String> subDeviceCatalogSet) {
	    int deviceNum = subDeviceCatalogSet.size();
	    if (sendSelf) {
	        deviceNum++;
        }
	    StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
        content.append("<Response>\r\n");
        content.append("<CmdType>Catalog</CmdType>\r\n");
        content.append("<SN>" + sn + "</SN>\r\n");
        content.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        content.append("<SumNum>" + deviceNum + "</SumNum>\r\n");
        content.append("<DeviceList Num=\"" + deviceNum + "\">\r\n");
        if (sendSelf) {
            content.append("<Item>\r\n");
            content.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
            content.append("<Name>Box 01</Name>\r\n");
            content.append("<Manufacturer>SmartCity</Manufacturer>\r\n");
            content.append("<Model>Edge Box</Model>\r\n");
            content.append("<Owner>Owner</Owner>\r\n");
            content.append("<CivilCode>CivilCode</CivilCode>\r\n");
            content.append("<Address>Address</Address>\r\n");
            content.append("<Parental>0</Parental>\r\n");
            content.append("<ParentID>" + deviceId + "</ParentID>\r\n");
            content.append("<SafetyWay>0</SafetyWay>\r\n");
            content.append("<RegisterWay>1</RegisterWay>\r\n");
            content.append("<Secrecy>0</Secrecy>\r\n");
            content.append("<Status>ON</Status>\r\n");
            content.append("</Item>\r\n");
        }
        for (String catalogInfo : subDeviceCatalogSet) {
            content.append(catalogInfo).append("\r\n");
        }
        content.append("</DeviceList>\r\n");
        content.append("</Response>\r\n");
        return content.toString();
    }
}
