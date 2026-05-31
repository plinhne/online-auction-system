package com.auction.model.base;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private int id;

    public Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}