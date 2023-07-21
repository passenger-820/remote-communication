package cn.edu.cqu.impl;

import cn.edu.cqu.HelloRC;

public class HelloRCImpl implements HelloRC {
    @Override
    public String sayHi(String msg) {
        return "Hi consumer: " + msg;
    }
}
