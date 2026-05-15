package com.auction.model.user;
import com.auction.model.base.Entity;
import com.auction.model.user.UserRole;
import com.auction.model.user.UserStatus;

public abstract class User extends Entity {

    private String name;
    private String email;
    private String password;

    private UserRole role;      // dùng enum
    private UserStatus status;  // thêm status

    public User(int id, String name, String email, String password, UserRole role) {
        super(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserRole getRole() {
        return role;
    }
}