package com.example.coupleexpensetracker.models;

import com.google.firebase.Timestamp;

public class User {

    public String uid;
    public String name;
    public String email;
    public String phone;
    public String gender;
    public String dob;
    public String profilePic;
    public boolean isAdmin;

    public Timestamp createdAt;
    public User() {}
}
