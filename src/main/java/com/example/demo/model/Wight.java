package com.example.demo.model;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-09
 * Time: 10:42
 */

// 这个类来定义文档与词的相关性
@Data
public class Wight {
    private int docId;  // 文档id
    private int wight;  // 词与文档的相关性(wight 越大, 相关性越高)
}
