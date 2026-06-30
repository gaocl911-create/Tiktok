<template>
  <div class="login">
    <section class="login-brand">
      <div class="login-brand-head">
        <div class="login-brand-mark">CM</div>
        <div class="login-brand-title">
          <h1>创作者监测</h1>
          <p>Creator Monitoring</p>
        </div>
      </div>
      <div class="login-brand-glow" aria-hidden="true" />
      <div class="login-brand-copy">
        <span class="login-brand-eyebrow">REAL-TIME CREATOR INTELLIGENCE</span>
        <strong>专注作品数据变化</strong>
        <span>持续监控账号动态、作品互动指标和采集运行状态。</span>
        <div class="login-brand-tags">
          <em>内容监测</em>
          <em>采集运行</em>
          <em>预警追踪</em>
        </div>
      </div>
      <div class="login-signal" aria-hidden="true">
        <i v-for="height in signalBars" :key="height" :style="{ height: `${height}%` }" />
      </div>
    </section>

    <section class="login-panel">
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="title-box">
          <div class="login-form-icon" aria-hidden="true">
            <span />
          </div>
          <div>
            <span>WELCOME BACK</span>
            <h3 class="title">登录{{ title }}</h3>
            <p>输入账号信息，进入创作者数据工作台。</p>
          </div>
        </div>

        <el-form-item v-if="tenantEnabled" prop="tenantId">
          <el-select v-model="loginForm.tenantId" filterable :placeholder="proxy.$t('login.selectPlaceholder')" style="width: 100%">
            <el-option v-for="item in tenantList" :key="item.tenantId" :label="item.companyName" :value="item.tenantId"></el-option>
            <template #prefix><svg-icon icon-class="company" class="el-input__icon input-icon" /></template>
          </el-select>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" :placeholder="proxy.$t('login.username')">
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            :placeholder="proxy.$t('login.password')"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code" class="captcha-item">
          <el-input
            v-model="loginForm.code"
            size="large"
            auto-complete="off"
            :placeholder="proxy.$t('login.code')"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
          </el-input>
          <button class="login-code" type="button" title="点击刷新验证码" @click="getCode">
            <img :src="codeUrl" class="login-code-img" />
          </button>
        </el-form-item>
        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">{{ proxy.$t('login.rememberPassword') }}</el-checkbox>
        </div>
        <el-form-item class="login-action">
          <el-button :loading="loading" size="large" type="primary" style="width: 100%" @click.prevent="handleLogin">
            <span v-if="!loading">{{ proxy.$t('login.login') }}</span>
            <span v-else>{{ proxy.$t('login.logging') }}</span>
          </el-button>
          <div v-if="register" class="register-link">
            <router-link class="link-type" :to="'/register'">{{ proxy.$t('login.switchRegisterPage') }}</router-link>
          </div>
        </el-form-item>
      </el-form>
      <!--  底部  -->
      <div class="el-login-footer">
        <span>Creator Monitoring System</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { getCodeImg, getTenantList } from '@/api/login';
import { authRouterUrl } from '@/api/system/social/auth';
import { useUserStore } from '@/store/modules/user';
import { LoginData, TenantVO } from '@/api/types';
import { to } from 'await-to-js';
import { HttpStatus } from '@/enums/RespEnum';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const title = import.meta.env.VITE_APP_TITLE;
const userStore = useUserStore();
const router = useRouter();
const { t } = useI18n();

const loginForm = ref<LoginData>({
  tenantId: '000000',
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
} as LoginData);

const signalBars = [24, 32, 28, 46, 40, 58, 54, 72, 63, 82, 74, 92, 86, 100];

const loginRules: ElFormRules = {
  tenantId: [{ required: true, trigger: 'blur', message: t('login.rule.tenantId.required') }],
  username: [{ required: true, trigger: 'blur', message: t('login.rule.username.required') }],
  password: [{ required: true, trigger: 'blur', message: t('login.rule.password.required') }],
  code: [{ required: true, trigger: 'change', message: t('login.rule.code.required') }]
};

const codeUrl = ref('');
const loading = ref(false);
// 验证码开关
const captchaEnabled = ref(true);
// 租户开关
const tenantEnabled = ref(true);

// 注册开关
const register = ref(false);
const redirect = ref('/');
const loginRef = ref<ElFormInstance>();
// 租户列表
const tenantList = ref<TenantVO[]>([]);

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    redirect.value = newRoute.query && newRoute.query.redirect && decodeURIComponent(newRoute.query.redirect);
  },
  { immediate: true }
);

const handleLogin = () => {
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      // 勾选了"记住我"：只持久化租户和用户名，密码绝不落 localStorage。
      // 历史实现把明文密码塞进 localStorage，等同于浏览器一被劫持就丢账号。
      if (loginForm.value.rememberMe) {
        localStorage.setItem('tenantId', String(loginForm.value.tenantId));
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        // 否则移除
        localStorage.removeItem('tenantId');
        localStorage.removeItem('username');
        localStorage.removeItem('rememberMe');
      }
      // 历史遗留的明文密码记录，强制清除一次。
      localStorage.removeItem('password');
      // 调用action的登录方法
      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        const redirectUrl = redirect.value || '/';
        await router.push(redirectUrl);
        loading.value = false;
      } else {
        loading.value = false;
        // 重新获取验证码
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      // 不打印整个 fields（含明文密码），只标记校验失败。
      if (import.meta.env.DEV) console.warn('[login] validation failed');
    }
  });
};

/**
 * 获取验证码
 */
const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    // 刷新验证码时清空输入框
    loginForm.value.code = '';
    codeUrl.value = 'data:image/gif;base64,' + data.img;
    loginForm.value.uuid = data.uuid;
  }
};

const getLoginData = () => {
  const tenantId = localStorage.getItem('tenantId');
  const username = localStorage.getItem('username');
  const rememberMe = localStorage.getItem('rememberMe');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? String(loginForm.value.username) : username,
    password: '',
    rememberMe: rememberMe === null ? false : Boolean(rememberMe)
  } as LoginData;
};

/**
 * 获取租户列表
 */
const initTenantList = async () => {
  const { data } = await getTenantList(false);
  tenantEnabled.value = data.tenantEnabled === undefined ? true : data.tenantEnabled;
  if (tenantEnabled.value) {
    tenantList.value = data.voList;
    if (tenantList.value != null && tenantList.value.length !== 0) {
      loginForm.value.tenantId = tenantList.value[0].tenantId;
    }
  }
};

/**
 * 第三方登录
 * @param type
 */
const doSocialLogin = (type: string) => {
  authRouterUrl(type, loginForm.value.tenantId).then((res: any) => {
    if (res.code === HttpStatus.SUCCESS) {
      // 获取授权地址跳转
      window.location.href = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

onMounted(() => {
  getCode();
  initTenantList();
  getLoginData();
});
</script>

<style lang="scss" scoped>
.login {
  position: relative;
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(460px, 1fr);
  min-height: 100%;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 18%, rgba(255, 255, 255, 0.9) 0, rgba(255, 255, 255, 0) 260px),
    linear-gradient(135deg, #f8fafc 0%, #f4f4f5 52%, #eef2f7 100%);
}

.login-brand {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 54px;
  color: #fff;
  background:
    linear-gradient(145deg, rgba(16, 185, 129, 0.14), transparent 34%),
    radial-gradient(circle at 82% 18%, rgba(59, 130, 246, 0.22), transparent 34%),
    #101010;
}

.login-brand::before,
.login-brand::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.login-brand::before {
  inset: 0;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(to bottom, #000 0%, transparent 86%);
}

.login-brand::after {
  right: -180px;
  bottom: -180px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  filter: blur(2px);
}

.login-brand-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
}

.login-brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 10px;
  color: #111;
  background: #fff;
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.28);
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.login-brand-title h1,
.login-brand-title p,
.login-brand-copy strong,
.login-brand-copy span {
  margin: 0;
}

.login-brand-title h1 {
  font-size: 20px;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.login-brand-title p {
  margin-top: 4px;
  color: #8a8a8a;
  font-size: 11px;
  letter-spacing: 0.02em;
}

.login-brand-glow {
  position: absolute;
  top: 18%;
  right: 8%;
  width: 280px;
  height: 280px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.08), transparent 68%);
}

.login-brand-copy {
  position: absolute;
  z-index: 1;
  left: 54px;
  right: 54px;
  bottom: 27%;
  max-width: 560px;
}

.login-brand-eyebrow {
  display: inline-block;
  margin-bottom: 18px;
  color: #8b8b8b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.login-brand-copy strong {
  display: block;
  max-width: 520px;
  font-size: clamp(34px, 4.5vw, 58px);
  line-height: 1.04;
  font-weight: 760;
  letter-spacing: -0.055em;
}

.login-brand-copy > span:not(.login-brand-eyebrow) {
  display: block;
  max-width: 440px;
  margin-top: 18px;
  color: #a3a3a3;
  font-size: 14px;
  line-height: 1.8;
}

.login-brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.login-brand-tags em {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  color: #d4d4d4;
  background: rgba(255, 255, 255, 0.05);
  font-size: 12px;
  font-style: normal;
}

.login-signal {
  position: absolute;
  right: 54px;
  bottom: 48px;
  left: 54px;
  z-index: 1;
  display: flex;
  height: 96px;
  align-items: flex-end;
  gap: 8px;
  opacity: 0.42;
}

.login-signal i {
  flex: 1;
  min-width: 12px;
  min-height: 6px;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(180deg, #fff, rgba(255, 255, 255, 0.52));
}

.login-panel {
  position: relative;
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-form {
  position: relative;
  z-index: 1;
  width: min(380px, 100%);
  padding: 32px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 24px 70px rgba(15, 23, 42, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.title-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  text-align: center;
}

.login-form-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  background: linear-gradient(180deg, #fff, #f7f7f8);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.login-form-icon span {
  width: 20px;
  height: 20px;
  border: 5px solid #18181b;
  border-radius: 50%;
}

.title-box span {
  color: #8a8a8a;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.title-box .title {
  margin: 6px 0 0;
  color: #18181b;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 760;
  letter-spacing: -0.035em;
}

.title-box p {
  margin: 8px 0 0;
  color: #71717a;
  font-size: 13px;
  line-height: 1.6;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-input),
.login-form :deep(.el-select) {
  height: 42px;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px #e4e4e7 inset;
  transition:
    box-shadow 180ms ease,
    background-color 180ms ease;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.login-form :deep(.el-input__wrapper.is-focus),
.login-form :deep(.el-select__wrapper.is-focused) {
  box-shadow:
    0 0 0 1px #18181b inset,
    0 0 0 4px rgba(24, 24, 27, 0.08);
}

.login-form :deep(.el-input__inner) {
  color: #18181b;
  font-size: 14px;
}

.input-icon {
  width: 15px;
  height: 42px;
  margin-left: 0;
  color: #a1a1aa;
}

.captcha-item :deep(.el-form-item__content) {
  display: grid;
  grid-template-columns: 1fr 104px;
  gap: 10px;
}

.login-code {
  height: 42px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #fff;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.login-code:hover {
  border-color: #18181b;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.login-code:active {
  transform: scale(0.98);
}

.login-code-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-options {
  display: flex;
  justify-content: space-between;
  margin: 2px 0 22px;
}

.login-options :deep(.el-checkbox__label) {
  color: #52525b;
  font-size: 13px;
}

.login-action {
  margin-bottom: 0;
}

.login-action :deep(.el-button--primary) {
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: #242424;
  box-shadow: 0 12px 28px rgba(24, 24, 27, 0.18);
  font-weight: 700;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    background-color 180ms ease;
}

.login-action :deep(.el-button--primary:hover) {
  background: #111;
  box-shadow: 0 16px 34px rgba(24, 24, 27, 0.24);
  transform: translateY(-1px);
}

.login-action :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.register-link {
  width: 100%;
  margin-top: 12px;
  text-align: right;
}

.el-login-footer {
  position: absolute;
  right: 0;
  bottom: 20px;
  left: 0;
  text-align: center;
  color: #a1a1aa;
  font-family: Arial, serif;
  font-size: 12px;
}

:global(html.dark) {
  .login {
    background: #111827;
  }

  .login-panel {
    background: linear-gradient(135deg, #111827 0%, #18181b 100%);
  }

  .login-form {
    border-color: rgba(148, 163, 184, 0.16);
    background: rgba(24, 24, 27, 0.9);
    box-shadow: 0 24px 70px rgba(0, 0, 0, 0.35);
  }

  .title-box .title {
    color: #f4f4f5;
  }

  .title-box p,
  .login-options :deep(.el-checkbox__label) {
    color: #a1a1aa;
  }

  .login-form-icon {
    border-color: rgba(148, 163, 184, 0.18);
    background: rgba(255, 255, 255, 0.06);
  }

  .login-form-icon span {
    border-color: #f4f4f5;
  }

  .login-form :deep(.el-input__wrapper),
  .login-form :deep(.el-select__wrapper),
  .login-code {
    background: rgba(255, 255, 255, 0.06);
    box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.22) inset;
  }

  .login-form :deep(.el-input__inner) {
    color: #f4f4f5;
  }

  .login-action :deep(.el-button--primary) {
    background: #f4f4f5;
    color: #111827;
  }
}

@media (max-width: 1024px) {
  .login {
    grid-template-columns: 44% 56%;
  }

  .login-brand {
    padding: 40px;
  }

  .login-brand-copy {
    left: 40px;
    right: 40px;
  }

  .login-signal {
    right: 40px;
    left: 40px;
  }
}

@media (max-width: 860px) {
  .login {
    display: flex;
    min-height: 100%;
  }

  .login-brand {
    display: none;
  }

  .login-panel {
    width: 100%;
    min-height: 100%;
    padding: 28px 20px 56px;
  }

  .login-form {
    width: min(400px, 100%);
    padding: 28px 22px;
  }

  .el-login-footer {
    bottom: 12px;
  }
}
</style>
