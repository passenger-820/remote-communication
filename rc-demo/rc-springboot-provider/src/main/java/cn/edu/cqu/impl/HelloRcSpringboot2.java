package cn.edu.cqu.impl;

import cn.edu.cqu.HelloRc;
import cn.edu.cqu.annotation.RcApi;

@RcApi
public class HelloRcSpringboot2 implements HelloRc {
    @Override
    public String sayHi(String msg) {
        return "Hi springboot consumer: " + msg;
    }
}
