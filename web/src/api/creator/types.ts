export interface CreatorAccount {
  creatorId: string;
  platform: string;
  platformCreatorId: string;
  platformUserId?: string;
  platformDisplayId?: string;
  nickname: string;
  avatarUrl?: string;
  signature?: string;
  homepageUrl?: string;
  ipLocation?: string;
  gender?: string;
  followerCount?: number;
  followingCount?: number;
  totalFavoritedCount?: number;
  contentCount?: number;
  profileStatus?: string;
  lastProfileCollectAt?: string;
  lastContentScanAt?: string;
  addedByName?: string;
  contactWechat?: string;
}

export interface ContentPost {
  contentId: string;
  creatorId?: string;
  platform: string;
  platformContentId: string;
  contentType?: string;
  title?: string;
  description?: string;
  coverUrl?: string;
  contentUrl?: string;
  shareUrl?: string;
  publishTime?: string;
  firstSeenAt?: string;
  addedSource?: string;
  latestLikeCount?: number;
  latestCommentCount?: number;
  latestCollectCount?: number;
  latestShareCount?: number;
  metricsStatus?: string;
  lastMetricsCollectAt?: string;
}

export interface ContentSnapshot {
  snapshotId: string;
  contentId: string;
  targetId?: string;
  collectedAt: string;
  likeCount?: number;
  commentCount?: number;
  collectCount?: number;
  shareCount?: number;
  likeDelta?: number;
  commentDelta?: number;
  collectDelta?: number;
  shareDelta?: number;
  metricsStatus?: string;
  missingMetricFields?: string;
}

export interface MonitorTarget {
  targetId: string;
  targetType: 'creator_collection' | 'single_content';
  platform: string;
  targetName: string;
  creatorId?: string;
  contentId?: string;
  discoverNewContent?: boolean;
  profileCollectIntervalMin?: number;
  contentCollectIntervalMin?: number;
  status?: string;
  dataStatus?: string;
  lastProfileCollectAt?: string;
  lastContentCollectAt?: string;
  lastDiscoveryAt?: string;
  nextContentCollectAt?: string;
  remark?: string;
  contactWechat?: string;
  tags?: string;
}

export interface CollectionRun {
  runId: string;
  runType: string;
  triggerSource: string;
  provider: string;
  targetId?: string;
  creatorId?: string;
  contentId?: string;
  status: string;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  discoveredCount?: number;
  collectedCount?: number;
  failedCount?: number;
  apiCallCount?: number;
  estimatedCostCny?: number;
  errorMessage?: string;
}

export interface CreatorMonitorForm {
  platform: 'douyin';
  profileInput: string;
  targetName?: string;
  remark?: string;
  contactWechat?: string;
  tags?: string;
  profileCollectIntervalMin?: number;
  contentCollectIntervalMin?: number;
}

export interface ContentLinkForm {
  platform: 'douyin';
  contentInput: string;
  targetName?: string;
  remark?: string;
  tags?: string;
  contentCollectIntervalMin?: number;
}

export interface AlertRule {
  ruleId: string;
  ruleName: string;
  metricType: 'like' | 'comment';
  ruleType: 'cumulative' | 'window_growth';
  windowMinutes?: number;
  thresholdValue: number;
  scopeType: 'all' | 'creator' | 'content';
  scopeId?: string;
  severity: 'normal' | 'important' | 'urgent';
  cooldownMinutes: number;
  enabled: boolean;
  createTime?: string;
}

export interface AlertRuleForm extends Omit<AlertRule, 'ruleId' | 'createTime'> {
  ruleId?: string;
}

export interface AlertEvent {
  eventId: string;
  ruleId: string;
  contentId: string;
  creatorId?: string;
  targetId?: string;
  snapshotId?: string;
  eventTitle: string;
  contentTitle?: string;
  creatorNickname?: string;
  metricType: 'like' | 'comment';
  ruleType: 'cumulative' | 'window_growth';
  windowMinutes?: number;
  thresholdValue: number;
  observedValue: number;
  windowStartAt?: string;
  windowEndAt?: string;
  severity: 'normal' | 'important' | 'urgent';
  status: 'pending' | 'tracking' | 'resolved' | 'ignored';
  triggerCount?: number;
  firstTriggeredAt: string;
  lastTriggeredAt: string;
  handledAt?: string;
  handleNote?: string;
}
