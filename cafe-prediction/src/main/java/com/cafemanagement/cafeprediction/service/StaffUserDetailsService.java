package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.model.enumoption.STAFF_ROLES;
import com.cafemanagement.cafeprediction.model.enumoption.USER_STATUS;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StaffUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private AuthRepository authRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Staff> staff = authRepository.findByEmail(email);
        if (staff.isEmpty()) {
            throw new UsernameNotFoundException("user not found with this email");
        }

        STAFF_ROLES role = staff.get().getRole();

        List<GrantedAuthority> authorityList = new ArrayList<>();
        authorityList.add(new SimpleGrantedAuthority(role.toString()));

        return User.withUsername(email).password(staff.get().getPassword())
                .authorities(authorityList).disabled(staff.get().getStatus() != USER_STATUS.USER_ACTIVE).build();
    }
}
