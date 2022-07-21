package com.example.demo.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 陈子豪
 * Date: 2022-05-08
 * Time: 19:38
 */

//这个类完成解析所有的 html 文件, 制作索引并保存到文件中
@Service
public class Parser {

    @Autowired
    private Index index;

    // 由于在生产环境下, 可以直接用索引, 没有必要自己制作, 所以这个路径可以不更改
    private static final String INPUT_PATH = "D:/java_document/jdk-8u333-docs-all/docs/api";

    // 这个方法完成(单线程)
    //  1. 遍历指定目录下的所有 html 文件
    //  2. 解析每一个 html 文件的内容, 并构建索引
    //  3. 把构建好的索引存到文件中
    public void run() {
        //1.遍历目录下的所有的 html 文件
        ArrayList<File> fileList = new ArrayList<>();
        enumFile(INPUT_PATH, fileList);

        // 2. 解析所有的 html 文件, 并构建索引
        for (File file : fileList) {
            parseHTML(file);
        }

        // 3. 保存索引到文件中
        index.save();
    }

    // 这个方法完成(多线程)
    //  1. 遍历指定目录下的所有 html 文件
    //  2. 解析每一个 html 文件的内容, 并构建索引
    //  3. 把构建好的索引存到文件中
    public void runByThread() throws InterruptedException {
        System.out.println("开始解析 html 文件, 并开始制作索引! ");
        long begin = System.currentTimeMillis();
        //1.遍历目录下的所有的 html 文件
        ArrayList<File> fileList = new ArrayList<>();
        enumFile(INPUT_PATH, fileList);
        // 2. 解析所有的 html 文件, 并构建索引
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch countDownLatch = new CountDownLatch(fileList.size());
        long buildIndexStart = System.currentTimeMillis();
        for (File file : fileList) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("开始解析: " + file.getAbsolutePath());
                    parseHTML(file);
                    countDownLatch.countDown();
                    ;
                }
            });
        }
        countDownLatch.await();
        executorService.shutdown();
        long buildIndexEnd = System.currentTimeMillis();
        System.out.println("索引制造完成! 耗时: " + (buildIndexEnd - buildIndexStart) + "ms");
        // 3. 保存索引到文件中
        index.save();
        long end = System.currentTimeMillis();
        System.out.println("制作索引完成! 耗时: " + (end - begin) + "ms");
    }

    private void parseHTML(File file) {
        // 1. 解析 HTML 的标题
        String title = parseTitle(file);
        // 2. 解析 HTML 对应的 URL
        String url = parseUrl(file);
        // 3. 解析 HTML 对应的正文(截取一部分作为描述)
        String desc = parseContentByRegex(file);
        // 4. 将解析出来的内容添加到索引中
        index.addIndex(title, url, desc);
    }

    private String parseUrl(File file) {
        String webUrlPrefix = "https://docs.oracle.com/javase/8/docs/api";

        // windows 下 默认 \ , 最后要转化成 web 的 / (不转换也行, 现在的浏览器都有很强的容错能力, 会自动解析)
        String filePath = file.getAbsolutePath();

        String localUrlSuffix = filePath.substring(INPUT_PATH.length());

        String finalUrl = webUrlPrefix + localUrlSuffix;

        // 将 \ 替换成 /
        return finalUrl.replaceAll("\\\\", "/");
    }

    // 解析文正并简化存储信息
    private String parseContentByRegex(File file) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file), 1024 * 1024)) {  // 指定 1M 大小的缓冲区
            StringBuilder content = new StringBuilder();
            while (true) {
                int ret = bufferedReader.read();
                if (ret == -1) {
                    break;
                }
                char ch = (char) ret;
                if (ch == '\n' || ch == '\r') {
                    ch = ' ';
                }
                // 下一个字符开始读取
                content.append(ch);
            }
            String saveContent = content.toString();
            saveContent = saveContent.replaceAll("<script.*?>(.*?)</script>", " ");
            saveContent = saveContent.replaceAll("<script>(.*?)</script>", " ");
            saveContent = saveContent.replaceAll("<.*?>", " ");
            saveContent = saveContent.replaceAll("\\s+", " ");
//            saveContent = saveContent.replaceAll("<head>(.*?)</head>", " ");
//            saveContent = saveContent.replaceAll("<script.*?>(.*?)</script>", " ");
//            saveContent = saveContent.replaceAll("<script>(.*?)</script>", " ");
//            saveContent = saveContent.replaceAll("<div class=\"topNav\">(.*?)</div>", " ");
//            saveContent = saveContent.replaceAll("<div class=\"subNav\">(.*?)</div>", " ");
//
//            saveContent = saveContent.replaceAll("<div class=\"summary\">(.*)</div>", " ");
//
//            saveContent = saveContent.replaceAll("<div class=\"classUseContainer\">(.*)</div>", " ");
//
//            saveContent = saveContent.replaceAll("<div class=\"serializedFormContainer\">(.*)</div>", " ");
//
//            saveContent = saveContent.replaceAll("<dl>(.*?)</dl>", " ");
//
//            saveContent = saveContent.replaceAll("<tbody>(.*?)</tbody>", " ");
//
//
//            saveContent = saveContent.replaceAll("<p class=\"legalCopy\">(.*?)</p>", " ");
//
//            saveContent = saveContent.replaceAll("<tt>", " ");
//            saveContent = saveContent.replaceAll("</tt>", " ");
//            saveContent = saveContent.replaceAll("<i>", " ");
//            saveContent = saveContent.replaceAll("</i>", " ");
//            saveContent = saveContent.replaceAll("<code>", " ");
//            saveContent = saveContent.replaceAll("</code>", " ");
//            saveContent = saveContent.replaceAll("\\s+", " ");
//            saveContent = saveContent + '\n';
            return saveContent;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // 手动去除普通标签, 但 script 的代码被外露
    private String parseContent(File file) {
        // BufferedReader 存在缓冲区, 首先将文件全部读取到内存中(缓存), 再在内存中处理
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file), 1024 * 1024)) {  // 指定 1M 大小的缓冲区
            StringBuilder content = new StringBuilder();
            boolean flag = false; // 不读取
            while (true) {
                int ret = bufferedReader.read();
                if (ret == -1) {
                    break;
                }
                char ch = (char) ret;
                // 下一个字符开始读取
                if (ch == '>') {
                    flag = true;
                    continue;
                }

                //下一个字符停止读取
                if (ch == '<') {
                    flag = false;
                    continue;
                }

                if (flag) {
                    if (ch == '\n' || ch == '\r') {
                        ch = ' ';
                    }
                    content.append(ch);
                }
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String parseTitle(File file) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.indexOf("."));
    }

    // 递归文件
    private void enumFile(String inputPath, List<File> fileList) {
        File rootFile = new File(inputPath);
        File[] files = rootFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                enumFile(file.getAbsolutePath(), fileList);
            } else {
                if (file.getAbsolutePath().endsWith(".html")) {
                    fileList.add(file);
                }
            }
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser();
//        parser.run();
        parser.runByThread();
    }
}
