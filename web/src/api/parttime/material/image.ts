import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { PtMaterialImage, PtMaterialImageForm, PtMaterialImageQuery } from './types';

export const listMaterialImages = (params: PtMaterialImageQuery): AxiosPromise<PtMaterialImage[]> =>
  request({
    url: '/parttime/material/image/list',
    method: 'get',
    params
  });

export const getMaterialImage = (imageId: string | number): AxiosPromise<PtMaterialImage> =>
  request({
    url: `/parttime/material/image/${imageId}`,
    method: 'get'
  });

export const addMaterialImage = (data: PtMaterialImageForm): AxiosPromise<PtMaterialImage> =>
  request({
    url: '/parttime/material/image',
    method: 'post',
    data
  });

export const updateMaterialImage = (data: PtMaterialImageForm): AxiosPromise<PtMaterialImage> =>
  request({
    url: '/parttime/material/image',
    method: 'put',
    data
  });

export const deleteMaterialImage = (imageIds: string | number | Array<string | number>) =>
  request({
    url: `/parttime/material/image/${imageIds}`,
    method: 'delete'
  });
