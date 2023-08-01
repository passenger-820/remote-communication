package cn.edu.cqu.impl;

import cn.edu.cqu.HelloRc;
import cn.edu.cqu.annotation.RcApi;

@RcApi(group = "primary")
public class HelloRcImpl implements HelloRc {
    @Override
    public String sayHi(String msg) {
        return "Hi consumer: " + msg;
    }
}
