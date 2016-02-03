package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Foo {
    private final String fooPassword;

    @Autowired
    public Foo(@Value("${foo.password}") String fooPassword) {
        System.out.println("foo password is " + fooPassword);
        this.fooPassword = fooPassword;
    }
}
