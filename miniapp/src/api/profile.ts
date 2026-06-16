import { request } from "@/utils/request";

export type OnboardingStatus = "incomplete" | "pending" | "approved" | "rejected";

export interface StaffProfile {
  profileId?: number;
  userId?: number;
  tenantId?: string;
  realName?: string;
  phone?: string;
  phoneVerified?: number;
  wechatId?: string;
  region?: string;
  douyinId?: string;
  onboardingStatus?: OnboardingStatus;
  rejectReason?: string;
  remark?: string;
  nickname?: string;
  avatarUrl?: string;
  deptName?: string;
}

export interface StaffProfilePayload {
  realName?: string;
  phone?: string;
  wechatId?: string;
  region?: string;
  douyinId?: string;
  remark?: string;
}

export const getMyProfile = (showError = true) =>
  request<StaffProfile>({
    url: "/miniapp/user/profile",
    method: "GET",
    showError,
  });

export const updateMyProfile = (data: StaffProfilePayload) =>
  request<StaffProfile>({
    url: "/miniapp/user/profile",
    method: "PUT",
    data,
  });

export const submitMyProfile = () =>
  request<StaffProfile>({
    url: "/miniapp/user/profile/submit",
    method: "POST",
  });
