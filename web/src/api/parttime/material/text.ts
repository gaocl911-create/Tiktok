import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { PtMaterialText, PtMaterialTextForm, PtMaterialTextQuery } from './types';

export const listMaterialTexts = (params: PtMaterialTextQuery): AxiosPromise<PtMaterialText[]> =>
  request({
    url: '/parttime/material/text/list',
    method: 'get',
    params
  });

export const getMaterialText = (textId: string | number): AxiosPromise<PtMaterialText> =>
  request({
    url: `/parttime/material/text/${textId}`,
    method: 'get'
  });

export const addMaterialText = (data: PtMaterialTextForm): AxiosPromise<PtMaterialText> =>
  request({
    url: '/parttime/material/text',
    method: 'post',
    data
  });

export const importMaterialTexts = (data: FormData) =>
  request({
    url: '/parttime/material/text/importData',
    method: 'post',
    data
  });

export const updateMaterialText = (data: PtMaterialTextForm): AxiosPromise<PtMaterialText> =>
  request({
    url: '/parttime/material/text',
    method: 'put',
    data
  });

export const deleteMaterialText = (textIds: string | number | Array<string | number>) =>
  request({
    url: `/parttime/material/text/${textIds}`,
    method: 'delete'
  });
