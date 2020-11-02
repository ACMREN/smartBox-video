package com.yangjie.JGB28181.common.constants;

public interface BaseConstants {
    /**
     * hls推流物理路径
     */
    String hlsStreamPath = "/tmp/hls/";

    String rtmpBaseUrl = "rtmp://127.0.0.1:1935/live/";

    String ONVIF_IPV4_REGEX = "http://[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+\\/onvif/device_service";

    String IPV4_REGEX = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+";

    String CATALOG_ITEM_REGEX = "(<Item[\\s\\S]+)?</Item>";

}
