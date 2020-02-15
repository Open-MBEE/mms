package org.openmbee.sdvc.authenticator.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmbee.sdvc.authenticator.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    private final String AUTHORIZATION = "Authorization";
    private final String BEARER = "Bearer ";

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        String authHeader = request.getHeader(AUTHORIZATION);

        // Require the Authorization: Bearer format for auth header
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            chain.doFilter(request, response);
        } else {
            String authToken = request.getHeader(AUTHORIZATION).substring(BEARER.length());

            if (!authToken.isEmpty()) {
                String username = jwtTokenGenerator.getUsernameFromToken(authToken);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    //possible to get authorities from jwt instead of looking up in db also, if authorities were included in jwt
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    if (jwtTokenGenerator.validateToken(authToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null,
                                userDetails.getAuthorities());
                        authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            chain.doFilter(request, response);
        }
    }
}
