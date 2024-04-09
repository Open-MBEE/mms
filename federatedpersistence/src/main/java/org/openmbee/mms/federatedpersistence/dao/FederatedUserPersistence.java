package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.data.domains.global.User;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.rdb.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FederatedUserPersistence implements UserPersistence {

    private UserRepository userRepository;
    private FederatedJsonUtils jsonUtils;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setJsonUtils(FederatedJsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    @Override
    public UserJson save(UserJson userJson) {

        Optional<User> existing = userRepository.findByUsernameIgnoreCase(userJson.getUsername());

        User user;
        if(existing.isPresent()) {
            user = existing.get();
        } else {
            user = new User();
            user.setUsername(userJson.getUsername());
        }

        user.setEmail(userJson.getEmail());
        user.setFirstName(userJson.getFirstName());
        user.setLastName(userJson.getLastName());
        user.setPassword(userJson.getPassword());
        user.setEnabled(userJson.isEnabled());
        user.setAdmin(userJson.isAdmin());

        User saved = userRepository.save(user);

        return getJson(saved);
    }

    @Override
    public Optional<UserJson> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsernameIgnoreCase(username);
        return user.map(this::getJson);
    }

    @Override
    public Collection<UserJson> findAll() {
        Collection<User> users = userRepository.findAll();
        return users.stream().map(this::getJson).collect(Collectors.toList());
    }

    private UserJson getJson(User saved) {
        UserJson savedJson = new UserJson();
        savedJson.merge(jsonUtils.convertToMap(saved));
        return savedJson;
    }
}
