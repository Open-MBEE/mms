package org.openmbee.mms.oauth.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class OAuth2Authentication extends AbstractAuthenticationToken {


    private static final long serialVersionUID = 2928542619979639608L;
    private String userId;
    private String autherizationToken;
    private UserDetails principal;
    Map<String, String> grants;

    public OAuth2Authentication() {
        super(new ArrayList<>());
        this.setAuthenticated(false);
    }

    public OAuth2Authentication(Map<String, String> grants) {
        super(new ArrayList<>());
        this.grants = grants;
        this.setAuthenticated(false);
    }

    public OAuth2Authentication(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.setAuthenticated(false);
    }

    public OAuth2Authentication(String authorizationToken, Map<String, String> grants, UserDetails userDetails) {
        super(userDetails.getAuthorities());
        this.autherizationToken = authorizationToken;
        this.grants = grants;
        this.principal = userDetails;
        this.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

}