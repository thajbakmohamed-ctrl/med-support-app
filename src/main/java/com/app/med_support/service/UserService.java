package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    // USER SERVICES WILL NEED REPOSITORY TO DEALS WITH USERS DATA
    private final UserRepository userRepository;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public User createUser(User user) {
        if(userRepository.existsByEmail(user.getEmail())) {
            return null;
        }
        if(!user.getRole().equals("DONOR") &&!user.getRole().equals("HOSPITAL_STAFF")
                &&!user.getRole().equals("ADMIN")) {
            return null;
        }
        if(!user.getStatus().equals("ACTIVE") && !user.getStatus().equals("INACTIVE")) {
            return null;
        }
        return userRepository.save(user);
    }


    // THE FLOW IS ----
    // WE RECEIVED THE USER ID -- WE SEARCH THE ID -- IF NOT  EXISTS (NULL)
    // IF EXISTS WE UPDATE THE DATA THAT IS ABLE TO CHANGE -- NEXT SAVE
    public User updateUser(Long userId, User updatedUser) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if(existingUser == null) {
            return null;
        }
        existingUser.setName(updatedUser.getName());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setProfileImage(updatedUser.getProfileImage());
        return userRepository.save(existingUser);
    }


    public boolean deleteUser(Long userId) {
        if(!userRepository.existsById(userId)) {
            return false;
        }
        userRepository.deleteById(userId);
        return true;
    }


    public User updateUserStatus(Long userId, String status) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if(existingUser == null) {
            return null;
        }
        if(!status.equals("ACTIVE")&&!status.equals("INACTIVE")) {
            return null;
        }
        existingUser.setStatus(status);
        return userRepository.save(existingUser);
    }


    public User verifyUserEmail(Long userId) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if(existingUser == null) {
            return null;
        }
        existingUser.setEmailVerified(true);
        return userRepository.save(existingUser);
    }


    public User updateUserRole(Long userId, String role) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if(existingUser == null) {
            return null;
        }
        if(!role.equals("DONOR") && !role.equals("HOSPITAL_STAFF") && !role.equals("ADMIN")) {
            return null;
        }
        existingUser.setRole(role);
        return userRepository.save(existingUser);
    }

    public User updateUserPassword(Long userId, String hashedPassword) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        existingUser.setHashedPassword(hashedPassword);
        return userRepository.save(existingUser);
    }


    public User assignHospitalToUser(Long userId, Hospital hospital) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if(existingUser == null) {
            return null;
        }
        if(!existingUser.getRole().equals("HOSPITAL_STAFF")) {
            return null;
        }
        existingUser.setHospital(hospital);
        return userRepository.save(existingUser);
    }
}

