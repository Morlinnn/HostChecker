package com.morlinnn.hostchecker;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于使用 ping 检查 Windows Host 地址有效性。
 * 既可以从 Windows Host 文件加载 dns 信息, 也
 * 可以从支持的文件或字符串中进行加载
 * <p><a href="https://github.com/Morlinnn">github Morlinnn</a></p>
 * @author Morlinnn
 */
public class HostChecker {
    private final String dir;
    private final String fileName;
    private List<String> lines;
    private List<Integer> validLines;
    private final AddressFilter filter;
    private int readValidLinesIndex = 0;
    private boolean isModified = false;
    private char annotation = '#';

    /**
     * 从 Host 文件中加载 DNS, 如果是系统文件需要管理员权限
     * @param dir 文件所在路径
     * @param fileName 文件名称
     * @param filter 过滤地址(标准 Host 文件为 "Address Domain", 此处为 Address 的过滤)的列表
     * @throws AccessDeniedException 没有文件夹/文件的读权限
     */
    public HostChecker(String dir, String fileName, AddressFilter filter) throws AccessDeniedException {
        this.filter = filter;
        this.dir = dir;
        this.fileName = fileName;
        init();
    }

    /**
     * 从 Host 文件中加载 DNS, 如果是系统文件需要管理员权限
     * @param dir 文件所在路径
     * @param fileName 文件名称
     * @param filter 过滤地址(标准 Host 文件为 "Address Domain", 此处为 Address 的过滤)的列表
     * @param annotation 注释符号
     * @throws AccessDeniedException 没有文件夹/文件的读权限
     */
    public HostChecker(String dir, String fileName, AddressFilter filter, char annotation) throws AccessDeniedException {
        this.filter = filter;
        this.dir = dir;
        this.fileName = fileName;
        this.annotation = annotation;
        init();
    }

    /**
     * 从字符串中加载 DNS, 如果是系统文件夹需要管理员权限
     * @param hostString 原字符串
     * @param saveDir 保存路径
     * @param fileName 保存名称
     * @param filter 过滤地址(标准 Host 文件为 "Address Domain", 此处为 Address 的过滤)的列表
     * @throws AccessDeniedException 没有文件夹/文件的读权限
     */
    public HostChecker(String hostString, String saveDir, String fileName, AddressFilter filter) throws AccessDeniedException {
        this.fileName = fileName;
        this.dir =saveDir;
        this.filter = filter;
        initFromString(hostString);
    }

    /**
     * 从字符串中加载 DNS, 如果是系统文件夹需要管理员权限
     * @param hostString 原字符串
     * @param saveDir 保存路径
     * @param fileName 保存名称
     * @param filter 过滤地址(标准 Host 文件为 "Address Domain", 此处为 Address 的过滤)的列表
     * @param annotation 注释符号
     * @throws AccessDeniedException 没有文件夹/文件的读权限
     */
    public HostChecker(String hostString, String saveDir, String fileName, AddressFilter filter, char annotation) throws AccessDeniedException {
        this.fileName = fileName;
        this.dir =saveDir;
        this.filter = filter;
        this.annotation = annotation;
        initFromString(hostString);
    }

    /**
     * 如果文件夹或文件没有读权限抛出异常
     * @param file 文件夹或文件
     * @throws AccessDeniedException 没有读权限
     */
    private static void checkReadableMessage(File file) throws AccessDeniedException {
        if (!Files.isReadable(file.toPath())) {
            throw new AccessDeniedException("Have no permission to read " + file.getAbsolutePath());
        }
    }

    /**
     * 如果文件夹或文件没有写权限抛出异常
     * @param file 文件夹或文件
     * @throws AccessDeniedException 没有写权限
     */
    private static void checkWriteableMessage(File file) throws AccessDeniedException {
        if (!Files.isWritable(file.toPath())) {
            throw new AccessDeniedException("Have no permission to write " + file.getAbsolutePath());
        }
    }

    private void init() throws AccessDeniedException {
        // 检查文件夹的读权限
        checkReadableMessage(new File(dir));
        // 检查文件的读权限
        checkReadableMessage(new File(dir, fileName));
        try {
            loadFromFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串的加载方法
     * @param hostString 原字符串
     */
    private void initFromString(String hostString) throws AccessDeniedException {
        // 检查文件夹的读权限
        checkReadableMessage(new File(dir));

        String[] strings = hostString.split("(\r\n|\n|\r)");
        validLines = new ArrayList<>();
        this.lines = new ArrayList<>();

        for (int i = 0; i < strings.length; i++) {
            String str = strings[i];
            if (str == null) break;
            lines.add(str);

            Map.Entry<String, String> dns = readValidString(str);
            if (dns == null) continue;

            if (!filter.filterAddress(dns.getKey())) validLines.add(i+1);
        }
        printLoadMessage(validLines.size());
    }

    /**
     * 文件的加载方法
     * @throws IOException
     */
    private void loadFromFile() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(dir, fileName), "r");
        validLines = new ArrayList<>();
        lines = new ArrayList<>();

        int line = 1;
        while (true) {
            String str = raf.readLine();
            if (str == null) break;
            lines.add(str);

            Map.Entry<String, String> dns = readValidString(str);
            if (dns == null) {
                line++;
                continue;
            }

            if (!filter.filterAddress(dns.getKey())) {
                validLines.add(line);
            }
            line++;
        }
        raf.close();
        printLoadMessage(validLines.size());
    }

    private void printLoadMessage(int loadSize) {
        System.out.println("load " + loadSize + " valid dns\n");
    }

    /**
     * 获取字符串中有效的地址和域名
     * @param str 原字符串
     * @return key: 地址, value: 域名
     */
    public Map.Entry<String, String> readValidString(String str) {
        if (str == null || str.length() < 4) return null;

        int addrStart = -1;
        int addrEnd = -1;
        int domainStart = -1;
        int domainEnd = -1;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == annotation) break;
            if (addrStart == -1) {
                if (isValidAddress((byte) str.charAt(i))) {
                    addrStart = i;
                    continue;
                }
            }
            if (addrStart != -1 && addrEnd == -1 && !isValidAddress((byte) str.charAt(i))) {
                addrEnd = i;
                continue;
            }
            if (domainStart == -1 && addrEnd != -1 && isValidAddress((byte) str.charAt(i))) {
                domainStart = i;
                continue;
            }
            if (domainStart != -1) {
                if (i == str.length()-1 && isValidAddress((byte) str.charAt(i))) {
                    domainEnd = i + 1;
                    break;
                }
                if (!isValidAddress((byte) str.charAt(i))) {
                    domainEnd = i;
                    break;
                }
            }
        }
        if (addrEnd == -1 || domainStart == -1 || domainEnd == -1) return null;
        String addr = str.substring(addrStart, addrEnd);
        String domain = str.substring(domainStart, domainEnd);
        return new AbstractMap.SimpleEntry<>(addr, domain);
    }

    /**
     * 检查是否是合法的地址字符
     * @param c 待检查的字符
     * @return 如果是则返回 true, 否在返回 false
     */
    private boolean isValidAddress(byte c) {
        return c != ' ' && c != annotation && c != '\r' && c != '\n';
    }

    /**
     * 获取下一个数据包{@link ReadPack}
     * @return 数据包
     */
    public ReadPack readNextPack() {
        if (readValidLinesIndex == validLines.size()) return null;
        String lineString = lines.get(validLines.get(readValidLinesIndex)-1);
        int line = validLines.get(readValidLinesIndex);
        readValidLinesIndex++;

        Map.Entry<String, String> dns = readValidString(lineString);
        return new ReadPack(dns.getKey(), dns.getValue(), line);
    }

    /**
     * 获取所有 ping 超时的行
     * @param threadNum 测试的线程数
     * @param timeout 超时时间
     * @return 所有 ping 超时的行
     * @throws InterruptedException
     */
    public Set<Integer> getPingTimeoutLines(int threadNum, int timeout) throws InterruptedException {
        ExecutorService executors = Executors.newFixedThreadPool(threadNum);
        CountDownLatch latch = new CountDownLatch(validLines.size());
        int tempLine = readValidLinesIndex;
        Set<Integer> failedSet = new HashSet<>();

        readValidLinesIndex = 0;
        while (true) {
            ReadPack pack = readNextPack();
            // 获取了所有的 DNS 键值对, 需要还原readValidLinesIndex
            if (pack == null) {
                readValidLinesIndex = tempLine;
                break;
            }
            Runnable ping = () -> {
               try {
                   int pingTime = ping(pack.addr, timeout);
                   if (pingTime == -1) {
                       System.out.println(
                               "ping: " + pack.domain + "\n      "
                               + pack.addr
                               + " is time out"
                       );
                       // 涉及异步调用共享资源需要确保操作同步
                       synchronized (this) {
                           failedSet.add(pack.lineIndex);
                       }
                   } else {
                       System.out.println(
                               "ping: " + pack.domain + "\n      "
                               + pack.addr
                               + " ping: "
                               + pingTime
                               + "ms"
                       );
                   }
                   latch.countDown();
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
            };
            executors.submit(ping);
        }
        latch.await();
        executors.shutdown();
        return failedSet;
    }

    /**
     * ping 测试
     * @param address 测试的地址
     * @param timeout 超时时间
     * @return 如果未超时返回 ping 时间 (ms), 否在返回-1
     * @throws IOException
     */
    public static int ping(String address, int timeout) throws IOException {
        long pingStart = System.currentTimeMillis();
        if (!InetAddress.getByName(address).isReachable(timeout)) {
            return -1;
        }
        long pingEnd = System.currentTimeMillis();
        return (int) (pingEnd - pingStart);
    }

    /**
     * 获取有效行索引在内容中的位置
     * @param validLineIndex 有效行的索引
     * @return 有效行索引在内容中的位置
     */
    public int getLine(int validLineIndex) {
        return validLines.get(validLineIndex);
    }

    /**
     * 跳转到有效行的位置
     * @param validLineIndex 有效行的索引
     * @return 内容中所在的位置
     */
    public int seekValidLineIndex(int validLineIndex) {
        if (validLines.contains(validLineIndex)) readValidLinesIndex = validLineIndex;
        return validLines.get(validLineIndex);
    }

    /**
     * 跳转到内容所在行, 如果行不是有效行则不执行操作
     * @param line 需要到达的行
     * @return 如果是有效行则返回内容中所在的位置, 否则返回-1
     */
    public int seekLine(int line) {
        if (validLines.contains(line)) {
            readValidLinesIndex = validLines.indexOf(line);
            return line;
        } else {
            return -1;
        }
    }

    /**
     * 移除所有在 removeIndexes 中行的读取内容的行
     * @param removeIndexes 需要移除的行索引
     */
    public void removeFromIndexes(Set<Integer> removeIndexes) {
        if (removeIndexes == null || removeIndexes.isEmpty()) {
            printRemoveMessage(validLines.size(), validLines.size(), 0);
            return;
        }

        List<Integer> sorted = new ArrayList<>(removeIndexes);
        sorted.sort((i1, i2) -> {
            if (Objects.equals(i1, i2)) {
                return 0;
            } else if (i1 > i2) {
                return -1;
            } else {
                return 1;
            }
        });

        int removed = 0;
        int total = validLines.size();
        for (int index : sorted) {
            System.out.println("remove: " + lines.remove(index - 1));
            validLines.remove(validLines.indexOf(index));
            removed++;
            if (!isModified) isModified = true;
        }
        printRemoveMessage(total, validLines.size(), removed);
    }

    private void printRemoveMessage(int total, int left, int removed) {
        System.out.println(
                "\n"
                        + total
                        + " in total, "
                        + left
                        + " is left, "
                        + removed
                        + " is removed"
        );
    }

    /**
     * 将旧的文件改名为 xxx.backup 使用原文件名称创建新的文件
     */
    public void save() throws IOException {
        if (!isModified) {
            System.out.println("Without modified, nothing is saved");
            return;
        }

        printSaveFileStartMessage();
        File file = new File(dir, fileName);
        Path filePath = file.toPath();
        Path backup = null;

        checkWriteableMessage(file);

        // rename to backup
        if (file.exists()) {
            backup = Paths.get(dir + "\\" + fileName + ".backup");
            int index = 1;
            while (true) {
                if (!backup.toFile().exists()) {
                    Files.move(filePath, filePath.resolveSibling(backup));
                    break;
                }
                backup = Paths.get(dir + "\\" + fileName + " (" + index + ").backup");
                index++;
            }
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            lines.forEach(writer::println);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        printSaveFileEndMessage(dir, backup, file.getName());
    }

    private void printSaveFileStartMessage() {
        System.out.println(
                "save file start."
        );
    }

    private void printSaveFileEndMessage(String dir, Path backupName, String newFileName) {
        if (backupName != null) {
            System.out.println(
                    "\nIn \""
                            + dir
                            + "\", old file is rename to \""
                            + backupName.getFileName()
                            + "\", new file is \""
                            + newFileName
                            + "\"."
            );
        } else {
            System.out.println(
                    "\nIn \""
                            + dir
                            + "\", new file is \""
                            + newFileName
                            + "\"."
            );
        }
    }

    /**
     * 对所有读取的内容进行 ping 测试并将超时部分移除, 将旧的文件改名为 xxx.backup 使用原文件名称创建新的文件
     * @param threadNum ping 线程数
     * @param timeout ping 超时时间
     */
    public void pingAndResolve(int threadNum, int timeout) throws InterruptedException, IOException {
        removeFromIndexes(getPingTimeoutLines(threadNum, timeout));
        save();
    }

    /**
     * 存储地址, 域名, 所在信息所在行
     */
    public static class ReadPack {
        final String addr;
        final String domain;
        final Integer lineIndex;

        public ReadPack(String addr, String domain, int lineIndex) {
            this.addr = addr;
            this.domain = domain;
            this.lineIndex = lineIndex;
        }

        @Override
        public String toString() {
            return "[address=" + addr + ", domain=" + domain + ", lineIndex=" + lineIndex + "]\n";
        }
    }
}
