package com.example.demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Administrator on 2019/3/18 0018.
 */
@RestController
public class TestController {

    @GetMapping("test")
    public String test(){
        return "test....";
    }
}
