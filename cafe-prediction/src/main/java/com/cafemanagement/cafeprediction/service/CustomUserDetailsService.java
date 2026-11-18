//package com.cafemanagement.cafeprediction.service;
//
//import com.cafemanagement.cafeprediction.model.Staff;
//import com.cafemanagement.cafeprediction.model.enumoption.USER_STATUS;
//import com.cafemanagement.cafeprediction.repository.AuthRepository;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//
//@Service
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final AuthRepository staffRepository;
//
//    public CustomUserDetailsService(AuthRepository staffRepository) {
//        this.staffRepository = staffRepository;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        // Spring Security calls this "username" but we're using email
//        Staff staff = staffRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//
//        return User.builder()
//                .username(staff.getEmail()) // Use email as the username for Spring Security
//                .password(staff.getPassword())
//                .authorities(Collections.singletonList(new SimpleGrantedAuthority(staff.getRole().name())))
//                .disabled(staff.getStatus() == USER_STATUS.USER_INACTIVE)
//                .build();
//    }
//}
