table schema
```sql
CREATE TABLE `jwork` (
     `id` bigint NOT NULL AUTO_INCREMENT,
     `task_id` varchar(64) NOT NULL COMMENT '任务唯一 id',
     `queue` varchar(32) NOT NULL COMMENT '队列名称',
     `group` varchar(100) NOT NULL COMMENT '队列里的分组',
     `status` varchar(30) NOT NULL COMMENT '状态',
     `next_time_sec` bigint DEFAULT NULL COMMENT '下次执行时间，单位：秒',
     `data` text COMMENT '执行数据 json',
     `trigger` text COMMENT 'trigger 序列化数据',
     `retry_times` int NOT NULL DEFAULT '0' COMMENT '重试次数',
     `create_time` timestamp NOT NULL,
     `update_time` timestamp NOT NULL,
     `result` text,
     PRIMARY KEY (`id`),
     UNIQUE KEY `jwork_unique` (`task_id`),
     KEY `jwork_queue_IDX` (`queue`, `status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=107 DEFAULT CHARSET=utf8mb4;

```