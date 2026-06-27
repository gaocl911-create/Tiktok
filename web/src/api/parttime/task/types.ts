export type PromotionTaskStatus = 'draft' | 'published' | 'paused' | 'finished';
export type PromotionTaskClaimLimitType = 'once' | 'limited' | 'unlimited';

export interface PtPromotionTask extends BaseEntity {
  taskId: string | number;
  tenantId?: string;
  taskTitle: string;
  platform: 'douyin' | string;
  taskDesc?: string;
  taskRequirement?: string;
  unitPrice: number;
  totalQuota: number;
  claimLimitType?: PromotionTaskClaimLimitType | string;
  claimLimitCount?: number;
  claimedCount?: number;
  submittedCount?: number;
  approvedCount?: number;
  startTime?: string;
  endTime?: string;
  taskStatus: PromotionTaskStatus;
  publishTime?: string;
  pauseTime?: string;
  finishTime?: string;
  textCategoryId?: string | number;
  imageCategoryId?: string | number;
  textCategoryName?: string;
  imageCategoryName?: string;
  remark?: string;
}

export interface PtPromotionTaskQuery extends PageQuery {
  taskTitle?: string;
  platform?: string;
  taskStatus?: PromotionTaskStatus | '';
}

export interface PtPromotionTaskForm {
  taskId?: string | number;
  taskTitle: string;
  platform: 'douyin' | string;
  taskDesc?: string;
  taskRequirement?: string;
  unitPrice: number;
  totalQuota: number;
  claimLimitType: PromotionTaskClaimLimitType;
  claimLimitCount: number;
  startTime?: string;
  endTime?: string;
  textCategoryId?: string | number;
  imageCategoryId?: string | number;
  remark?: string;
}
