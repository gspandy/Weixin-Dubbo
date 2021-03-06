package com.cheng.weixin.web.manage.security;

import com.cheng.common.entity.enums.Status;
import com.cheng.weixin.common.security.Encodes;
import com.cheng.weixin.common.utils.ServletUtils;
import com.cheng.weixin.common.utils.StringUtils;
import com.cheng.weixin.rpc.admin.entity.Admin;
import com.cheng.weixin.rpc.admin.entity.Permission;
import com.cheng.weixin.rpc.admin.entity.Role;
import com.cheng.weixin.rpc.admin.service.RpcAdminService;
import com.cheng.weixin.rpc.log.service.RpcLogService;
import com.cheng.weixin.web.manage.utils.Captcha;
import com.cheng.weixin.web.manage.utils.UserUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;

/**
 * Desc: 登录认证与授权
 * Author: Cheng
 * Date: 2016/1/26 0026
 */
public class SystemAuthorizingRealm extends AuthorizingRealm {
    @Autowired
    private RpcAdminService adminService;
    @Autowired
    private RpcLogService logService;

    // 返回一个唯一的Realm名字
    @Override
    public String getName() {
        return super.getName();
    }
    // 判断此Realm是否支持此Token
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof WxUsernamePasswordToken ;
    }
    // 认证
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        WxUsernamePasswordToken token = (WxUsernamePasswordToken) authenticationToken;
        // 判断验证码是否正确
        if (Captcha.isValidateCodeLogin(token.getUsername(), false, false)) {
            String captcha = (String) UserUtils.getSession().getAttribute(Captcha.CAPTCHA);
            if (null == token.getCaptcha() || !token.getCaptcha().equalsIgnoreCase(captcha)) {
                throw new AuthenticationException("msg:验证码错误，请重试.");
            }
        }
        // 校验用户名
        Admin admin = UserUtils.getAdminByUsername(token.getUsername());
        if(admin != null) {
            if(admin.getStatus().equals(Status.LOCKED)) {
                throw new LockedAccountException("msg:该帐号已禁止登录.");
            }
            byte[] salt = Encodes.decodeHex(admin.getPassword().substring(0, 16));
            return new SimpleAuthenticationInfo(new Principal(admin, token.isMobilelogin()),
                    admin.getPassword().substring(16), ByteSource.Util.bytes(salt), getName());
        }
        return null;
    }
    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        System.out.println("=======AuthorizationInfo=======");
        Admin admin = UserUtils.getAdminByUsername((String) principalCollection.getPrimaryPrincipal());
        if (admin != null) {
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            // 添加基于Permissionde权限
            List<Permission> principals = UserUtils.getPermissions();
            for (Permission permission : principals) {
                if (StringUtils.isNotBlank(permission.getPrecode())) {
                    info.addStringPermission(permission.getPrecode());
                }
            }
            // 添加用户角色信息
            for (Role role : admin.getRoles()) {
                info.addRole(role.getEnname());
            }
            // 保存日志
            HttpServletRequest request = ServletUtils.getRequest();
            logService.saveLog(StringUtils.getRemoteAddr(request),request.getHeader("user-agent"),request.getRequestURI(),
                    request.getParameterMap(),request.getMethod(), null, "系统登录", UserUtils.getPrincipal().getUsername());
            return info;
        }

        return null;
    }

    /**
     * 授权信息
     */
    public static class Principal implements Serializable {
        private static final long serialVersionUID = 2866069566032650619L;
        /** 编号 **/
        private String id;
        /** 登录名 **/
        private String username;
        /** 是否是手机登录 **/
        private boolean mobileLogin;

        public Principal(Admin admin, boolean mobileLogin) {
            this.id = admin.getId();
            this.username = admin.getUsername();
            this.mobileLogin = mobileLogin;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isMobileLogin() {
            return mobileLogin;
        }

        public void setMobileLogin(boolean mobileLogin) {
            this.mobileLogin = mobileLogin;
        }
    }

    /**
     * 设定密码校验的Hash算法与迭代次数
     * ！这里已在xml配置了 id=hashMatcher
     */
    /*@PostConstruct
    public void initCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        // 设置加密方式
        matcher.setHashAlgorithmName(SystemUtils.HASH_ALGORITHM);
        // 设置迭代次数
        matcher.setHashIterations(SystemUtils.HASH_INTERATIONS);
        // 注入到Shrio里自定义的加密方式
        setCredentialsMatcher(matcher);
    }*/

}
