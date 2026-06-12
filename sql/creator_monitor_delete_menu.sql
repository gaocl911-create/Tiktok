-- Creator monitor cancellation permissions.
-- 2118-2122 are reserved for alert permissions, so cancellation uses 2130-2131.

DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT menu_id FROM sys_menu
    WHERE perms IN ('creator:account:remove', 'creator:content:remove')
);
DELETE FROM sys_menu
WHERE perms IN ('creator:account:remove', 'creator:content:remove');

INSERT INTO sys_menu VALUES
('2118', 'Alert query', '2103', '1', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:list', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2119', 'Alert rule add', '2103', '2', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:alert:rule:add', '#', 103, 1, SYSDATE(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    perms = VALUES(perms),
    update_time = SYSDATE();

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2118, 2119);

DELETE FROM sys_role_menu WHERE menu_id IN (2130, 2131);
DELETE FROM sys_menu WHERE menu_id IN (2130, 2131);

INSERT INTO sys_menu VALUES
('2130', 'Cancel account monitor', '2101', '3', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:account:remove', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
('2131', 'Cancel content monitor', '2102', '4', '', NULL, '', 1, 0, 'F', '0', '0', 'creator:content:remove', '#', 103, 1, SYSDATE(), NULL, NULL, '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2130, 2131);
