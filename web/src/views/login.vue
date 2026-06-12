<template>
  <div class="login">
    <section class="login-brand">
      <div class="login-brand-mark">CM</div>
      <div class="login-brand-title">
        <h1>创作者监测</h1>
        <p>Creator Monitoring</p>
      </div>
      <div class="login-brand-copy">
        <strong>专注作品数据变化</strong>
        <span>持续监控账号动态、作品互动指标和采集运行状态。</span>
      </div>
      <div class="login-signal" aria-hidden="true">
        <i v-for="height in signalBars" :key="height" :style="{ height: `${height}%` }" />
      </div>
    </section>
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="title-box">
        <div>
          <span>WELCOME BACK</span>
          <h3 class="title">登录{{ title }}</h3>
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
      <el-form-item v-if="captchaEnabled" prop="code">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          :placeholder="proxy.$t('login.code')"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" class="login-code-img" @click="getCode" />
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin: 0 0 25px 0">{{ proxy.$t('login.rememberPassword') }}</el-checkbox>
      <el-form-item style="width: 100%">
        <el-button :loading="loading" size="large" type="primary" style="width: 100%" @click.prevent="handleLogin">
          <span v-if="!loading">{{ proxy.$t('login.login') }}</span>
          <span v-else>{{ proxy.$t('login.logging') }}</span>
        </el-button>
        <div v-if="register" style="float: right">
          <router-link class="link-type" :to="'/register'">{{ proxy.$t('login.switchRegisterPage') }}</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Creator Monitoring System</span>
    </div>
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
      // 勾选了需要记住密码设置在 localStorage 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        localStorage.setItem('tenantId', String(loginForm.value.tenantId));
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('password', String(loginForm.value.password));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        // 否则移除
        localStorage.removeItem('tenantId');
        localStorage.removeItem('username');
        localStorage.removeItem('password');
        localStorage.removeItem('rememberMe');
      }
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
      console.log('error submit!', fields);
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
  const password = localStorage.getItem('password');
  const rememberMe = localStorage.getItem('rememberMe');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? String(loginForm.value.username) : username,
    password: password === null ? String(loginForm.value.password) : String(password),
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
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url('../assets/images/login-background.jpg');
  background-size: cover;
  background-position: center;
}

.title-box {
  display: flex;
  align-items: center;
  gap: 8px;

  .title {
    margin: 0px auto 26px auto;
    text-align: center;
    color: var(--el-text-color-primary);
    font-weight: 600;
    letter-spacing: 0.5px;
  }

  :deep(.lang-select--style) {
    line-height: 0;
    color: var(--el-text-color-secondary);
  }
}

.login-form {
  border-radius: var(--app-radius-lg);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.5);
  width: min(420px, 90vw);
  padding: 32px 30px 12px 30px;
  z-index: 1;
  box-shadow: var(--app-shadow-lg);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-form :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.9);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.login-form :deep(.el-button--primary) {
  border-radius: var(--app-radius-md);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25);
}

.login-form :deep(.el-button.is-circle) {
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: var(--el-text-color-regular);
}

.login-form :deep(.el-button.is-circle:hover) {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.2);
}

.login-code {
  width: calc(37% - 10px);
  height: 40px;
  float: right;
  margin-left: 10px;
  box-sizing: border-box;
  border-radius: var(--app-radius-sm);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--el-border-color-light);

  img {
    cursor: pointer;
    vertical-align: middle;
    display: block;
    width: 100%;
    height: 40px;
    object-fit: cover;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.75);
  font-family: Arial, serif;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 40px;
  padding-left: 0;
}

:global(html.dark) {
  .login-form {
    background: rgba(17, 24, 39, 0.9);
    border-color: rgba(148, 163, 184, 0.2);
  }

  .login-form :deep(.el-input__wrapper) {
    background-color: rgba(17, 24, 39, 0.7);
  }

  .login-form :deep(.el-button.is-circle) {
    background: rgba(148, 163, 184, 0.12);
    border-color: rgba(148, 163, 184, 0.25);
    color: #e5e7eb;
  }

  .el-login-footer {
    color: rgba(226, 232, 240, 0.65);
  }
}

.login {
  justify-content: flex-end;
  padding: 40px 9vw;
  background: #f5f5f4;
}

.login-brand {
  position: absolute;
  inset: 0 50% 0 0;
  overflow: hidden;
  padding: 54px;
  color: #fff;
  background: #111;
}

.login-brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 5px;
  color: #111;
  background: #fff;
  font-size: 16px;
  font-weight: 750;
}

.login-brand-title {
  position: absolute;
  top: 56px;
  left: 108px;
}

.login-brand h1,
.login-brand p {
  margin: 0;
}

.login-brand h1 {
  font-size: 20px;
}

.login-brand p {
  margin-top: 2px;
  color: #737373;
  font-size: 11px;
}

.login-brand-copy {
  position: absolute;
  left: 54px;
  bottom: 30%;
  max-width: 460px;
}

.login-brand-copy strong,
.login-brand-copy span {
  display: block;
}

.login-brand-copy strong {
  font-size: 32px;
  line-height: 42px;
  font-weight: 650;
}

.login-brand-copy span {
  margin-top: 12px;
  color: #a3a3a3;
  font-size: 14px;
  line-height: 24px;
}

.login-signal {
  position: absolute;
  right: 54px;
  bottom: 54px;
  left: 54px;
  height: 90px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  opacity: 0.38;
}

.login-signal i {
  flex: 1;
  min-height: 4px;
  background: #fff;
}

.login-form {
  width: min(420px, 42vw);
  padding: 36px 34px 20px;
  border: 1px solid #dedede;
  border-radius: 6px;
  box-shadow: none;
  backdrop-filter: none;
}

.title-box {
  margin-bottom: 28px;
}

.title-box > div {
  width: 100%;
}

.title-box span {
  color: #8a8a8a;
  font-size: 11px;
  font-weight: 700;
}

.title-box .title {
  margin: 6px 0 0;
  text-align: left;
  font-size: 24px;
}

.login-form :deep(.el-button--primary) {
  height: 42px;
  border-radius: 4px;
  box-shadow: none;
}

.el-login-footer {
  right: 0;
  width: 50%;
  color: #a3a3a3;
  letter-spacing: 0;
}

@media (max-width: 900px) {
  .login {
    justify-content: center;
    padding: 24px;
  }

  .login-brand {
    display: none;
  }

  .login-form {
    width: min(420px, 100%);
  }

  .el-login-footer {
    width: 100%;
  }
}
</style>
