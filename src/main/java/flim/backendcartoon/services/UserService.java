package flim.backendcartoon.services;


import flim.backendcartoon.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    void createUser(User user);
    User findUserById(String id);
    User findUserByPhoneNumber(String phoneNumber);
    Page<User> findAllUsers(int page, int size, String keyword);
    void updateUser(User user);

}
