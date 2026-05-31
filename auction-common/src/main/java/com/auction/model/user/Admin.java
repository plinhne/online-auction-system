package com.auction.model.user;

public class Admin extends User {
    private static final long serialVersionUID = 1L;
    
    public Admin(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.ADMIN);
    }

    public void manageUsers() {
        // TODO
    }
}