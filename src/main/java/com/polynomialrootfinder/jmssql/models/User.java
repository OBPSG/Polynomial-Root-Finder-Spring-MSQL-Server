package com.polynomialrootfinder.jmssql.models;


public class User {

    public String id;

    public String name;
    public String email;

    public User() {}

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User(String userName, String userEmail) {
        this.name = userName;
        this.email = userEmail;
    }

    public String toString() {
        return String.format("User[id=%s, name=%s, email=%s]", id, name, email);
    }
}
