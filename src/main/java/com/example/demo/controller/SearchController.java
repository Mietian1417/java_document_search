package com.example.demo.controller;

import com.example.demo.config.NotFoundQueryException;
import com.example.demo.config.NotFoundWordInLibraryException;
import com.example.demo.model.Result;
import com.example.demo.search.DocSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-12
 * Time: 11:26
 */

@RequestMapping("/java_document")
@RestController
public class SearchController {

    @Autowired
    private DocSearch docSearch;

    @RequestMapping("/search")
    public List<Result> getList(String query) throws NotFoundQueryException, NotFoundWordInLibraryException {
        if (query == null || "".equals(query)){
            throw new NotFoundQueryException();
        }
        List<Result> resultList = docSearch.search(query);
        if (resultList == null || resultList.isEmpty()){
            throw new NotFoundWordInLibraryException();
        }
        return resultList;
    }
}
