package flim.backendcartoon.services;


import flim.backendcartoon.entities.User;

import java.util.List;

public interface UserService {
    void createUser(User user);
    User findUserById(String id);
    User findUserByPhoneNumber(String phoneNumber);
    List<User> findAllUsers();
    void updateUser(User user);

}
