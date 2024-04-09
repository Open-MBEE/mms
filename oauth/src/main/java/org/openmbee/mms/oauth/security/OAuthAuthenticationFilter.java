package org.openmbee.mms.oauth.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmbee.mms.oauth.constants.OAuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class OAuthAuthenticationFilter extends OncePerRequestFilter {
    private static Logger logger = LoggerFactory.getLogger(OAuthAuthenticationFilter.class);
    OAuthProcessor oAuthProcessor;

    @Autowired
    public void setOAuthProcessor(OAuthProcessor oAuthProcessor ){
        this.oAuthProcessor  = oAuthProcessor; 
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader(OAuthConstants.AUTHORIZATION);
            if((authHeader == null || authHeader.isEmpty()) && request.getHeader(OAuthConstants.ACCESS_TOKEN) != null){
                authHeader = OAuthConstants.BEARER+ request.getHeader(OAuthConstants.ACCESS_TOKEN);
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Require the Authorization: Bearer format for auth header
            // Skip OAuth is already authenticated 
            if (authHeader != null && authHeader.startsWith(OAuthConstants.BEARER) && auth == null) {
                String authToken = authHeader;
                OAuth2Authentication authentication = oAuthProcessor.validateAuthToken(authToken);
                if (authentication != null) {
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}
