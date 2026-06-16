import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { PtPromotionTask, PtPromotionTaskForm, PtPromotionTaskQuery } from './types';

export const listParttimeTasks = (params: PtPromotionTaskQuery): AxiosPromise<PtPromotionTask[]> =>
  request({
    url: '/parttime/task/list',
    method: 'get',
    params
  });

export const getParttimeTask = (taskId: string | number): AxiosPromise<PtPromotionTask> =>
  request({
    url: `/parttime/task/${taskId}`,
    method: 'get'
  });

export const addParttimeTask = (data: PtPromotionTaskForm): AxiosPromise<PtPromotionTask> =>
  request({
    url: '/parttime/task',
    method: 'post',
    data
  });

export const updateParttimeTask = (data: PtPromotionTaskForm): AxiosPromise<PtPromotionTask> =>
  request({
    url: '/parttime/task',
    method: 'put',
    data
  });

export const publishParttimeTask = (taskId: string | number): AxiosPromise<PtPromotionTask> =>
  request({
    url: `/parttime/task/${taskId}/publish`,
    method: 'post'
  });

export const pauseParttimeTask = (taskId: string | number): AxiosPromise<PtPromotionTask> =>
  request({
    url: `/parttime/task/${taskId}/pause`,
    method: 'post'
  });

export const finishParttimeTask = (taskId: string | number): AxiosPromise<PtPromotionTask> =>
  request({
    url: `/parttime/task/${taskId}/finish`,
    method: 'post'
  });
