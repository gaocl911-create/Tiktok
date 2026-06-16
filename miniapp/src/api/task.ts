import { type PageResult, request } from "@/utils/request";

export type TaskStatus = "draft" | "published" | "paused" | "finished";
export type ClaimStatus = "claimed" | "submitted" | "approved" | "rejected";
export type SubmissionStatus = "pending" | "approved" | "rejected";

export interface PromotionTask {
  taskId: number;
  taskTitle: string;
  platform: string;
  taskDesc?: string;
  taskRequirement?: string;
  unitPrice?: number | string;
  totalQuota?: number;
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
  claimId: number;
  taskId: number;
  profileId: number;
  userId: number;
  claimStatus: ClaimStatus;
  claimTime?: string;
  submitTime?: string;
  finishTime?: string;
  taskTitle?: string;
  platform?: string;
  realName?: string;
  phone?: string;
}

export interface TaskSubmission {
  submissionId: number;
  claimId: number;
  taskId: number;
  platform: string;
  contentUrl: string;
  contentDesc?: string;
  screenshotUrl?: string;
  submissionStatus: SubmissionStatus;
  submitTime?: string;
  rejectReason?: string;
  monitorContentId?: number;
  monitorTargetId?: number;
  monitorRunId?: number;
  taskTitle?: string;
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

export const claimTask = (taskId: number) =>
  request<TaskClaim>({
    url: `/miniapp/task/${taskId}/claim`,
    method: "POST",
  });

export const listMyTasks = (params: { pageNum: number; pageSize: number }) =>
  request<PageResult<TaskClaim>>({
    url: "/miniapp/task/my",
    method: "GET",
    data: params,
  });

export const submitTaskContent = (claimId: number, data: SubmitContentPayload) =>
  request<TaskSubmission>({
    url: `/miniapp/task/claim/${claimId}/submit-content`,
    method: "POST",
    data,
  });
