package com.app.med_support.security;

import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.List;

// this class takes the user data that in CLASS USER  like email and password
// and prepare it to the form that spring security understand it
public class MyUserDetails implements UserDetails {

    private final User user;
    public MyUserDetails(User user) {
        this.user = user;
    }
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    @Override
    public String getPassword() {
        return user.getHashedPassword();
    }
    // takes s the role from the user and give it ti spring security in a way the spring can understand
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

}
