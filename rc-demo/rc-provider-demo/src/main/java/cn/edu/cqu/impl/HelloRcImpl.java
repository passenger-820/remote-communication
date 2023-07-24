package cn.edu.cqu.impl;

import cn.edu.cqu.HelloRc;

public class HelloRcImpl implements HelloRc {
    @Override
    public String sayHi(String msg) {
        return "Hi consumer: " + msg;
    }
}
