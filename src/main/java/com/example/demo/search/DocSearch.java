package com.example.demo.search;

import com.example.demo.model.DocInfo;
import com.example.demo.model.Result;
import com.example.demo.model.Wight;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-09
 * Time: 19:03
 */
@Service
// 这个类来完成与用户交互的过程(接收查询词, 返回查询结果)
public class DocSearch {

    // 暂停词文件路径
    @Value("${stopword.file}")
    private String STOP_WORDS_PATH;

    private HashSet<String> hashSet = new HashSet<>();

    @Autowired
    private Index index;

    // 加载索引
    @PostConstruct  // 无法在注入对象时构造, 可以在 bean 对象注册前后, 提供的的准备和事后的方法和接口中执行
    public void loadDependFile() {
        index.load();
        // 预热加载
        ToAnalysis.parse("").getTerms();
        filterStopWords();
    }

    // 这个方法完成
    // 1. 分词, 对query进行分词
    // 2. 触发, 对分词结果来进行倒排查询
    // 3. 排序, 对查询结果以权重进行降序排序
    // 4. 包装, 返回结果是(标题 + url + 描述)
    public List<Result> search(String query) {
        // 1. 分词, 对query进行分词, 并且过滤掉停用词
        List<Term> oldTerms = ToAnalysis.parse(query).getTerms();
        List<Term> terms = new ArrayList<>();
        for (Term term : oldTerms) {
            if (hashSet.contains(term.getName())) {
                continue;
            }
            terms.add(term);
        }
        // 2. 触发, 对分词结果来进行倒排查询
        List<List<Wight>> allWightList = new ArrayList<>();
        for (Term term : terms) {
            String word = term.getName();
            List<Wight> wightList = index.getInverted(word);
            if (wightList == null) {
                // 不存在 wight 列表, 说明词在所有文档中都不存在(如果到最后, 所有的词在文档中到找不到, 返回的将会是一个空 list 集合)
                continue;
            }
            // 汇总所有的 wight ,再进行去重
            allWightList.add(wightList);
        }

        // 3. 合并相同搜索项(合并权重)
        List<Wight> mergeList = mergeWight(allWightList);

        // 4. 排序, 对查询结果以权重进行降序排序
        mergeList.sort(new Comparator<Wight>() {
            @Override
            public int compare(Wight o1, Wight o2) {
                // 降序
                return o2.getWight() - o1.getWight();
            }
        });

        // 5. 包装, 返回结果是(标题 + url + 描述)
        System.out.println("开始包装!");
        long start4 = System.currentTimeMillis();
        List<Result> resultList = new ArrayList<>();
        for (Wight wight : mergeList) {
            DocInfo docInfo = index.getForwardDocInfo(wight.getDocId());
            Result result = new Result();
            result.setTitle(docInfo.getTitle());
            result.setUrl(docInfo.getUrl());
            result.setDesc(getDesc(docInfo.getContent(), terms));
            resultList.add(result);
        }
        long end4 = System.currentTimeMillis();
        System.out.println("包装结束, 耗时: " + (end4 - start4) + "ms");
        return resultList;
    }

    static class Pos {
        public int row;
        public int col;

        public Pos(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    // 合并权重
    private List<Wight> mergeWight(List<List<Wight>> source) {
        // 1. 先升序排列每个 weight 列表(将 docId 小的排到前面)
        for (List<Wight> cowWight : source) {
            cowWight.sort(new Comparator<Wight>() {
                @Override
                public int compare(Wight w1, Wight w2) {
                    return w1.getDocId() - w2.getDocId();
                }
            });
        }
        // 2. 借助优先级队列(堆), 来排列每个 wight(这里我们把堆的类型改为 pos, 表示每一个 wight 对象, 再对相同的 wight 进行合并)
        // 我们要求堆顶始终为 docId 最小的 wight 对象, 方便进行 wight 合并
        PriorityQueue<Pos> queue = new PriorityQueue<>(new Comparator<Pos>() {
            @Override
            public int compare(Pos o1, Pos o2) {
                Wight w1 = source.get(o1.row).get(o1.col);
                Wight w2 = source.get(o2.row).get(o2.col);
                return w1.getDocId() - w2.getDocId();
            }
        });

        // 接收对象
        List<Wight> result = new ArrayList<>();

        // 2. 初始化堆
        for (int row = 0; row < source.size(); row++) {
            Pos pos = new Pos(row, 0);
            queue.offer(pos);
        }

        // 3. 取出堆的一个 wight 对象, 加入到 结果 wight 列表中
        while (!queue.isEmpty()) {
            Pos minPos = queue.poll();
            Wight curWight = source.get(minPos.row).get(minPos.col);
            if (!result.isEmpty()) {
                Wight lastWight = result.get(result.size() - 1);
                if (curWight.getDocId() == lastWight.getDocId()) {
                    lastWight.setWight(curWight.getWight() + lastWight.getWight());
                } else {
                    result.add(curWight);
                }
            } else {
                result.add(curWight);
            }

            // 4. 移动光标, 再添加一个 wight 对象进堆
            Pos nextPos = new Pos(minPos.row, minPos.col + 1);
            if (nextPos.col >= source.get(minPos.row).size()) {
                // 本 wight 列表到末尾了
                continue;
            }
            queue.offer(nextPos);
        }
        return result;
    }


    //-------------------------------------------------------------------------------------------------------------------------------------
    private String getDesc(String content, List<Term> terms) {
        int firstPos = -1;
        for (Term term : terms) {
            String word = term.getName();
            // 用正则表达式来解决匹配问题(将边界替换成空格, 在来查找, 注意这个是查找词, 不含边界, 替换的时候也仅仅是替换词本身)
            content = content.toLowerCase().replaceAll("\\b" + word + "\\b", " " + word + " ");
            firstPos = content.indexOf(" " + word + " ");
            if (firstPos >= 0) {
                // 找到了, 直接跳出
                break;
            }
        }

        if (firstPos == -1) {
            // 没找到(这个情况的可能极小, 在标题中存在, 而在正文中不存在, 这个时候我们返回正文的前一部分)
            if (content.length() >= 300) {
                return content.substring(0, 300);
            }
            return content;
        }

        //返回描述
        String desc = "";
        int descStart = firstPos < 60 ? 0 : firstPos - 60;
        if (descStart + 240 > content.length()) {
            desc = content.substring(descStart);
        } else {
            desc = content.substring(descStart, descStart + 240) + "...";
        }

//        int descStart = content.indexOf("<div class=\"block\">");
//        // 如果 descStart = -1, 说明这个网页是一个列表, 而不是具体的类, 这个时候, 我们返回一个提示信息.
//        if (descStart == -1) {
//            return "这个网页描述的是一个列表或者是包的结构层次, 并非具体的类或接口, 不是您要查找的内容.";
//        }
//        desc = content.substring(descStart);
//        int descEnd = desc.indexOf("</div>");
//        desc = desc.substring(0, descEnd);
//
//
//        desc = desc.replaceAll("<pre>(.*?)</pre>", " ");
//        desc = desc.replaceAll("<table.*?>(.*?)</table>", " ");
//        desc = desc.replaceAll("<.*?>", " ");
//        desc = desc.replaceAll("\\s+", " ");
//
//        if (desc.length() >= 380) {
//            desc = desc.substring(0, 380) + "...";
//        }

        // 这里得到最后的 desc, 可以对其进行标红, 即后端在把相关的 词 -> <i>词</i>, 然后前端对 i 标签进行 css 表示,
        for (Term term : terms) {
            String word = term.getName();
            // (?i)是忽略大小写进行匹配
            desc = desc.replaceAll("(?i) " + word + " ", "<i> " + word + " </i>");
        }

        return desc;
    }

    public void filterStopWords() {
        try (BufferedReader renderBuffer = new BufferedReader(new FileReader(STOP_WORDS_PATH))) {
            while (true) {
                String line = renderBuffer.readLine();
                if (line == null) {
                    break;
                }
                hashSet.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        DocSearch docSearch = new DocSearch();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            System.out.println("---------------------------------------------------------");
            List<Result> results = docSearch.search(scanner.nextLine());
            System.out.println(results);
            System.out.println(results.size());
        }
    }


}
