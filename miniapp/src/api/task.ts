import { type PageResult, request } from "@/utils/request";

export type TaskStatus = "draft" | "published" | "paused" | "finished";
export type ClaimStatus = "claimed" | "submitted" | "approved" | "rejected";
export type TaskClaimGroup = "pending" | "completed";
export type SubmissionStatus = "pending" | "approved" | "rejected";

export interface PromotionTask {
  taskId: number | string;
  taskTitle: string;
  platform: string;
  taskDesc?: string;
  taskRequirement?: string;
  unitPrice?: number | string;
  totalQuota?: number;
  claimLimitType?: "once" | "limited" | "unlimited" | string;
  claimLimitCount?: number;
  claimedCount?: number;
  submittedCount?: number;
  approvedCount?: number;
  startTime?: string;
  endTime?: string;
  taskStatus: TaskStatus;
  publishTime?: string;
  remark?: string;
}

export interface TaskClaim {
  claimId: number | string;
  taskId: number | string;
  profileId: number | string;
  userId: number | string;
  claimStatus: ClaimStatus;
  claimTime?: string;
  submitTime?: string;
  finishTime?: string;
  taskTitle?: string;
  platform?: string;
  realName?: string;
  phone?: string;
  claimRound?: number;
  assignIndex?: number;
  textId?: number | string;
  assignedText?: string;
  imageId?: number | string;
  assignedImageUrl?: string;
  assignedImageName?: string;
  submissionId?: number | string;
  contentUrl?: string;
  contentDesc?: string;
  screenshotUrl?: string;
  submissionStatus?: SubmissionStatus;
  auditTime?: string;
  rejectReason?: string;
  monitorContentId?: number | string;
}

export interface TaskSubmission {
  submissionId: number | string;
  claimId: number | string;
  taskId: number | string;
  platform: string;
  contentUrl: string;
  contentDesc?: string;
  screenshotUrl?: string;
  submissionStatus: SubmissionStatus;
  submitTime?: string;
  rejectReason?: string;
  monitorContentId?: number | string;
  monitorTargetId?: number | string;
  monitorRunId?: number | string;
  taskTitle?: string;
  assignIndex?: number;
  textId?: number | string;
  assignedText?: string;
  imageId?: number | string;
  assignedImageUrl?: string;
  assignedImageName?: string;
}

export interface SubmitContentPayload {
  contentUrl: string;
  contentDesc?: string;
  screenshotUrl?: string;
  remark?: string;
}

export const listPublishedTasks = (params: { pageNum: number; pageSize: number }) =>
  request<PageResult<PromotionTask>>({
    url: "/miniapp/task/list",
    method: "GET",
    data: params,
  });

export const claimTask = (taskId: number | string) =>
  request<TaskClaim>({
    url: `/miniapp/task/${taskId}/claim`,
    method: "POST",
  });

export const listMyTasks = (params: { pageNum: number; pageSize: number; group?: TaskClaimGroup }) =>
  request<PageResult<TaskClaim>>({
    url: "/miniapp/task/my",
    method: "GET",
    data: params,
  });

export const getMyTaskClaim = (claimId: number | string) =>
  request<TaskClaim>({
    url: `/miniapp/task/claim/${claimId}`,
    method: "GET",
  });

export const submitTaskContent = (claimId: number | string, data: SubmitContentPayload) =>
  request<TaskSubmission>({
    url: `/miniapp/task/claim/${claimId}/submit-content`,
    method: "POST",
    data,
  });
