package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.XcxLoginBody;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.service.ISysClientService;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.IAuthStrategy;
import org.dromara.web.service.SysLoginService;
import org.springframework.web.bind.annotation.*;

@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/auth")
public class MiniappAuthController {

    private static final String MINIAPP_CLIENT_ID = "ba9e8a5f68fd1436043780186727e92f";
    private static final String GRANT_TYPE_XCX = "xcx";

    private final ISysClientService clientService;
    private final SysLoginService loginService;

    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody MiniappLoginRequest request) {
        SysClientVo client = clientService.queryByClientId(MINIAPP_CLIENT_ID);
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), GRANT_TYPE_XCX)) {
            throw new ServiceException("小程序客户端未配置或不支持 xcx 登录");
        }
        if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException("小程序客户端已停用");
        }

        String tenantId = StringUtils.blankToDefault(request.getTenantId(), TenantConstants.DEFAULT_TENANT_ID);
        loginService.checkTenant(tenantId);

        XcxLoginBody body = new XcxLoginBody();
        body.setClientId(MINIAPP_CLIENT_ID);
        body.setGrantType(GRANT_TYPE_XCX);
        body.setTenantId(tenantId);
        body.setAppid(request.getAppid());
        body.setXcxCode(resolveCode(request));

        LoginVo loginVo = TenantHelper.dynamic(
            tenantId,
            () -> IAuthStrategy.login(JsonUtils.toJsonString(body), client, GRANT_TYPE_XCX)
        );
        return R.ok(loginVo);
    }

    private String resolveCode(MiniappLoginRequest request) {
        if (StringUtils.isNotBlank(request.getCode())) {
            return request.getCode();
        }
        if (StringUtils.isNotBlank(request.getMockOpenid())) {
            return "mock:" + request.getMockOpenid();
        }
        throw new ServiceException("缺少微信登录 code");
    }

    @Data
    public static class MiniappLoginRequest {
        private String code;
        private String appid;
        private String tenantId;
        private String mockOpenid;
    }
}
