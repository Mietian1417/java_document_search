package com.example.demo.model;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-09
 * Time: 10:38
 */

// 这个类表示文档的具体信息
@Data
public class DocInfo {
    private int docId;
    private String title;
    private String url;
    private String content;
}
