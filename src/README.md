## HostChecker
用于使用 ping 检查 Windows Host 地址有效性。
既可以从 Windows Host 文件加载 dns 信息, 也
可以从支持的文件或字符串中进行加载

### 支持的格式
- 默认使用 `#` 进行 `注释`, 目前只支持单个字符, 可在构造方法中重新指定 
- `DNS` 书写方法为 `127.0.0.1 example.com`
- 使用 `\r\n` `\r` `rn` 进行多行分隔

### 使用方法
#### 作为包使用方法
##### 从文件中加载
*如果是系统文件 `...\etc\host` 需要管理员权限才能操作*
```java
    public static void main(String[] args) {
        HostChecker checker = new HostChecker(
                // 文件夹位置
                "C:\\dir",
                // 文件名称
                "host",
                AddressFilter.getLoopbackFilter(),
                // 如果需要使用自定义注释可以在此处修改
                // '#'
        );
        // ping 操作的线程数, 超时时间
        // 旧的文件会被命名为host.backup, 新的文件使用旧文件名称
        checker.pingAndResolve(8, 10 * 1000);
    }
```
##### 从字符串中加载
```java
    public static void main(String[] args) {
        HostChecker checker = new HostChecker(
                "# annotation"
                + "127.0.0.1 localhost.test\n"
                + "8.8.0.0 example.test\n",
                // 保存的文件夹位置
                "C:\\dir",
                // 保存的文件名称
                "host",
                AddressFilter.getLoopbackFilter(),
                // 如果需要使用自定义注释可以在此处修改
                // '#'
        );
        // ping 操作的线程数, 超时时间
        // 旧的文件会被命名为host.backup, 新的文件使用旧文件名称
        checker.pingAndResolve(8, 10 * 1000);
    }
```
#### 控制台使用方法
```powershell
    # 使用默认的回环地址筛选
    # -threadNum ping 线程数 (默认为8)
    # -timeout 超时时间 (ms) 默认为 (10000ms)
    java -jar your_dir\HostChecker.jar "C:\example" "host" -threadNum 8 -timeout 10000
    # 使用自定义筛选
    # -annotation 为注释符号, 默认为 '#'
    # 过滤可不写, 使用默认的回环地址
    #   -filter 为过滤地址
    #   -regexp 后所有的参数都是正则表达式
    java -jar your_dir\HostChecker.jar "C:\example" "host" -annotation "#" -filter "3.3.3.3" "8::3:1" -regexp "8.9.\d+.\d+"
    # 处理字符串
    java -jar your_dir\HostChecker.jar "C:\example" "host" -string "8.8.0.0 example.test\n1::1 example.test"
```

### License
The MIT License (MIT)

当然, 如果注明原作者我会很高兴