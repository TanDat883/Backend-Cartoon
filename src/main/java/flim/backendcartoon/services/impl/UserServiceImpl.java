package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserReponsitory userRepository;


    @Autowired
    public UserServiceImpl(UserReponsitory userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void createUser(User user) {
        userRepository.save(user);
    }

    @Override
    public User findUserById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public User findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAllUsers();
    }

    @Override
    public void updateUser(User user) {
        userRepository.save(user);
    }
}
