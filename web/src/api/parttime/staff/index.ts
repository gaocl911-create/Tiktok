import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { PtStaffProfile, PtStaffQuery } from './types';

export const listParttimeStaff = (params: PtStaffQuery): AxiosPromise<PtStaffProfile[]> =>
  request({
    url: '/parttime/staff/list',
    method: 'get',
    params
  });

export const approveParttimeStaff = (profileId: string | number): AxiosPromise<PtStaffProfile> =>
  request({
    url: `/parttime/staff/${profileId}/approve`,
    method: 'put'
  });

export const rejectParttimeStaff = (profileId: string | number, rejectReason: string): AxiosPromise<PtStaffProfile> =>
  request({
    url: `/parttime/staff/${profileId}/reject`,
    method: 'put',
    params: { rejectReason }
  });
