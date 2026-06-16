import { request } from "@/utils/request";

export interface MiniappLoginResult {
  access_token: string;
  expire_in: number;
  client_id: string;
  openid: string;
}

export interface MiniappLoginPayload {
  code?: string;
  appid?: string;
  tenantId?: string;
  mockOpenid?: string;
}

export const miniappLogin = (data: MiniappLoginPayload) =>
  request<MiniappLoginResult>({
    url: "/miniapp/auth/login",
    method: "POST",
    data,
  });
