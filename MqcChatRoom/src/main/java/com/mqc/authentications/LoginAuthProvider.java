package com.mqc.authentications;


import com.mqc.entity.UserInfo;
import com.mqc.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;

@Component
@Slf4j

public class LoginAuthProvider implements AuthenticationProvider {


    @Autowired
    private HttpSession httpSession;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    UserInfoRepository usersRepository;

    @Override
    public Authentication authenticate(Authentication auth)
            throws AuthenticationException {
        String account = auth.getName().toLowerCase();
        String password = auth.getCredentials().toString();

//        String adminAccount = "admin";
//        String adminPassword = "1wax@QSZz";
//        httpSession.setAttribute("isAdmin", false);
//        if (adminAccount.equals(account) && adminPassword.equals(password)) {
//            httpSession.setAttribute("name", account);
//            httpSession.setAttribute("extension", null);
//
//            return new UsernamePasswordAuthenticationToken(account, password, null);
//        }
//

        // boolean isSuccess = AdCheck(account, password);
        boolean isSuccess = true;
        UserInfo userInfo = usersRepository.findByAccount(account);
        if (userInfo != null && passwordEncoder.matches(password, userInfo.getPassword())) {

            httpSession.setAttribute("name", String.format("%s", userInfo.getUserName()));
            httpSession.setAttribute("extension", userInfo.getExtension());
            httpSession.setAttribute("userInfo",userInfo);
            if(userInfo.isAdmin()){
                httpSession.setAttribute("isAdmin", true);
            }else{
                httpSession.setAttribute("isAdmin", false);
            }
        } else {
            log.warn("Account {} Not Found", account);
            return null;
        }
        return new UsernamePasswordAuthenticationToken(account, password, getAuthorities(userInfo));
    }


//    private boolean AdCheck(String account, String password) {
//
//        try {
//
//            Properties env = new Properties();
//            // 使用UPN格式：User@domain或SamAccountName格式：domain\\User
//            String adminName = account + "@" + adConfig.getDomain();
//            String adminPassword = password;
//
//            URI uri = URI.create(adConfig.getUrl());
//            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//            env.put(Context.SECURITY_AUTHENTICATION, "simple");// LDAP訪問安全級別："none","simple","strong"
//            env.put(Context.SECURITY_PRINCIPAL, adminName);// AD User
//            env.put(Context.SECURITY_CREDENTIALS, adminPassword);// AD Password
//            env.put(Context.PROVIDER_URL, adConfig.getUrl());// LDAP工廠類
//
//            if (uri.getScheme().equalsIgnoreCase("ldaps")) {
//                env.put(Context.SECURITY_PROTOCOL, "ssl");
//                env.put("java.naming.ldap.factory.socket", CustomSSLSocketFactory.class.getName());
//            }
//
//            InitialDirContext ctx = new InitialDirContext(env);
//
//
//            // 搜索控制器
////            SearchControls searchCtls = new SearchControls();
////            // 創建搜索控制器
////            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
////            // LDAP搜索過濾器類，此處只獲取AD域用户，所以條件為用户user或者person均可
////            // (&(objectCategory=person)(objectClass=user)(name=*))
////            String searchFilter = String.format("sAMAccountName=%s", account);
////            // AD域節點結構
////            String searchBase = adbase;
////            String returnedAtts[] = {"sn", "cn", "mail", "name", "userPrincipalName",
////                    "department", "sAMAccountName", "whenChanged"};
////            searchCtls.setReturningAttributes(returnedAtts);
////            NamingEnumeration<SearchResult> answer = ctx.search(searchBase, searchFilter, searchCtls);
//
//
//            ctx.close();
//            return true;
//        } catch (Exception e) {
//            logger.error("AD Login Error", e);
//            return false;
//        }
//
//
//    }

    private Collection<GrantedAuthority> getAuthorities(UserInfo users) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        // String title = Objects.equals(cucmUserDetails.getTitle(), "") ? "test" : cucmUserDetails.getTitle();
        String title = "test";
        authorities.add(new SimpleGrantedAuthority(title));
        return authorities;
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }


    @PostConstruct
    public void disableEndpointIdentification() {
        System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification", "true");
    }
}
