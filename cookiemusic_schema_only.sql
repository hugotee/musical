/*
 CookieMusic schema-only SQL
 Generated from the current project database schema.
 Contains DDL only, without INSERT data.
 Use this file for ER diagram generation or schema review.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `music_creation`  (
  `creation_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创作ID',
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `prompt` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提示词',
  `lyrics` varchar(1500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '歌词',
  `model` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型',
  `music_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '音乐类型 0:音乐 1:纯音乐',
  `mode_type` tinyint(1) NULL DEFAULT NULL COMMENT '模式 0:简单模式 1:专家模式',
  `settings` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '设置信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`creation_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '音乐创作信息' ROW_FORMAT = DYNAMIC;

CREATE TABLE `music_info`  (
  `music_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '音乐ID',
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `task_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID',
  `creation_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创作ID',
  `music_title` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `cover` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面',
  `audio_path` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '音乐地址',
  `duration` int(11) NULL DEFAULT NULL COMMENT '持续时间',
  `lyrics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '歌词',
  `play_count` int(11) NULL DEFAULT 0 COMMENT '播放数量',
  `good_count` int(11) NULL DEFAULT 0 COMMENT '点赞数',
  `commend_type` tinyint(1) NULL DEFAULT 0 COMMENT '0:未推荐 1:已推荐',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `music_status` tinyint(1) NULL DEFAULT 0 COMMENT '0:生成音乐中 1:生成完毕',
  `music_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '音乐类型 0:音乐 1:纯音乐',
  PRIMARY KEY (`music_id`) USING BTREE,
  UNIQUE INDEX `idx_key_task_id`(`task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '音乐信息' ROW_FORMAT = DYNAMIC;

CREATE TABLE `music_info_action`  (
  `action_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '操作ID',
  `music_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '音乐ID',
  `music_user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '音乐用户ID',
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `action_type` tinyint(1) NULL DEFAULT NULL COMMENT '操作类型1:点赞',
  PRIMARY KEY (`action_id`) USING BTREE,
  UNIQUE INDEX `idx_key_user_music_id`(`music_id`, `user_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '音乐操作' ROW_FORMAT = DYNAMIC;

CREATE TABLE `pay_code_info`  (
  `pay_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付码',
  `amount` decimal(15, 2) NULL DEFAULT NULL COMMENT '金额',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `use_user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '使用用户ID',
  `use_time` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态 0:待使用 1:已使用',
  PRIMARY KEY (`pay_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付码' ROW_FORMAT = DYNAMIC;

CREATE TABLE `pay_order_info`  (
  `order_id` varchar(28) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '支付了行',
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `product_id` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品ID',
  `product_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `amount` decimal(5, 2) NULL DEFAULT NULL COMMENT '金额',
  `channel_order_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付通道订单ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '订单创建时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '0:待支付 1:支付完成',
  `integral` int(11) NULL DEFAULT NULL COMMENT '购买积分',
  `pay_info` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付信息',
  `pay_type` tinyint(1) NULL DEFAULT NULL COMMENT '支付类型',
  PRIMARY KEY (`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付订单信息' ROW_FORMAT = DYNAMIC;

CREATE TABLE `product_info`  (
  `product_id` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品ID',
  `product_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `cover` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面',
  `price` decimal(5, 2) NULL DEFAULT NULL COMMENT '价格',
  `product_description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品描述',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `onsale_type` tinyint(1) NULL DEFAULT NULL COMMENT '上架类型',
  `integral` int(11) NULL DEFAULT NULL COMMENT '购买积分',
  `sort` int(11) NULL DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`product_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品信息' ROW_FORMAT = DYNAMIC;

CREATE TABLE `sys_dict`  (
  `dict_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典编号',
  `dict_pcode` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父级字典ID',
  `dict_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典值',
  `dict_desc` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典描述',
  `sort` tinyint(1) NULL DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`dict_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 101 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统字典' ROW_FORMAT = DYNAMIC;

CREATE TABLE `user_info`  (
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `nick_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户头像',
  `password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `integral` int(11) NULL DEFAULT 0 COMMENT '积分',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `idx_key_email`(`email`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息' ROW_FORMAT = DYNAMIC;

CREATE TABLE `user_integral_record`  (
  `record_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `user_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `change_integral` int(11) NULL DEFAULT NULL COMMENT '积分',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `record_type` tinyint(4) NULL DEFAULT NULL COMMENT '记录类型 0:创作失败退回 1:创作消耗 2:充值 3:系统赠送',
  `business_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务ID',
  `amount` decimal(5, 2) NULL DEFAULT NULL COMMENT '充值金额',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10042 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户积分记录信息' ROW_FORMAT = DYNAMIC;
SET FOREIGN_KEY_CHECKS = 1;
