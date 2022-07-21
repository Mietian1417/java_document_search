package com.example.demo.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-12
 * Time: 12:13
 */

@RestControllerAdvice
public class ErrorAdvice {

    @ExceptionHandler(Exception.class)
    public HashMap<String, Object> exception(){
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", -1);
        hashMap.put("message", "服务器处理异常, 在检查是否存在非法操作! ");
        return hashMap;
    }

    @ExceptionHandler(NotFoundQueryException.class)
    public HashMap<String, Object> notFoundQueryException(){
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", -1);
        hashMap.put("message", "查询词为空, 请输入查询词! ");
        return hashMap;
    }

    @ExceptionHandler(NotFoundWordInLibraryException.class)
    public HashMap<String, Object> notFoundWordInLibraryException(){
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", -1);
        hashMap.put("message", "查询库中还未录入该词! ");
        return hashMap;
    }
}
