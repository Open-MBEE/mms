package org.openmbee.mms.authenticator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

import org.openmbee.mms.core.services.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenGenerator implements Serializable, TokenService {

    private static final long serialVersionUID = 6463567580980594813L;
    private static final String CLAIM_KEY_USERNAME = "sub";
    private static final String CLAIM_KEY_USERID = "id";
    private static final String CLAIM_KEY_CREATED = "created";
    private static final String CLAIM_KEY_ENABLED = "enabled";
    private static final String CLAIM_KEY_AUTHORITIES = "authorities";
    static Logger logger = LoggerFactory.getLogger(JwtTokenGenerator.class);
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String getUsernameFromToken(String token) {
        String username = null;
        try {
            final Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                username = claims.getSubject();
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error getting username from token", e);
            }
        }
        return username;
    }

    public Date getCreatedDateFromToken(String token) {
        Date created = null;
        try {
            final Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                created = new Date((Long) claims.get(CLAIM_KEY_CREATED));
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error getting created date from token", e);
            }
        }
        return created;
    }

    public Date getExpirationDateFromToken(String token) {
        Date expires = null;
        try {
            final Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                expires = claims.getExpiration();
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error getting expiration from token", e);
            }
        }
        return expires;
    }

    private Claims getClaimsFromToken(String token) {
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(getSecretKey()).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error getting claims from token", e);
            }
        }
        return claims;
    }

    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + expiration * 1000);
    }

    private boolean isTokenExpired(String token) {
        final Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate != null) {
            return expirationDate.before(new Date());
        }
        return true;
    }

    @Override
    public String generateToken(UserDetails userDetails) {

        List<String> authorities = new ArrayList<>();
        for (GrantedAuthority ga : userDetails.getAuthorities()) {
            authorities.add(ga.getAuthority());
        }
        return generateToken(userDetails.getUsername(), userDetails.isEnabled(), authorities);
    }

    @Override
    public String generateToken(String principal, Collection<String> authorities) {
        return generateToken(principal, true, authorities);
    }

    private String generateToken(String principal, boolean enabled, Collection<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USERNAME, principal);
        claims.put(CLAIM_KEY_USERID, principal);
        claims.put(CLAIM_KEY_ENABLED, enabled);
        claims.put(CLAIM_KEY_CREATED, new Date());
        claims.put(CLAIM_KEY_AUTHORITIES, authorities);
        return generateToken(claims);
    }

    private String generateToken(Map<String, Object> claims) {
        return Jwts.builder().setClaims(claims).setExpiration(generateExpirationDate())
            .signWith(getSecretKey())
            .compact();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String refreshToken(String token) {
        String refreshedToken = null;
        try {
            final Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                claims.put(CLAIM_KEY_CREATED, new Date());
                refreshedToken = generateToken(claims);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error refreshing token", e);
            }
        }
        return refreshedToken;
    }

    public boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    public Collection<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        try {
            final Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                @SuppressWarnings("unchecked")
                ArrayList<String> tokenAuthorities = (ArrayList<String>) claims.get(CLAIM_KEY_AUTHORITIES);
                for (String auth : tokenAuthorities) {
                    authorities.add(new SimpleGrantedAuthority(auth));
                }
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error getting authorities from token", e);
            }
        }
        return authorities;
    }

}
