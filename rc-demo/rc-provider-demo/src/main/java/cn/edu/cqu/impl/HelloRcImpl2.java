package cn.edu.cqu.impl;

import cn.edu.cqu.HelloRc2;
import cn.edu.cqu.annotation.RcApi;

@RcApi
public class HelloRcImpl2 implements HelloRc2 {
    @Override
    public String sayHi(String msg) {
        return "Hi consumer: " + msg;
    }
}
