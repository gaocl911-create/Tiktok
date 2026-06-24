import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { MaterialCategoryType, PtMaterialCategory, PtMaterialCategoryForm, PtMaterialCategoryQuery } from './types';

export const listMaterialCategories = (params: PtMaterialCategoryQuery): AxiosPromise<PtMaterialCategory[]> =>
  request({
    url: '/parttime/material/category/list',
    method: 'get',
    params
  });

export const listMaterialCategoryOptions = (categoryType: MaterialCategoryType): AxiosPromise<PtMaterialCategory[]> =>
  request({
    url: '/parttime/material/category/options',
    method: 'get',
    params: { categoryType }
  });

export const getMaterialCategory = (categoryId: string | number): AxiosPromise<PtMaterialCategory> =>
  request({
    url: `/parttime/material/category/${categoryId}`,
    method: 'get'
  });

export const addMaterialCategory = (data: PtMaterialCategoryForm): AxiosPromise<PtMaterialCategory> =>
  request({
    url: '/parttime/material/category',
    method: 'post',
    data
  });

export const updateMaterialCategory = (data: PtMaterialCategoryForm): AxiosPromise<PtMaterialCategory> =>
  request({
    url: '/parttime/material/category',
    method: 'put',
    data
  });

export const deleteMaterialCategory = (categoryIds: string | number | Array<string | number>) =>
  request({
    url: `/parttime/material/category/${categoryIds}`,
    method: 'delete'
  });
