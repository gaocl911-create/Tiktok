package org.dromara.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.XcxLoginBody;
import org.dromara.common.core.domain.model.XcxLoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.creator.config.WeChatMiniappProperties;
import org.dromara.creator.domain.bo.PtStaffProfileBo;
import org.dromara.creator.service.IPtStaffProfileService;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysSocialService;
import org.dromara.system.service.ISysUserService;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.IAuthStrategy;
import org.dromara.web.service.SysLoginService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 小程序认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private static final String SOCIAL_SOURCE = "WECHAT_MINI_PROGRAM";
    private static final Long PT_STAFF_ROLE_ID = 5L;
    private static final Long DEFAULT_DEPT_ID = 103L;

    private final SysLoginService loginService;
    private final ISysUserService userService;
    private final ISysSocialService socialService;
    private final IPtStaffProfileService staffProfileService;
    private final WeChatMiniappProperties weChatMiniappProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVo login(String body, SysClientVo client) {
        XcxLoginBody loginBody = JsonUtils.parseObject(body, XcxLoginBody.class);
        ValidatorUtils.validate(loginBody);
        // xcxCode 为 小程序调用 wx.login 授权后获取
        String xcxCode = loginBody.getXcxCode();
        // 多个小程序识别使用
        String appid = loginBody.getAppid();
        String actualAppid = StringUtils.blankToDefault(appid, weChatMiniappProperties.getAppid());
        if (StringUtils.isBlank(actualAppid) || StringUtils.isBlank(weChatMiniappProperties.getAppSecret())) {
            throw new ServiceException("请先配置微信小程序 AppID 和 AppSecret");
        }

        // 校验 appid + appSecret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(actualAppid)
            .clientSecret(weChatMiniappProperties.getAppSecret())
            .ignoreCheckRedirectUri(true)
            .ignoreCheckState(true)
            .build());
        AuthCallback authCallback = new AuthCallback();
        authCallback.setCode(xcxCode);
        AuthResponse<AuthUser> resp = authRequest.login(authCallback);
        String openid, unionId;
        AuthToken token;
        if (resp.ok()) {
            token = resp.getData().getToken();
            openid = token.getOpenId();
            // 微信小程序只有关联到微信开放平台下之后才能获取到 unionId，因此unionId不一定能返回。
            unionId = token.getUnionId();
        } else {
            throw new ServiceException("微信登录失败：" + resp.getMsg());
        }
        // 框架登录不限制从什么表查询 只要最终构建出 LoginUser 即可
        SysUserVo user = loadUserByOpenid(openid, unionId, token);
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        XcxLoginUser loginUser = new XcxLoginUser();
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(user.getUserType());
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        loginUser.setOpenid(openid);

        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        // 小程序用户 token 有效期：活跃 30 分钟，最长 7 天
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        loginVo.setOpenid(openid);
        return loginVo;
    }

    /**
     * 通过 openid 查询或自动创建绑定用户。
     * 1. 查 sys_social 表获取已绑定的 userId
     * 2. 未绑定则自动创建 sys_user + pt_staff_profile + sys_social
     */
    private SysUserVo loadUserByOpenid(String openid, String unionId, AuthToken token) {
        // 查询已有的社交绑定
        List<SysSocialVo> socialList = socialService.selectByAuthId(openid);
        SysSocialVo existingSocial = socialList.stream()
            .filter(s -> SOCIAL_SOURCE.equals(s.getSource()))
            .findFirst()
            .orElse(null);

        if (existingSocial != null) {
            // 已有绑定，直接加载用户
            SysUserVo user = userService.selectUserById(existingSocial.getUserId());
            if (ObjectUtil.isNull(user)) {
                throw new ServiceException("微信绑定的系统用户不存在，请联系管理员");
            }
            if (SystemConstants.DISABLE.equals(user.getStatus())) {
                throw new ServiceException("您的账号已被停用，请联系管理员");
            }
            return user;
        }

        // 新用户：自动创建 sys_user
        log.info("微信小程序新用户 openid={}，自动创建账号", openid);
        SysUserBo userBo = new SysUserBo();
        userBo.setNickName("微信用户");
        userBo.setUserName("wx_" + openid.substring(0, Math.min(openid.length(), 16)));
        userBo.setPassword("");  // 小程序用户无密码，使用 Sa-Token 管理登录
        userBo.setDeptId(DEFAULT_DEPT_ID);
        userBo.setStatus("0"); // 正常状态
        userBo.setUserType("xcx_user");
        int inserted = userService.insertUser(userBo);
        if (inserted <= 0) {
            throw new ServiceException("自动创建用户失败，请联系管理员");
        }
        // 获取刚创建的用户
        SysUserVo newUser = userService.selectUserByUserName(userBo.getUserName());
        if (ObjectUtil.isNull(newUser)) {
            throw new ServiceException("自动创建用户失败，请联系管理员");
        }
        // 绑定兼职角色
        userService.insertUserAuth(newUser.getUserId(), new Long[]{PT_STAFF_ROLE_ID});

        // 创建社交绑定（openid → userId）
        SysSocialBo socialBo = new SysSocialBo();
        socialBo.setUserId(newUser.getUserId());
        socialBo.setTenantId(newUser.getTenantId());
        socialBo.setSource(SOCIAL_SOURCE);
        socialBo.setAuthId(openid);
        socialBo.setAccessToken(StringUtils.blankToDefault(token.getAccessToken(), openid));
        socialBo.setOpenId(openid);
        socialBo.setUserName(openid);
        socialBo.setNickName("微信用户");
        if (unionId != null) {
            socialBo.setUnionId(unionId);
        }
        socialService.insertByBo(socialBo);

        // 创建空的兼职资料档案（onboarding_status=incomplete）
        PtStaffProfileBo profileBo = new PtStaffProfileBo();
        profileBo.setUserId(newUser.getUserId());
        profileBo.setTenantId(newUser.getTenantId());
        profileBo.setOnboardingStatus("incomplete");
        staffProfileService.createEmptyProfile(profileBo);

        log.info("微信小程序新用户自动建档完成：userId={}, openid={}", newUser.getUserId(), openid);
        return newUser;
    }

}
