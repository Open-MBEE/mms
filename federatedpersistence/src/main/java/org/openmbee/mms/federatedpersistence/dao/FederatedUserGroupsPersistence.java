package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.User;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.rdb.repositories.GroupRepository;
import org.openmbee.mms.rdb.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FederatedUserGroupsPersistence implements UserGroupsPersistence {
    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private FederatedJsonUtils jsonUtils;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setGroupRepository(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Autowired
    public void setJsonUtils(FederatedJsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    @Override
    @Transactional
    public boolean addUserToGroup(String groupName, String username) {
        Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(username);
        if(userOptional.isEmpty()) {
            return false;
        }
        Optional<Group> groupOptional = groupRepository.findByName(groupName);
        if(groupOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        if(user.getGroups().contains(groupOptional.get())) {
            return false;
        }
        user.getGroups().add(groupOptional.get());
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean removeUserFromGroup(String groupName, String username) {
        Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(username);
        if(userOptional.isEmpty()) {
            return false;
        }
        Optional<Group> groupOptional = groupRepository.findByName(groupName);
        if(groupOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        if(!user.getGroups().contains(groupOptional.get())) {
            return false;
        }
        user.getGroups().remove(groupOptional.get());
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public Collection<UserJson> findUsersInGroup(String groupName) {
        Optional<Group> groupOptional = groupRepository.findByName(groupName);
        if(groupOptional.isEmpty()){
            return List.of();
        }
        return groupOptional.get().getUsers().stream().map(this::getJson).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Collection<GroupJson> findGroupsAssignedToUser(String username) {
        Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(username);
        if(userOptional.isEmpty()) {
            return List.of();
        }
        return userOptional.get().getGroups().stream().map(this::getJson).collect(Collectors.toList());
    }

    private GroupJson getJson(Group saved) {
        GroupJson json = new GroupJson();
        json.merge(jsonUtils.convertToMap(saved));
        return json;
    }

    private UserJson getJson(User saved) {
        UserJson savedJson = new UserJson();
        savedJson.merge(jsonUtils.convertToMap(saved));
        return savedJson;
    }
}
