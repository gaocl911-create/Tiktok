SET NAMES utf8mb4;

-- Creator monitoring menus and permissions for RuoYi-Vue-Plus.
-- Navigation is loaded dynamically from sys_menu. Re-run safely after menu changes.

DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2100 AND 2129;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2100 AND 2129;

INSERT INTO sys_menu VALUES
('2100', '抖音监测', '0', '0', 'douyin', NULL, '', 1, 0, 'M', '0', '0', '', 'monitor', 103, 1, SYSDATE(), NULL, NULL, '抖音账号与作品监测'),
('2101', '账号监测', '2100', '1', 'account', 'creator/account/index', '', 1, 0, 'C', '0', '0', 'creator:account:list', 'people', 103, 1, SYSDATE(), NULL, NULL, '抖音作者账号监测'),
('2102', '内容监测', '2100', '2', 'content', 'creator/content/index', '', 1, 0, 'C', '0', '0', 'creator:content:list', 'list', 103, 1, SYSDATE(), NULL, NULL, '抖音作品指标监测'),
('2103', '预警中心', '2100', '3', 'alerts', 'creator/alert/index', '', 1, 0, 'C', '0', '0', '', 'bug', 103, 1, SYSDATE(), NULL, NULL, '作品与采集预警'),
('2104', '采集运行', '2100', '4', 'runs', 'creator/runs/index', '', 1, 0, 'C', '0', '0', 'creator:target:list', 'time', 103, 1, SYSDATE(), NULL, NULL, '采集运行记录'),

('2110', '账号查询', '2101', '1', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:account:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2111', '账号新增', '2101', '2', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:account:add', '#', 103, 1, SYSDATE(), NULL, NULL, ''),

('2112', '作品查询', '2102', '1', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:content:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2113', '作品新增', '2102', '2', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:content:add', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2114', '作品采集', '2102', '3', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:target:collect', '#', 103, 1, SYSDATE(), NULL, NULL, ''),

('2115', '目标查询', '2104', '1', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:target:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2116', '目标新增', '2104', '2', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:target:add', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2117', '目标删除', '2104', '3', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:target:remove', '#', 103, 1, SYSDATE(), NULL, NULL, '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2100 AND 2129;
