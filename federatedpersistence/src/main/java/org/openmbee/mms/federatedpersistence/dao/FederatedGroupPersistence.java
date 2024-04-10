package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.rdb.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FederatedGroupPersistence implements GroupPersistence {

    private GroupRepository groupRepository;
    private FederatedJsonUtils jsonUtils;

    @Autowired
    public void setGroupRepository(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Autowired
    public void setJsonUtils(FederatedJsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    @Override
    public GroupJson save(GroupJson groupJson) {
        Group groupObj = new Group();
        groupObj.setName(groupJson.getName());
        Group saved = groupRepository.saveAndFlush(groupObj);
        return getJson(saved);
    }

    @Override
    public void delete(GroupJson groupJson) {
        Optional<Group> group = groupRepository.findByName(groupJson.getName());
        group.ifPresent(g -> groupRepository.delete(g));
    }

    @Override
    public Optional<GroupJson> findByName(String name) {
        Optional<Group> group = groupRepository.findByName(name);
        return group.map(this::getJson);
    }

    @Override
    public Collection<GroupJson> findAll() {
        List<Group> groups = groupRepository.findAll(Sort.by(Group.NAME_COLUMN));
        return groups.stream().map(this::getJson).collect(Collectors.toList());
    }

    private GroupJson getJson(Group saved) {
        GroupJson json = new GroupJson();
        json.merge(jsonUtils.convertToMap(saved));
        return json;
    }

    @Override
    public boolean hasPublicPermissions(String groupName) {
        Optional<Group> group = groupRepository.findByName(groupName);
        if (group.isEmpty()) {
            throw new NotFoundException("group " + groupName + " not found");
        }
        return group.get().isPublic();
    }
}
