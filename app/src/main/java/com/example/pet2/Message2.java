package com.example.pet2;

// data class Message1(val id: Long, val time: Long, val text: String, val image: String)

public class Message2 {
    public String name;
    public String password;

    public Message2(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
