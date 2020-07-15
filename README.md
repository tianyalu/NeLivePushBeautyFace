# 直播美颜推流与视频录制
[TOC]  
## 一、简介

本文实现了直播美颜推流和视频录制功能，其中美颜推流数据处理路程如下图所示： 

![image](https://github.com/tianyalu/NeLivePushBeautyFace/raw/master/show/live_push_process.png)  

## 二、遗留问题

在点击"结束推流"按钮后程序崩溃，经排查是在`RtmpSender`的第194行出现的异常：  

```java
final int closeR = RtmpClient.close(jniRtmpPointer);
```

当前只是简单地把相关代码注释掉，程序得以不再崩溃，但未从根本上解决问题。