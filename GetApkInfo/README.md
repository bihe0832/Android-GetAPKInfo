## 背景

作为基于Android平台的渠道SDK的开发者或者联调同学每天都会面对大量的apk，需要检查他们的包名、版本、应用签名等信息，尤其现在Android-V2Sginature带来的问题更多(我会在另一篇文章中介绍)，为了提高工作效率，整理了一个获取应用基本信息的工具。下面主要是工具使用的方法，具体怎么实现可以参考源码。

具体的实现原理包括怎么生成可执行jar，怎么混淆jar等，我会在另一篇文章中说明。


根据 [https://github.com/bihe0832/Android-GetAPKInfo/issues/2](https://github.com/bihe0832/Android-GetAPKInfo/issues/2) 的需求，增加了获取更多信息的jar：getMorePackageInfo.jar；可以直接在根目录下载。由于个人实际开发中并不需要其余信息，因此在代码中**新增字段在打印时被注释了**。如果有相关的需求，可以删除com.bihe0832.packageinfo.bean.ApkInfo的toString中相关的注释。如果有更多字段的添加需求，可以参照com.bihe0832.packageinfo.utils.ApkUtil中的代码实现添加。

## 使用事例

### 查看帮助

	➜  java -jar ./GetAPKInfo.jar
	
	 usage: java -jar ./GetAPKInfo.jar [--version] [--help] [filePath]
	
	such as:
	
		 java -jar ./GetAPKInfo.jar --version
		 java -jar ./GetAPKInfo.jar --help
		 java -jar ./GetAPKInfo.jar ./test.apk

### 查看版本


	➜  java -jar ./GetAPKInfo.jar --version
	
	com.bihe0832.packageinfo.Main version 2.0 (GetApkInfo - 6)
	
	homepage : https://github.com/bihe0832/AndroidGetAPKInfo
	blog : http://blog.bihe0832.com
	github : https://github.com/bihe0832
		
	
### 查看应用信息

	➜  java -jar ./GetAPKInfo.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	
	执行结果: 成功
	应用信息:
	  包名: com.tencent.tmgp.yybtestsdk
	  版本名: 1.3.1
	  版本号: 1
	  渠道号: null
	  签名: 252e3ded833125ed3e3bb010bc24f4dc
	  使用V2签名: false
	  V2签名验证通过: false	
	  
	  
	  ➜  java -jar ./GetMorePackageInfo.jar ./test.apk
	  
	执行结果: 成功
	应用信息:
	  包名: com.tencent.jygame
	  版本名: 0.0.1.30
	  版本号: 287
	  签名文件MD5: 634b6933d798de3498f20a9b02452575
	  SDK版本:
	      minSdkVersion:12
	      targetSdkVersion:25
	  V1签名验证通过: true
	  使用V2签名: true
	  V2签名验证通过: true
	  使用权限列表:
	      android.permission.INTERNET
	      android.permission.VIBRATE
	      android.permission.MOUNT_UNMOUNT_FILESYSTEMS
	      android.permission.WRITE_EXTERNAL_STORAGE
	      android.permission.ACCESS_NETWORK_STATE
	      android.permission.ACCESS_WIFI_STATE
	      android.permission.READ_PHONE_STATE