SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS cm_alert_rule
(
    rule_id             bigint(20)      NOT NULL,
    tenant_id           varchar(20)     DEFAULT '000000',
    rule_name           varchar(128)    NOT NULL,
    metric_type         varchar(32)     NOT NULL COMMENT 'like,comment',
    rule_type           varchar(32)     NOT NULL COMMENT 'cumulative,window_growth',
    window_minutes      int(11)         DEFAULT NULL,
    threshold_value     bigint(20)      NOT NULL,
    scope_type          varchar(32)     DEFAULT 'all' COMMENT 'all,creator,content',
    scope_id            bigint(20)      DEFAULT NULL,
    severity            varchar(32)     DEFAULT 'important',
    cooldown_minutes    int(11)         DEFAULT 120,
    enabled             tinyint(1)      DEFAULT 1,
    create_dept         bigint(20)      DEFAULT NULL,
    create_by           bigint(20)      DEFAULT NULL,
    create_time         datetime        DEFAULT NULL,
    update_by           bigint(20)      DEFAULT NULL,
    update_time         datetime        DEFAULT NULL,
    del_flag            char(1)         DEFAULT '0',
    PRIMARY KEY (rule_id),
    KEY idx_cm_alert_rule_enabled (tenant_id, enabled, metric_type),
    KEY idx_cm_alert_rule_scope (tenant_id, scope_type, scope_id)
) ENGINE=InnoDB COMMENT='creator monitor - alert rule';

CREATE TABLE IF NOT EXISTS cm_alert_event
(
    event_id            bigint(20)      NOT NULL,
    tenant_id           varchar(20)     DEFAULT '000000',
    rule_id             bigint(20)      NOT NULL,
    content_id          bigint(20)      NOT NULL,
    creator_id          bigint(20)      DEFAULT NULL,
    target_id           bigint(20)      DEFAULT NULL,
    snapshot_id         bigint(20)      DEFAULT NULL,
    event_title         varchar(255)    NOT NULL,
    content_title       varchar(512)    DEFAULT NULL,
    creator_nickname    varchar(128)    DEFAULT NULL,
    metric_type         varchar(32)     NOT NULL,
    rule_type           varchar(32)     NOT NULL,
    window_minutes      int(11)         DEFAULT NULL,
    threshold_value     bigint(20)      NOT NULL,
    observed_value      bigint(20)      NOT NULL,
    window_start_at     datetime        DEFAULT NULL,
    window_end_at       datetime        DEFAULT NULL,
    severity            varchar(32)     DEFAULT 'important',
    status              varchar(32)     DEFAULT 'pending',
    trigger_count       int(11)         DEFAULT 1,
    first_triggered_at  datetime        NOT NULL,
    last_triggered_at   datetime        NOT NULL,
    handled_by          bigint(20)      DEFAULT NULL,
    handled_at          datetime        DEFAULT NULL,
    handle_note         varchar(1000)   DEFAULT NULL,
    create_dept         bigint(20)      DEFAULT NULL,
    create_by           bigint(20)      DEFAULT NULL,
    create_time         datetime        DEFAULT NULL,
    update_by           bigint(20)      DEFAULT NULL,
    update_time         datetime        DEFAULT NULL,
    PRIMARY KEY (event_id),
    KEY idx_cm_alert_event_status (tenant_id, status, severity, last_triggered_at),
    KEY idx_cm_alert_event_content (tenant_id, content_id, last_triggered_at),
    KEY idx_cm_alert_event_rule (tenant_id, rule_id, content_id, last_triggered_at)
) ENGINE=InnoDB COMMENT='creator monitor - alert event';

UPDATE sys_menu SET perms = 'creator:alert:list' WHERE menu_id = 2103;

DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2118 AND 2122;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2118 AND 2122;

INSERT INTO sys_menu VALUES
('2118', 'Alert query', '2103', '1', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:list', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2119', 'Alert rule add', '2103', '2', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:rule:add', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2120', 'Alert rule edit', '2103', '3', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:rule:edit', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2121', 'Alert rule remove', '2103', '4', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:rule:remove', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2122', 'Alert event handle', '2103', '5', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:event:handle', '#', 103, 1, SYSDATE(), NULL, NULL, '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2118 AND 2122;
