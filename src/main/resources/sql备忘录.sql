CREATE TABLE `camera_info`(
	id INT(10) AUTO_INCREMENT COMMENT '自增id',
	ip VARCHAR(20) DEFAULT '' COMMENT 'ip地址',
	device_name VARCHAR(20) DEFAULT '' COMMENT '设备名称',
	project VARCHAR(20) DEFAULT '' COMMENT '项目名称',
	link_status TINYINT(2) DEFAULT 0 COMMENT '注册状态：0-未注册，1-已注册',
	link_type TINYINT(2) DEFAULT 0 COMMENT '注册类型：0-链接，1-国标，2-平台',
	net_type TINYINT(2) DEFAULT 0 COMMENT '网络类型：0-局域网，1-互联网',
	PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;