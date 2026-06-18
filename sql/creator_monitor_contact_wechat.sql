alter table cm_monitor_target
    add column contact_wechat varchar(128) default null comment 'contact wechat account' after remark;
