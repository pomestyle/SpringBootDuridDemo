package com.example.demo.pojo;


import lombok.Data;

@Data
public class User {

    private Long id;

    private String name;

    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
