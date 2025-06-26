package com.example.demo.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.demo.entity.User;

public interface IUserService extends UserDetailsService {
    User register(User user);
}
