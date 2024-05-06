package org.openmbee.mms.rdb.repositories;

import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.GroupGroupPerm;
import org.openmbee.mms.data.domains.global.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GroupGroupPermRepository extends JpaRepository<GroupGroupPerm, Long> {

    List<GroupGroupPerm> findAllByGroup(Group group);

    List<GroupGroupPerm> findAllByGroup_Name(String group);

    Optional<GroupGroupPerm> findByGroupAndGroupPerm(Group group, Group groupPerm);

    List<GroupGroupPerm> findAllByGroupAndRole_Name(Group group, String r);

    boolean existsByGroupAndGroupPerm_NameInAndRoleIn(Group group, Set<String> user, Set<Role> roles);

    void deleteByGroupAndGroupPerm_NameIn(Group group, Set<String> groups);

    void deleteByGroup(Group group);

}
