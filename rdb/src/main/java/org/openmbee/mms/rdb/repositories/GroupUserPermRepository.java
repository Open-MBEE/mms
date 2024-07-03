package org.openmbee.mms.rdb.repositories;

import org.openmbee.mms.data.domains.global.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GroupUserPermRepository extends JpaRepository<GroupUserPerm, Long> {

    List<GroupUserPerm> findAllByGroup(Group group);

    List<GroupUserPerm> findAllByGroup_Name(String groupName);

    Optional<GroupUserPerm> findByGroupAndUser(Group b, User u);

    List<GroupUserPerm> findAllByGroupAndRole_Name(Group group, String r);

    List<GroupUserPerm> findAllByUser_Username(String username);

    boolean existsByGroupAndUser_UsernameAndRoleIn(Group group, String user, Set<Role> roles);

    void deleteByGroupAndUser_UsernameIn(Group group, Set<String> users);

    void deleteByGroup(Group group);
}
