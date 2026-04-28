package com.auction.model.user;

import com.auction.model.base.Entity;

public abstract class User extends Entity {
    private String name;
    private String email;

    public User(String id, String name, String email) {
        super(id);
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}