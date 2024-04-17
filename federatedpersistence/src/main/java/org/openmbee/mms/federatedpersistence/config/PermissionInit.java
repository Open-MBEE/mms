package org.openmbee.mms.federatedpersistence.config;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.services.PermissionService;
import org.openmbee.mms.data.domains.global.*;
import org.openmbee.mms.rdb.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import javax.transaction.Transactional;
import java.util.*;

import static org.openmbee.mms.core.config.Constants.RPmap;
import static org.openmbee.mms.core.config.Constants.aPriv;

@Component
@Transactional
public class PermissionInit implements ApplicationListener<ApplicationReadyEvent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private PrivilegeRepository privRepo;

    private RoleRepository roleRepo;

    private GroupRepository groupRepo;

    private UserRepository userRepo;

    private GroupGroupPermRepository groupGroupPermRepo;

    @Autowired
    public void setPrivRepo(PrivilegeRepository privRepo) {
        this.privRepo = privRepo;
    }

    @Autowired
    public void setRoleRepo(RoleRepository roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Autowired
    public void setGroupRepo(GroupRepository groupRepo) {
        this.groupRepo = groupRepo;
    }

    @Autowired
    public void setUserRepo(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Autowired
    public void setGroupGroupPermRepo(GroupGroupPermRepository groupGroupPermRepo) {
        this.groupGroupPermRepo = groupGroupPermRepo;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        for (String role : RPmap.keySet()) {
            Optional<Role> roleIn = roleRepo.findByName(role);
            if (!(roleIn.isPresent())) {
                Role missingRole = new Role();
                missingRole.setName(role);
                roleRepo.saveAndFlush(missingRole);
            }
        }

        for (String privs : aPriv) {
            Optional<Privilege> privIn = privRepo.findByName(privs);
            if (!(privIn.isPresent())) {
                Privilege missingPriv = new Privilege();
                missingPriv.setName(privs);
                privRepo.saveAndFlush(missingPriv);
            }
        }

        List<Role> roleList = roleRepo.findAll();
        List<Privilege> privList = privRepo.findAll();

        for (Role role : roleList) {
            Set<Privilege> pSet = new HashSet<Privilege>();
            for (Privilege priv : privList) {
                if ((RPmap.get(role.getName())).contains(priv.getName())) {
                    pSet.add(priv);
                }
            }
            role.setPrivileges(pSet);
            roleRepo.saveAndFlush(role);
        }
        //Ensure ev group exists and is correct in the event someone messes it up
        Optional<Group> evGroupIn = groupRepo.findByName(AuthorizationConstants.EVERYONE);
        Group evGroup;
        if (evGroupIn.isEmpty()) {
            evGroup = new Group();
            evGroup.setName(AuthorizationConstants.EVERYONE);
        }else {
            evGroup = evGroupIn.get();
        }
        if (evGroup.getType() != Group.VALID_GROUP_TYPES.REMOTE) {
            evGroup.setType("everyone");
        }
        evGroup.setPublic(true);
        groupRepo.saveAndFlush(evGroup);
        Optional<GroupGroupPerm> evGroupPermOp = groupGroupPermRepo.findByGroupAndGroupPerm(evGroup,evGroup);
        Optional<Role> evRole = roleRepo.findByName(AuthorizationConstants.READER);
        if (evGroupPermOp.isEmpty() && evRole.isPresent()) {
                evGroupPermOp = Optional.of(new GroupGroupPerm(evGroup, evGroup, evRole.get()));
                groupGroupPermRepo.saveAndFlush(evGroupPermOp.get());
        }
        //Ensure permissions are correct on ev group in the event someone messes it up
        if (evGroupPermOp.isPresent() && evRole.isPresent() && evGroupPermOp.get().getRole() != evRole.get()) {
            GroupGroupPerm evGroupPerm = evGroupPermOp.get();
            evGroupPerm.setRole(evRole.get());
            groupGroupPermRepo.saveAndFlush(evGroupPerm);
        }

    }

}