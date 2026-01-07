package com.desk.security.filter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class FaceAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal; // 여기서는 email(userId)

    // 인증 전 생성자
    public FaceAuthenticationToken(Object principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false);
    }

    // 인증 후 생성자
    public FaceAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() { return null; }
    @Override
    public Object getPrincipal() { return this.principal; }
}