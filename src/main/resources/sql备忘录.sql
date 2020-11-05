CREATE TABLE `camera_info`(
	id INT(10) AUTO_INCREMENT COMMENT '自增id',
	device_base_id INT(10) NOT NULL COMMENT '基础设备id',
	rtsp_link VARCHAR(200) DEFAULT '' COMMENT 'rtsp链接',
	ip VARCHAR(20) DEFAULT '' COMMENT 'ip地址',
	device_name VARCHAR(20) DEFAULT '' COMMENT '设备名称',
	project VARCHAR(20) DEFAULT '' COMMENT '项目名称',
	link_status TINYINT(2) DEFAULT 0 COMMENT '注册状态：0-未注册，1-已注册',
	link_type TINYINT(2) DEFAULT 0 COMMENT '注册类型：0-链接，1-国标，2-平台',
	net_type TINYINT(2) DEFAULT 0 COMMENT '网络类型：0-局域网，1-互联网',
	PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄像头注册表';

CREATE TABLE `device_base_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    project VARCHAR(20) DEFAULT '' COMMENT '项目名称',
    device_name VARCHAR(20) DEFAULT '' COMMENT '设备名称',
    device_type TINYINT(2) DEFAULT 0 COMMENT '设备类型：1-枪机，2-球机，3-半球机',
    device_link TINYINT(2) DEFAULT 0 COMMENT '设备连接方式：0-有线，1-无线',
    address VARCHAR(100) DEFAULT '' COMMENT '设备地址',
    longitude VARCHAR(10) DEFAULT '' COMMENT '经度',
    latitude VARCHAR(10) DEFAULT '' COMMENT '纬度',
    reg_date DATE COMMENT '注册日期',
    specification VARCHAR(100) DEFAULT '' COMMENT '品牌型号',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备基础信息表';

CREATE TABLE `tree_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    user_id INT(10) NOT NULL COMMENT '用户id',
    tree_info TEXT COMMENT '树状图信息',
    polling_list TEXT COMMENT '轮询列表信息',
    tree_type TINYINT(2) DEFAULT 0 COMMENT '树状图类型：0-摄像头',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='树状图表';