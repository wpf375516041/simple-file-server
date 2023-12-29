### 软件介绍
这是一款简单的文件上传下载服务器，特性如下：
1. 支持文件上传下载
2. 采用graalvm编译，无需安装java环境，一键运行
3. 采用springboot3框架，是一个学习的好demo

### 编译说明
Use this option if you want to explore more options such as running your tests in a native image.
The GraalVM `native-image` compiler should be installed and configured on your machine.

NOTE: GraalVM 22.3+ is required.

编译命令如下：

```
$ ./mvnw native:compile -Pnative
```

然后在target目录下会生成一个simple-file-server可执行文件，直接运行即可
```
$ target/simple-file-server
```

极限压缩可以使用upx软件
```
upx --best -k target/simple-file-server
```
极限压缩后的文件大小为：25M，可一键运行，无需安装java环境
![image](https://raw.githubusercontent.com/wpf375516041/simple-file-server/main/src/main/resources/static/picture/1.png)
![image](https://raw.githubusercontent.com/wpf375516041/simple-file-server/main/src/main/resources/static/picture/2.png)

### 软件截图
![image](https://raw.githubusercontent.com/wpf375516041/simple-file-server/main/src/main/resources/static/picture/3.png)
![image](https://raw.githubusercontent.com/wpf375516041/simple-file-server/main/src/main/resources/static/picture/4.png)
![image](https://raw.githubusercontent.com/wpf375516041/simple-file-server/main/src/main/resources/static/picture/5.png)
