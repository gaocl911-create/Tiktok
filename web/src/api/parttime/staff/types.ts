export interface PtStaffProfile extends BaseEntity {
  profileId: string | number;
  userId: string | number;
  tenantId?: string;
  realName?: string;
  phone?: string;
  phoneVerified?: number;
  wechatId?: string;
  region?: string;
  douyinId?: string;
  inviterUserId?: string | number;
  onboardingStatus?: StaffOnboardingStatus;
  auditBy?: string | number;
  auditAt?: string;
  rejectReason?: string;
  remark?: string;
  nickname?: string;
  avatarUrl?: string;
  deptName?: string;
}

export type StaffOnboardingStatus = 'incomplete' | 'pending' | 'approved' | 'rejected' | 'disabled';

export interface PtStaffQuery extends PageQuery {
  userId?: string | number;
  realName?: string;
  onboardingStatus?: StaffOnboardingStatus | '';
}
