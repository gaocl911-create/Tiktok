import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { PtTaskSubmission, PtTaskSubmissionQuery } from './types';

export const listParttimeSubmissions = (params: PtTaskSubmissionQuery): AxiosPromise<PtTaskSubmission[]> =>
  request({
    url: '/parttime/submission/list',
    method: 'get',
    params
  });

export const getParttimeSubmission = (submissionId: string | number): AxiosPromise<PtTaskSubmission> =>
  request({
    url: `/parttime/submission/${submissionId}`,
    method: 'get'
  });

export const approveParttimeSubmission = (submissionId: string | number): AxiosPromise<PtTaskSubmission> =>
  request({
    url: `/parttime/submission/${submissionId}/approve`,
    method: 'post'
  });

export const rejectParttimeSubmission = (submissionId: string | number, rejectReason: string): AxiosPromise<PtTaskSubmission> =>
  request({
    url: `/parttime/submission/${submissionId}/reject`,
    method: 'post',
    params: { rejectReason }
  });
