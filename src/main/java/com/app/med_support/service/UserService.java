package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import com.app.med_support.request.LoginRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.app.med_support.request.RegisterRequest;

import java.util.List;

@Service
public class UserService {
    // USER SERVICES WILL NEED REPOSITORY TO DEALS WITH USERS DATA
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    //  give me the registertion data and the service will do the account
    public User registerUser(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            return null;
        }
        // this if statment -- the reg is only for donor and hospital staff
        if (!registerRequest.getRole().equals("DONOR")
                && !registerRequest.getRole().equals("HOSPITAL_STAFF")) {
            return null;
        }
        //RegisterRequest.name        -->  User.name
        //RegisterRequest.email       -->  User.email
        //RegisterRequest.phoneNumber --> User.phoneNumber
        //RegisterRequest.role        -->  User.role


        //for example
        // REGISTER REQUEST                 USER ENTITY

        // name = "Thajba"      ->  name = "Thajba"

        // email = "t@x.com"    ->  email = "t@x.com"

        //phone = "3333"       ->  phone = "3333"

        //role = "DONOR"       ->  role = "DONOR"

        //password = "1234"    ->  we need to hash the pass now
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(registerRequest.getRole());
        user.setHashedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus("ACTIVE");
        user.setEmailVerified(false);
        return userRepository.save(user);
    }
    public User loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getHashedPassword())) {
            return null;
        }
        return user;
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

