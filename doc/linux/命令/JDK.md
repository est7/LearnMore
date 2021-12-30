1、官网下载jdk

下载链接  

http://www.oracle.com/technetwork/java/javase/downloads/index.html

可以根据自己的系统进行下载

2、进行解压

sudo tar -zxvf jdk-8u171-linux-x64.tar.gz 则解压到了当前目录下，解压后可以把解压文件移动到自己想要放的目录下，使用mv命令 sudo mv jdk1.8.0_171 /usr/lib/xxx

3、进行配置

使用全局设置方法，它是所有用户的共用的环境变量

命令如下：$sudo gedit ~/.bashrc

然后把如下命令复制到最底部

export JAVA_HOME=/usr/local/java/jdk1.8.0_25  
export JRE_HOME=${JAVA_HOME}/jre  
export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib  

export PATH=${JAVA_HOME}/bin:$PATH

export JAVA_HOME=后面要填写自己解压后的jdk的路径

4、生效~/.bashrc文件

命令如下：$sudo source ~/.bashrc

5、测试是否安装成功

java -version 查看版本号是否改变



JDK8在线安装
安装ppa

sudo add-apt-repository ppa:webupd8team/java 
sudo apt-get update

安装jdk

sudo apt-get install oracle-java8-installer

验证安装是否成功

java -version

成功后会出现：

java version “1.8.0_171” 
Java(TM) SE Runtime Environment (build 1.8.0_171-b11) 
Java HotSpot(TM) 64-Bit Server VM (build 25.171-b11, mixed mode)

如果系统中安装有多个JDK版本，则可以通过如下命令设置系统默认JDK为Oracle JDK 8：

sudo update-java-alternatives -s java-8-oracle

设置JAVA_HOME环境变量

经过上述过程时候JAVA_HOME对应的位置应该在/usr/lib/jvm/java-8-oracle处。

编辑/etc/profile文件，在文件末尾添加如下3行：

export JAVA_HOME=/usr/lib/jvm/java-8-oracle
export JRE_HOME=/usr/lib/jvm/java-8-oracle/jre
export PATH=${JAVA_HOME}/bin:$PATH

这里没有在环境变量PATH中添加JAVA信息的原因是：之前通过apt安装的时候已经设置好了，所以不用添加。 
并执行：

source /etc/profile

此时可以通过echo $JAVA_HOME来验证结果。
