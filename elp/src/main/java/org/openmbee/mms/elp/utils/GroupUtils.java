package org.openmbee.mms.elp.utils;

import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.rdb.repositories.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static org.openmbee.mms.core.config.AuthorizationConstants.EVERYONE;
import static org.openmbee.mms.core.config.AuthorizationConstants.MMSADMIN;
import static org.openmbee.mms.elp.Constants.READ_GROUPS;
import static org.openmbee.mms.elp.Constants.WRITE_GROUPS;

@Component
public class GroupUtils {
    private static final Logger logger = LoggerFactory.getLogger(GroupUtils.class);

    private static final Set<String> DEFAULT_SET = Collections.unmodifiableSet(Set.of(EVERYONE));
    private GroupRepository groupRepository;

    @Autowired
    public void setGroupRepository(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public boolean isOnlyEveryone(Set<String> groups) {
        return groups.contains(EVERYONE) && groups.size() == 1;
    }

    public boolean isAdmin(Set<String> userGroups) {
        return userGroups.contains(MMSADMIN);
    }

    private Set<String> parseGroups(Object groups) {
        Set<String> groupSet = new HashSet<>();
        if(groups instanceof Iterable) {
            for (Object group : (Iterable)groups) {
                groupSet.add(String.valueOf(group));
            }
        } else if(groups != null) {
            logger.error("Could not parse groups of type: "+ groups.getClass());
            throw new BadRequestException("Could not parse groups");
        }

        if(groupSet.isEmpty()) {
            return DEFAULT_SET;
        }
        return groupSet;
    }

    public Set<String> getCurrentUserGroups() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            logger.warn("No authentication found while determining user groups");
            return Collections.emptySet();
        } else {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        }
    }

    public Set<String> getExistingGroups() {
        return groupRepository.findAll().stream().map(Group::getName).collect(Collectors.toSet());
    }

    public Set<String> getReadWriteGroups(Map<String, Object> existingElement) {
        return parseGroups(existingElement.get(WRITE_GROUPS));
    }

    public Set<String> getReadOnlyGroups(Map<String, Object> existingElement) {
        return parseGroups(existingElement.get(READ_GROUPS));
    }
}
