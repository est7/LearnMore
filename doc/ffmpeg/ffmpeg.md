1：图片合成视频：

ffmpeg -r {} -i {}%05d.jpg -vcodec libx264 -pix_fmt yuv420p -b {}k {}

-pix_fmt yuv420p ：目标像素 采样率   yuv422p  yuv444p 

2：无损合成

ffmpeg -r {} -i {}%05d.jpg -vcodec libx264rgb  -crf 28 -b {}k {}

-crf ： [Constant Rate Factor (CRF)](https://trac.ffmpeg.org/wiki/Encode/H.264#crf)  返回0-51  一般视觉无差别为 17到 28   51 效果最差



3：h264 -> h265

ffmpeg -i /home/sundu/Desktop/test/demo_video.mp4 -c:v libx265 output.mp4

