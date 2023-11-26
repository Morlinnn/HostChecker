package com.morlinnn.hostchecker;

import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println(
                    "参数:\n"
                    + "dir fileName\n"
                    + "-string 加载字符串\n"
                    + "-annotation 注释字符\n"
                    + "-threadNum ping线程数量\n"
                    + "-timeout 超时时间(ms)\n"
                    + "-filter 过滤地址\n"
                    + "-regexp 过滤正则表达式"
            );
        }

        String dir = args[0];
        String file = args[1];
        String string = null;
        int threadNum = 8;
        int timeout = 10000;
        char annotation = '#';
        Set<String> addressFilter = new HashSet<>();
        Set<String> regexpFilter = new HashSet<>();
        AddressFilter filter;
        // 0: 无状态, 1: -annotation 2: -filter 3: -regexp 4: string 5: threadNum 6: timeout
        int status = 0;
        for (int i = 2; i < args.length; i++) {
            if (i == 2 && args[2].equals("-string")) {
                status = 4;
                continue;
            }
            if (args[i].equals("-annotation")) {
                status = 1;
                continue;
            }
            if (args[i].equals("-filter")) {
                status = 2;
                continue;
            }
            if (args[i].equals("-regexp")) {
                status = 3;
                continue;
            }
            if (args[i].equals("-threadNum")) {
                status = 5;
                continue;
            }
            if (args[i].equals("-timeout")) {
                status = 6;
                continue;
            }
            if (status == 4) {
                string = args[i];
                status = 0;
                continue;
            }
            if (status == 1) {
                if (args[i].length() > 1) {
                    System.out.println("annotation 只能是单个字符");
                } else if (args[i].length() == 1) {
                    annotation = args[i].charAt(0);
                }
            }
            if (status == 2) {
                addressFilter.add(args[i]);
            }
            if (status == 3) {
                regexpFilter.add(args[i]);
            }
            if (status == 5) {
                threadNum = Integer.parseInt(args[i]);
            }
            if (status == 6) {
                timeout = Integer.parseInt(args[i]);
            }
        }
        if (addressFilter.isEmpty() && regexpFilter.isEmpty()) {
            filter = AddressFilter.getLoopbackFilter();
        } else {
            filter = new AddressFilter(addressFilter, regexpFilter);
        }
        HostChecker checker;
        if (string == null) {
            checker = new HostChecker(
                    dir,
                    file,
                    filter,
                    annotation
            );
        } else {
            checker = new HostChecker(
                    dir,
                    file,
                    string,
                    filter,
                    annotation
            );
        }
        checker.pingAndResolve(threadNum, timeout);
        checker.save();

    }
}
