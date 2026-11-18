package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.model.Staff;
import com.cafemanagement.cafeprediction.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {


    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private LoginSessionService sessions;

    @Override
    public Optional<Staff> findUserByJwtToken(String jwt) throws Exception {

        return findUserByEmail(sessions.authenticate(jwt).getName());
    }

    @Override
    public Optional<Staff> findUserByEmail(String email) throws Exception {

        //option 1: kalau tak direct return, perlu buat condition checking
//        Optional<Staff> user = authRepository.findByEmail(email);
//        if (user.isEmpty()) {
//            throw new Exception("email not found");
//        }

//        return user;

        // option 2, caller akan decide untuk optional dengan guna orLese throw
        return authRepository.findByEmail(email);

    }

}
