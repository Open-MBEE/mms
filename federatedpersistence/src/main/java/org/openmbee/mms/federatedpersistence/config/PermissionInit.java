package org.openmbee.mms.federatedpersistence.config;

import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.Privilege;
import org.openmbee.mms.data.domains.global.Role;
import org.openmbee.mms.rdb.repositories.GroupRepository;
import org.openmbee.mms.rdb.repositories.PrivilegeRepository;
import org.openmbee.mms.rdb.repositories.RoleRepository;
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

    private PrivilegeRepository privRepo;

    private RoleRepository roleRepo;

    private GroupRepository groupRepo;

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

        Optional<Group> evGroupIn = groupRepo.findByName("everyone");
        if (!(evGroupIn.isPresent())) {
            Group evGroup = new Group();
            evGroup.setName("everyone");
            groupRepo.saveAndFlush(evGroup);
        }
    }
}