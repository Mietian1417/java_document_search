package com.example.demo.search;

import com.example.demo.model.DocInfo;
import com.example.demo.model.Wight;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-09
 * Time: 10:05
 */

// 这个类来完成索引的一系列的操作
// 1. 正排索引查询
// 2. 倒排索引查询
// 3. 添加索引
// 4. 将索引从内存保存到磁盘中
// 5. 将索引从磁盘加载到内存中
@Service
public class Index {

    // 正排索引集合的锁
    private Object forwardLocker = new Object();

    // 倒排索引集合的锁
    private Object invertedLocker = new Object();

    // 保存索引的目录
    @Value("${index.dir}")
    private String SAVE_INDEX_DIR;

    // 通过这个对象来完成索引的序列化和反序列化
    private ObjectMapper objectMapper = new ObjectMapper();

    // 正排索引的集合
    private ArrayList<DocInfo> forwardIndex = new ArrayList<>();

    // 倒排索引的集合(key 是词, value 是 相关的文档, 以wight来权衡相关性)
    private HashMap<String, List<Wight>> invertedIndex = new HashMap<>();

    // 1. 正排索引查询 (返回一个文档)
    public DocInfo getForwardDocInfo(int docId) {
        return forwardIndex.get(docId);
    }

    // 2. 倒排索引查询(返回一系列相关的文档, 以 Wight 这个类来权衡相关级别)
    public List<Wight> getInverted(String term) {
        return invertedIndex.get(term);
    }

    // 3. 添加索引
    public void addIndex(String title, String url, String content) {
        // 构建正排索引
        DocInfo docInfo = buildForwardIndex(title, url, content);
        // 构建倒排索引
        buildInvertedIndex(docInfo);
    }

    private void buildInvertedIndex(DocInfo docInfo) {
        // 创建一个内部类, 来统计词出现的次数(要不然或许, 要用两个 map 分别操作 title 和 content)
        class TermCount {
            public int titleCount;
            public int contentCount;
        }
        // map 来统计词出现的次数
        HashMap<String, TermCount> worldCount = new HashMap<>();

        // 对标题进行分词并统计词出现的次数
        List<Term> titleTerms = ToAnalysis.parse(docInfo.getTitle()).getTerms();
        for (Term term : titleTerms) {
            String word = term.getName();
            TermCount oldTermCount = worldCount.get(word);
            if (oldTermCount == null) {
                // 新词, 添加词
                TermCount newWordCount = new TermCount();
                newWordCount.titleCount = 1;
                newWordCount.contentCount = 0;
                worldCount.put(word, newWordCount);
            } else {
                // 不是新词, 则修改词的权重
                oldTermCount.titleCount += 1;
            }
        }
        // 对正文进行分词并统计词出现的次数
        List<Term> content = ToAnalysis.parse(docInfo.getContent()).getTerms();
        for (Term term : content) {
            String word = term.getName();
            TermCount oldTermCount = worldCount.get(word);
            if (worldCount.get(word) == null) {
                // 词不存在, 添加词
                TermCount newTermCount = new TermCount();
                newTermCount.titleCount = 0;
                newTermCount.contentCount = 1;
                worldCount.put(word, newTermCount);
            } else {
                //词存在, 则修改词的权重
                oldTermCount.contentCount += 1;
            }
        }
        // 权衡文档的相关性(这里以 10 * 词在标题出现的次数 + 词在正文出现的次数)
        // 遍历每一个词, 词不存在则构建 wight 列表, 词存在 则在 wight 列表中添加 wight
        // 由于在上面一个统计分词的时候就已经汇总出现的次数了, 所以不存在同一个词, 出现两个的情况
        for (Map.Entry<String, TermCount> entry : worldCount.entrySet()) {
            // 由 倒排索引集合得到 wight 列表
            synchronized (invertedLocker) {
                List<Wight> invertedList = invertedIndex.get(entry.getKey());
                if (invertedList == null) {
                    // 不存在词对应的 wight 列表, 则创建 wight 列表
                    ArrayList<Wight> newInvertedList = new ArrayList<>();
                    Wight wight = new Wight();
                    wight.setDocId(docInfo.getDocId());
                    wight.setWight(entry.getValue().titleCount * 10 + entry.getValue().contentCount);
                    newInvertedList.add(wight);
                    invertedIndex.put(entry.getKey(), newInvertedList);
                } else {
                    //存在词对应的 wight 列表, 则添加 wight
                    Wight wight = new Wight();
                    wight.setDocId(docInfo.getDocId());
                    wight.setWight(entry.getValue().titleCount * 10 + entry.getValue().contentCount);
                    invertedList.add(wight);
                }
            }
        }
    }

    private DocInfo buildForwardIndex(String title, String url, String content) {
        DocInfo docInfo = new DocInfo();
        docInfo.setTitle(title);
        docInfo.setUrl(url);
        docInfo.setContent(content);
        synchronized (forwardLocker) {
            docInfo.setDocId(forwardIndex.size());
            forwardIndex.add(docInfo);
        }
        return docInfo;
    }

    // 4. 保存索引到磁盘
    public void save() {
        System.out.println("开始保存索引! ");
        long begin = System.currentTimeMillis();
        File indexDir = new File(SAVE_INDEX_DIR);
        if (!indexDir.exists()) {
            indexDir.mkdirs();
        }
        File forwardIndexFile = new File(SAVE_INDEX_DIR + "forwardIndex.txt");
        File invertedIndexFile = new File(SAVE_INDEX_DIR + "invertedIndex.txt");
        try {
            objectMapper.writeValue(forwardIndexFile, forwardIndex);
            objectMapper.writeValue(invertedIndexFile, invertedIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("保存索引完成! 耗时" + (end - begin) + "ms");
    }

    // 5. 加载索引到内存
    public void load() {
        System.out.println("开始加载索引!");
        long begin = System.currentTimeMillis();
        File forwardIndexFile = new File(SAVE_INDEX_DIR + "forwardIndex.txt");
        File invertedIndexFile = new File(SAVE_INDEX_DIR + "invertedIndex.txt");
        try {
            forwardIndex = objectMapper.readValue(forwardIndexFile, new TypeReference<ArrayList<DocInfo>>() {
            });
            invertedIndex = objectMapper.readValue(invertedIndexFile, new TypeReference<HashMap<String, List<Wight>>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("加载索引完成! 耗时:" + (end - begin) + "ms");

    }

    public static void main(String[] args) {
        Index index = new Index();
        index.load();
    }
}
