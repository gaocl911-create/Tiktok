export type MaterialCategoryType = 'text' | 'image';
export type MaterialStatus = '0' | '1';

export interface PtMaterialCategory extends BaseEntity {
  categoryId: string | number;
  tenantId?: string;
  categoryType: MaterialCategoryType;
  categoryName: string;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}

export interface PtMaterialCategoryQuery extends PageQuery {
  categoryType?: MaterialCategoryType | '';
  categoryName?: string;
  status?: MaterialStatus | '';
}

export interface PtMaterialCategoryForm {
  categoryId?: string | number;
  categoryType: MaterialCategoryType;
  categoryName: string;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}

export interface PtMaterialText extends BaseEntity {
  textId: string | number;
  tenantId?: string;
  categoryId: string | number;
  categoryName?: string;
  content: string;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}

export interface PtMaterialTextQuery extends PageQuery {
  categoryId?: string | number;
  content?: string;
  status?: MaterialStatus | '';
}

export interface PtMaterialTextForm {
  textId?: string | number;
  categoryId?: string | number;
  content: string;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}

export interface PtMaterialImage extends BaseEntity {
  imageId: string | number;
  tenantId?: string;
  categoryId: string | number;
  categoryName?: string;
  imageUrl: string;
  imageName?: string;
  imageSize?: number;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}

export interface PtMaterialImageQuery extends PageQuery {
  categoryId?: string | number;
  imageName?: string;
  status?: MaterialStatus | '';
}

export interface PtMaterialImageForm {
  imageId?: string | number;
  categoryId?: string | number;
  imageUrl: string;
  imageName?: string;
  imageSize?: number;
  sort?: number;
  status?: MaterialStatus;
  remark?: string;
}
