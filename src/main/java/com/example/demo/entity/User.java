package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "demoUsers")
@Data
public class User {
    @Id @GeneratedValue
    private Long id;
    private String username;
    private String password;
}
