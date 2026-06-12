import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type {
  CollectionRun,
  AlertEvent,
  AlertRule,
  AlertRuleForm,
  ContentLinkForm,
  ContentPost,
  ContentSnapshot,
  CreatorAccount,
  CreatorMonitorForm,
  MonitorTarget
} from './types';

export const listCreatorAccounts = (params: Record<string, unknown>): AxiosPromise<CreatorAccount[]> =>
  request({
    url: '/creator/account/list',
    method: 'get',
    params
  });

export const getCreatorAccount = (creatorId: string): AxiosPromise<CreatorAccount> =>
  request({
    url: `/creator/account/${creatorId}`,
    method: 'get'
  });

export const collectCreatorProfile = (creatorId: string) =>
  request({
    url: `/creator/account/${creatorId}/collect-profile`,
    method: 'post'
  });

export const addCreatorMonitor = (data: CreatorMonitorForm) =>
  request({
    url: '/creator/account/monitor',
    method: 'post',
    data
  });

export const deleteCreatorMonitors = (creatorIds: string[]) =>
  request({
    url: `/creator/account/${creatorIds.join(',')}`,
    method: 'delete'
  });

export const listContentPosts = (params: Record<string, unknown>): AxiosPromise<ContentPost[]> =>
  request({
    url: '/creator/content/list',
    method: 'get',
    params
  });

export const getContentPost = (contentId: string): AxiosPromise<ContentPost> =>
  request({
    url: `/creator/content/${contentId}`,
    method: 'get'
  });

export const listContentSnapshots = (contentId: string, limit = 200): AxiosPromise<ContentSnapshot[]> =>
  request({
    url: `/creator/content/${contentId}/snapshots`,
    method: 'get',
    params: { limit }
  });

export const getContentTarget = (contentId: string): AxiosPromise<MonitorTarget> =>
  request({
    url: `/creator/content/${contentId}/target`,
    method: 'get'
  });

export const listContentRuns = (contentId: string, limit = 100): AxiosPromise<CollectionRun[]> =>
  request({
    url: `/creator/content/${contentId}/runs`,
    method: 'get',
    params: { limit }
  });

export const addContentLink = (data: ContentLinkForm) =>
  request({
    url: '/creator/content/link',
    method: 'post',
    data
  });

export const deleteContentMonitors = (contentIds: string[]) =>
  request({
    url: `/creator/content/${contentIds.join(',')}`,
    method: 'delete'
  });

export const listMonitorTargets = (params: Record<string, unknown>): AxiosPromise<MonitorTarget[]> =>
  request({
    url: '/creator/target/list',
    method: 'get',
    params
  });

export const collectTarget = (targetId: string) =>
  request({
    url: `/creator/target/${targetId}/collect`,
    method: 'post'
  });

export const collectDueTargets = (limit = 100): AxiosPromise<number> =>
  request({
    url: '/creator/target/collect-due',
    method: 'post',
    params: { limit }
  });

export const listTargetRuns = (targetId: string, limit = 30): AxiosPromise<CollectionRun[]> =>
  request({
    url: `/creator/target/${targetId}/runs`,
    method: 'get',
    params: { limit }
  });

export const listAlertRules = (params: Record<string, unknown>): AxiosPromise<AlertRule[]> =>
  request({
    url: '/creator/alert/rule/list',
    method: 'get',
    params
  });

export const addAlertRule = (data: AlertRuleForm): AxiosPromise<AlertRule> =>
  request({
    url: '/creator/alert/rule',
    method: 'post',
    data
  });

export const updateAlertRule = (data: AlertRuleForm): AxiosPromise<AlertRule> =>
  request({
    url: '/creator/alert/rule',
    method: 'put',
    data
  });

export const deleteAlertRule = (ruleId: string) =>
  request({
    url: `/creator/alert/rule/${ruleId}`,
    method: 'delete'
  });

export const listAlertEvents = (params: Record<string, unknown>): AxiosPromise<AlertEvent[]> =>
  request({
    url: '/creator/alert/event/list',
    method: 'get',
    params
  });

export const handleAlertEvent = (eventId: string, data: { status: AlertEvent['status']; handleNote?: string }): AxiosPromise<AlertEvent> =>
  request({
    url: `/creator/alert/event/${eventId}/handle`,
    method: 'put',
    data
  });
