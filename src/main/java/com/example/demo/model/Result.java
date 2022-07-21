package com.example.demo.model;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-09
 * Time: 19:07
 */

@Data
public class Result {
    private String title;
    private String url;
    // desc 是对 content 的一段截取(简要描述)
    private String desc;
}
