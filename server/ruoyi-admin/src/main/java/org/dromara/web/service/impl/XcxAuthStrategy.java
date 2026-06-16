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
import org.dromara.common.mybatis.helper.DataPermissionHelper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private static final String SOCIAL_SOURCE = "WECHAT_MINI_PROGRAM";
    private static final Long PT_STAFF_ROLE_ID = 5L;
    private static final Long DEFAULT_DEPT_ID = 103L;

    private final ISysUserService userService;
    private final ISysSocialService socialService;
    private final IPtStaffProfileService staffProfileService;
    private final WeChatMiniappProperties weChatMiniappProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVo login(String body, SysClientVo client) {
        XcxLoginBody loginBody = JsonUtils.parseObject(body, XcxLoginBody.class);
        ValidatorUtils.validate(loginBody);

        String xcxCode = loginBody.getXcxCode();
        String appid = loginBody.getAppid();
        String actualAppid = StringUtils.blankToDefault(appid, weChatMiniappProperties.getAppid());
        if (StringUtils.isBlank(actualAppid) || StringUtils.isBlank(weChatMiniappProperties.getAppSecret())) {
            throw new ServiceException("请先配置微信小程序 AppID 和 AppSecret");
        }

        WechatIdentity identity = resolveWechatIdentity(actualAppid, xcxCode);
        SysUserVo user = loadUserByOpenid(identity.openid(), identity.unionId(), identity.token());

        XcxLoginUser loginUser = new XcxLoginUser();
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(user.getUserType());
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        loginUser.setOpenid(identity.openid());

        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        loginVo.setOpenid(identity.openid());
        return loginVo;
    }

    private WechatIdentity resolveWechatIdentity(String actualAppid, String xcxCode) {
        if (Boolean.TRUE.equals(weChatMiniappProperties.getMockEnabled()) && xcxCode.startsWith("mock:")) {
            String openid = StringUtils.blankToDefault(xcxCode.substring("mock:".length()), "dev_openid");
            AuthToken token = new AuthToken();
            token.setOpenId(openid);
            token.setAccessToken(openid);
            return new WechatIdentity(openid, null, token);
        }

        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(actualAppid)
            .clientSecret(weChatMiniappProperties.getAppSecret())
            .ignoreCheckRedirectUri(true)
            .ignoreCheckState(true)
            .build());
        AuthCallback authCallback = new AuthCallback();
        authCallback.setCode(xcxCode);
        AuthResponse<AuthUser> resp = authRequest.login(authCallback);
        if (!resp.ok()) {
            throw new ServiceException("微信登录失败：" + resp.getMsg());
        }
        AuthToken token = resp.getData().getToken();
        return new WechatIdentity(token.getOpenId(), token.getUnionId(), token);
    }

    private SysUserVo loadUserByOpenid(String openid, String unionId, AuthToken token) {
        List<SysSocialVo> socialList = socialService.selectByAuthId(openid);
        SysSocialVo existingSocial = socialList.stream()
            .filter(s -> SOCIAL_SOURCE.equals(s.getSource()))
            .findFirst()
            .orElse(null);

        if (existingSocial != null) {
            SysUserVo user = userService.selectUserById(existingSocial.getUserId());
            if (ObjectUtil.isNull(user)) {
                throw new ServiceException("微信绑定的系统用户不存在，请联系管理员");
            }
            if (SystemConstants.DISABLE.equals(user.getStatus())) {
                throw new ServiceException("您的账号已被停用，请联系管理员");
            }
            return user;
        }

        log.info("New miniapp user openid={}, creating account.", openid);
        SysUserBo userBo = new SysUserBo();
        userBo.setNickName("微信用户");
        userBo.setUserName("wx_" + openid.substring(0, Math.min(openid.length(), 16)));
        userBo.setPassword("");
        userBo.setDeptId(DEFAULT_DEPT_ID);
        userBo.setStatus(SystemConstants.NORMAL);
        userBo.setUserType("xcx_user");
        int inserted = userService.insertUser(userBo);
        if (inserted <= 0) {
            throw new ServiceException("自动创建用户失败，请联系管理员");
        }

        SysUserVo newUser = userService.selectUserByUserName(userBo.getUserName());
        if (ObjectUtil.isNull(newUser)) {
            throw new ServiceException("自动创建用户失败，请联系管理员");
        }
        DataPermissionHelper.ignore(() ->
            userService.insertUserAuth(newUser.getUserId(), new Long[]{PT_STAFF_ROLE_ID})
        );

        SysSocialBo socialBo = new SysSocialBo();
        socialBo.setUserId(newUser.getUserId());
        socialBo.setTenantId(newUser.getTenantId());
        socialBo.setSource(SOCIAL_SOURCE);
        socialBo.setAuthId(openid);
        socialBo.setAccessToken(StringUtils.blankToDefault(token.getAccessToken(), openid));
        socialBo.setOpenId(openid);
        socialBo.setUserName(openid);
        socialBo.setNickName("微信用户");
        if (StringUtils.isNotBlank(unionId)) {
            socialBo.setUnionId(unionId);
        }
        socialService.insertByBo(socialBo);

        PtStaffProfileBo profileBo = new PtStaffProfileBo();
        profileBo.setUserId(newUser.getUserId());
        profileBo.setTenantId(newUser.getTenantId());
        profileBo.setOnboardingStatus("incomplete");
        staffProfileService.createEmptyProfile(profileBo);

        log.info("Miniapp user account created. userId={}, openid={}", newUser.getUserId(), openid);
        return newUser;
    }

    private record WechatIdentity(String openid, String unionId, AuthToken token) {
    }
}
