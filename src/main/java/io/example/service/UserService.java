package io.example.service;

import io.example.domain.dto.CreateUserRequest;
import io.example.domain.dto.SearchUsersRequest;
import io.example.domain.dto.UpdateUserRequest;
import io.example.domain.dto.UserView;
import io.example.domain.mapper.UserEditMapper;
import io.example.domain.mapper.UserViewMapper;
import io.example.domain.model.User;
import io.example.repository.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;

import java.util.List;

import static java.lang.String.format;

@Service
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;
    private final UserEditMapper userEditMapper;
    private final UserViewMapper userViewMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo,
                       UserEditMapper userEditMapper,
                       UserViewMapper userViewMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.userEditMapper = userEditMapper;
        this.userViewMapper = userViewMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserView create(CreateUserRequest request) {
        if (!request.getPassword().equals(request.getRePassword())) {
            throw new ValidationException("Passwords don't match!");
        }

        User user = userEditMapper.create(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user = userRepo.save(user);

        return userViewMapper.toUserView(user);
    }

    @Transactional
    public UserView update(ObjectId id, UpdateUserRequest request) {
        User user = userRepo.getById(id);
        userEditMapper.update(request, user);

        user = userRepo.save(user);

        return userViewMapper.toUserView(user);
    }

    @Transactional
    public UserView delete(ObjectId id) {
        User user = userRepo.getById(id);

        user.setEnabled(false);
        user = userRepo.save(user);

        return userViewMapper.toUserView(user);
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo
                .findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException(format("User with username - %s, not found", username))
                );

        return user;
    }

    public boolean usernameExists(String username) {
        return userRepo.findByUsername(username).isPresent();
    }

    public UserView getUser(ObjectId id) {
        return userViewMapper.toUserView(userRepo.getById(id));
    }

    public List<UserView> searchUsers(SearchUsersRequest request) {
        List<User> users = userRepo.searchUsers(request);
        return userViewMapper.toUserView(users);
    }

}
