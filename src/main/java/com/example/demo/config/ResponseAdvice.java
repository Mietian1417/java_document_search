package com.example.demo.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-12
 * Time: 12:18
 */

@ControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("success", 200);
        if (body instanceof HashMap){
            HashMap<String ,Object> acceptMap = (HashMap<String, Object>) body;
            hashMap.put("status", (int)acceptMap.get("status"));
            if (acceptMap.get("message") != null) {
                hashMap.put("data", acceptMap.get("message"));
                return hashMap;
            }
            return hashMap;
        }
        // 返回具体对象信息
        hashMap.put("status", 1);
        hashMap.put("data", body);
        return hashMap;
    }
}
