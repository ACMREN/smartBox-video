CREATE TABLE `camera_info`(
	id INT(10) AUTO_INCREMENT COMMENT '自增id',
	device_base_id INT(10) NOT NULL COMMENT '基础设备id',
	device_serial_num VARCHAR(30) DEFAULT NULL COMMENT '设备编号',
	parent_serial_num VARCHAR(30) DEFAULT NULL COMMENT '父设备编号',
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

CREATE TABLE `preset_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_base_id INT(10) NOT NULL COMMENT '基础设备id',
    preset_name VARCHAR(20) NOT NULL DEFAULT '' COMMENT '预置点名称',
    preset_pos TEXT COMMENT '预置点点位信息',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预置点信息表';

CREATE TABLE `record_video_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_base_id INT(10) NOT NULL COMMENT '基础设备id',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    file_size BIGINT(20) DEFAULT NULL COMMENT '文件大小',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='录像文件信息表';

CREATE TABLE `snapshot_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_base_id INT(10) NOT NULL COMMENT '基础设备id',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT(20) DEFAULT NULL COMMENT '文件大小',
    thumbnail_path VARCHAR(500) NOT NULL COMMENT '缩略图路径',
    create_time DATETIME DEFAULT NULL COMMENT '截图时间',
    `type` TINYINT(2) NOT NULL COMMENT '截图类型：1-报警截图，2-AI截图，3-人工截图',
    alarm_type TINYINT(2) DEFAULT 0 COMMENT '报警类型：1-人工视频报警； 2-运动目标检测报警； 3-遗留物检测报警； 4-物体移除检测报警； 5-绊线检测报警；6-入侵检测报警； 7-逆行检测报警； 8-徘徊检测报警； 9-流量统计报警； 10-密度检测报警； 11-视频异常检测报警； 12-快速移动报警',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='截图文件信息表';

CREATE TABLE `gb_server_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_serial_num VARCHAR(30) NOT NULL COMMENT '设备编号',
    name VARCHAR(20) DEFAULT NULL COMMENT '名称',
    project VARCHAR(20) DEFAULT NULL COMMENT '项目',
    domain VARCHAR(20) NOT NULL COMMENT '设备国标域',
    ip VARCHAR (20) NOT NULL COMMENT 'ip地址',
    port INT(10) NOT NULL COMMENT '端口',
    password VARCHAR(100) DEFAULT NULL COMMENT '认证密码',
    local_serial_num VARCHAR(30) DEFAULT NULL COMMENT '本地认证用户',
    local_ip VARCHAR(20) DEFAULT NULL COMMENT '本地ip',
    local_port INT(10) DEFAULT NULL COMMENT '本地端口',
    expire_time INT(10) NOT NULL COMMENT '过期时间',
    register_interval INT(10) NOT NULL COMMENT '注册间隔',
    heart_beat_interval INT(10) NOT NULL COMMENT '心跳间隔',
    catalog_size TINYINT(10) DEFAULT NULL COMMENT '目录分组大小',
    charset_code VARCHAR(10) NOT NULL COMMENT '字符集',
    type VARCHAR(20) DEFAULT 'direct' COMMENT '传输方式',
    trans_protocol VARCHAR(10) DEFAULT NULL COMMENT '传输协议',
    stream_protocol VARCHAR(10) DEFAULT NULL COMMENT '推流传输协议',
    status TINYINT(10) DEFAULT 0 COMMENT '在线状态：0-不在线，1-在线',
    camera_list VARCHAR(100) DEFAULT NULL COMMENT '摄像头列表',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    last_update_time DATETIME DEFAULT NULL COMMENT '最后更新时间',
    camera_num TINYINT(10) DEFAULT NULL COMMENT '监控数量',
    link_type TINYINT(2) DEFAULT 1 COMMENT '注册类型：0-链接，1-国标，2-平台',
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='父设备信息表';

CREATE TABLE `entity_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    type TINYINT(20) NOT NULL DEFAULT 1 COMMENT '三维实体类型：1-模型，2-多边形',
    name VARCHAR(20) DEFAULT NULL COMMENT '三维实体名称',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三维实体数据表';

CREATE TABLE `ar_config_info` (
    device_base_id INT(10) NOT NULL COMMENT '设备基础id',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(device_base_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ar基础设置表';

CREATE TABLE `ar_tag_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_base_id INT(10) NOT NULL COMMENT '设备基础id',
    type INT(10) DEFAULT NULL COMMENT '标签类型',
    name VARCHAR(20) DEFAULT NULL COMMENT '标签名称',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ar标签信息表';

CREATE TABLE `ar_template_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    template_name VARCHAR(20) DEFAULT NULL COMMENT '标签模板名称',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ar标签模板表';

CREATE TABLE `ar_style_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    style_name VARCHAR(20) DEFAULT NULL COMMENT '标签样式名称',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ar标签样式表';

CREATE TABLE `ar_scene_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    device_base_id INT(10) NOT NULL COMMENT '设备基础id',
    name VARCHAR(20) DEFAULT NULL COMMENT '场景名称',
    data TEXT DEFAULT NULL COMMENT '数据',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ar场景信息表';

CREATE TABLE `gb_client_info` (
    id INT(10) AUTO_INCREMENT COMMENT '自增id',
    ip VARCHAR(20) NOT NULL COMMENT '下级平台平台ip',
    device_serial_num VARCHAR(30) DEFAULT NULL COMMENT '下级平台sip编号',
    domain VARCHAR(20) DEFAULT NULL COMMENT '下级平台sip域',
    project VARCHAR(100) DEFAULT NULL COMMENT '项目名称',
    address VARCHAR(100) DEFAULT NULL COMMENT '地址名称',
    longitude VARCHAR(10) DEFAULT NULL COMMENT '经度',
    latitude VARCHAR(10) DEFAULT NULL COMMENT '纬度',
    camera_num INT(10) DEFAULT NULL COMMENT '监控数量',
    link_type TINYINT(2) DEFAULT 0 COMMENT '注册类型：0-链接，1-国标，2-平台',
	net_type TINYINT(2) DEFAULT 0 COMMENT '网络类型：0-局域网，1-互联网',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    last_update_time DATETIME DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY(id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子设备信息表';
