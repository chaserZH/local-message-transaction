package com.charserzh.lmt.starter.test;

import org.springframework.stereotype.Service;

import java.io.Serializable;

public class TestEntity implements Serializable {
    private Long id;
    private String name;

    public TestEntity() {

    }

    public TestEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
