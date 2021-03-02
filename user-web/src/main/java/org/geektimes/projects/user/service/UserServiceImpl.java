package org.geektimes.projects.user.service;

import org.apache.commons.lang.StringUtils;
import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.repository.DatabaseUserRepository;
import org.geektimes.projects.user.repository.UserRepository;
import org.geektimes.projects.user.sql.DBConnectionManager;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @create 2021/3/2 19:42
 */
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    public UserServiceImpl() {
        DBConnectionManager dbConnectionManager = new DBConnectionManager();
        try {
            dbConnectionManager.setConnection(DriverManager.getConnection("jdbc:derby:db/user-platform;create=true"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.userRepository = new DatabaseUserRepository(dbConnectionManager);
    }

    @Override
    public boolean register(User user) {
        if(StringUtils.isEmpty(user.getPhoneNumber())) return false;
        if(userRepository.getByPhoneNumber(user.getPhoneNumber()) != null) {
            return false;
        }
        return userRepository.save(user);
    }

    public boolean save(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean deregister(User user) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public User queryUserById(Long id) {
        return null;
    }

    @Override
    public User queryUserByNameAndPassword(String name, String password) {
        return null;
    }


}
