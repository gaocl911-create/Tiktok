SET NAMES utf8mb4;

-- P0 data-scope and creator-baseline repair.
-- Safe to run repeatedly.

-- SELF includes the current user and recursive direct subordinates.
update sys_role
set data_scope = '5',
    update_time = now()
where role_key in ('comment', 'test2')
  and del_flag = '0'
  and data_scope <> '5';

-- Remove historical creator-discovery bindings that violate the target baseline.
-- Content rows and independent single-content targets are intentionally preserved.
update cm_monitor_target_content relation
join cm_monitor_target target on target.target_id = relation.target_id
join cm_content_post content on content.content_id = relation.content_id
set relation.status = 'removed',
    relation.update_time = now()
where relation.status = 'active'
  and target.status = 'active'
  and target.target_type = 'creator_collection'
  and target.baseline_time is not null
  and relation.relation_source = 'creator_discovery'
  and (
    content.publish_time is null
    or content.publish_time <= target.baseline_time
    or coalesce(relation.published_after_base, 0) <> 1
  );
