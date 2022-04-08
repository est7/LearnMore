

ffmpeg 链接库

sudo apt install yasm

配置编译环境

./configure --prefix=/usr/local/ffmpeg --enable-gpl --enable-shared --enable-libx264

报错 265 not found using pkg--config

apt-get install pkg-config

make - j8

make install

apt 获取相关库

apt-cache search opencv

ubuntu 系统下 ImageMagick 入门：查看和修改图像的常见方法

https://blog.csdn.net/qianxuedegushi/article/details/103836308

安装ffmpeg
（1）下载X264：http://ffmpeg.org/releases/
    tar -xjvg ffmpeg-4.2.tar.bz2
    cd ffmpeg-4.2

（2）配置：
./configure --enable-shared -enable-libx264 --enable-gpl --extra-cflags=-I/usr/local/x264/include--extra-ldflags=-L/usr/local/x264/lib --prefix=/usr/local/ffmpeg
 其中需要引入x264的两个lib文件夹和include文件夹

 (3)如果第二步骤报错：yasm/nasm not found or too old..... （未报错请略过此步骤）
 需要安装新的ysam
 下载链接：http://yasm.tortall.net/Download.html
 tar -xvzf yasm-1.3.0.tar.gz
cd yasm-1.3.0/
./configure
make
make install
完成后，再执行第二步（需要回到ffmpeg文件夹）。
————————————————
版权声明：本文为CSDN博主「yc_way」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/yc_game/article/details/105139710