package cn.edu.cqu;

import cn.edu.cqu.annotation.RcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // 需要注入一个代理对象
    // 把这个bean处理下，通过此注解判断，强制创建一个代理
    @RcService
    private HelloRc helloRc;

    @GetMapping("hello")
    public String hello(){
        return helloRc.sayHi("provider");
    }
}
