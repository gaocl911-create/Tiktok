export type SubmissionStatus = 'pending' | 'approved' | 'rejected';

export interface PtTaskSubmission extends BaseEntity {
  submissionId: string | number;
  tenantId?: string;
  claimId: string | number;
  taskId: string | number;
  profileId: string | number;
  userId: string | number;
  platform: 'douyin' | string;
  contentUrl: string;
  contentDesc?: string;
  screenshotUrl?: string;
  submissionStatus: SubmissionStatus;
  submitTime?: string;
  auditBy?: string | number;
  auditTime?: string;
  rejectReason?: string;
  monitorContentId?: string | number;
  monitorTargetId?: string | number;
  monitorRunId?: string | number;
  remark?: string;
  taskTitle?: string;
  realName?: string;
  phone?: string;
  douyinId?: string;
  assignIndex?: number;
  textId?: string | number;
  assignedText?: string;
  imageId?: string | number;
  assignedImageUrl?: string;
  assignedImageName?: string;
}

export interface PtTaskSubmissionQuery extends PageQuery {
  taskId?: string | number;
  userId?: string | number;
  platform?: string;
  contentUrl?: string;
  submissionStatus?: SubmissionStatus | '';
}
