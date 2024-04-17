package org.openmbee.mms.users.security;

import java.util.ArrayList;
import java.util.Collection;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class DefaultUsersDetails implements UsersDetails {

    private final UserJson user;
    private final Collection<GroupJson> groups;

    public DefaultUsersDetails(UserJson user, Collection<GroupJson> groups) {
        this.user = user;
        this.groups = groups;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        if (groups != null) {
            for (GroupJson group : groups) {
                authorities.add(new SimpleGrantedAuthority(group.getName()));
            }
        }
        if (Boolean.TRUE.equals(user.isAdmin())) {
            authorities.add(new SimpleGrantedAuthority(AuthorizationConstants.MMSADMIN));
        }
        authorities.add(new SimpleGrantedAuthority(AuthorizationConstants.EVERYONE));
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    public UserJson getUser() {
        return user;
    }

}
