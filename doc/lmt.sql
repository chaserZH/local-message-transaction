CREATE TABLE `t_status_transaction_record` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
`exec_status` tinyint(4) DEFAULT '0' COMMENT '执行状态 0.待执行 1.执行成功 2.执行失败',
`first_exec_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '首次执行时间',
`biz_scene` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '业务场景描述',
`biz_scene_code` varchar(128) COLLATE utf8mb4_bin DEFAULT '' COMMENT '业务场景code',
`exec_times` int(11) NOT NULL DEFAULT '0' COMMENT '执行次数',
`retry_times` int(11) DEFAULT '0' COMMENT '重试次数',
`biz_id` varchar(128) COLLATE utf8mb4_bin DEFAULT '' COMMENT '业务ID,幂等键',
`biz_content` text COLLATE utf8mb4_bin COMMENT '业务内容',
`error_message` text COLLATE utf8mb4_bin COMMENT '错误信息',
`is_delete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除，执行成功之后，会将此记录删除 1.已删除 0.未删除',
`create_time` bigint(20) DEFAULT '0' COMMENT '创建时间',
`update_time` bigint(20) DEFAULT '0' COMMENT '更新时间',
PRIMARY KEY (`id`),
UNIQUE KEY `unq_biz_scene_code_biz_id` (`biz_scene_code`,`biz_id`),
KEY `idx_exec_status` (`exec_status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='本地消息事务表';