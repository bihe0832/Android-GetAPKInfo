# AndroidGetAPKInfo

## 项目简介

作为基于Android平台的渠道SDK的开发者或者联调同学每天都会面对大量的apk，需要检查他们的包名（packageName）、版本(versionName\versionCode)、应用签名(Signature)等信息，尤其现在Android的V2签名认证(APK Signature Scheme v2)带来的问题更多，为了提高工作效率，整理了一个获取应用基本信息的系列工具集。

目前主干已经修改为使用Android Studio来构建，如果需要使用Eclipse来构建，请查看项目分支：

Eclipse：[https://github.com/bihe0832/Android-GetAPKInfo/tree/eclipse](https://github.com/bihe0832/Android-GetAPKInfo/tree/eclipse)
## 目录结构

	├── AndroidGetSignature.apk : 一款基于Android Studio开发的通过包名获取apk签名的应用
	│
	├── AndroidGetSignature : AndroidGetSignature.apk对应源码
	│
	├── AXMLPrinter2_zixie.jar ：对于官方工具AXMLPrinter2的优化，解决因为不同api下apk的AndroidMainfest编码引起的问题
	│
	├── AXMLPrinter2_zixie ：AXMLPrinter2_zixie.jar的源码
	│
	├── CheckAndroidV2SignatureByAPKSig ：CheckAndroidV2SignatureByAPKSig.jar的源码
	│
	├── CheckAndroidV2SignatureByAPKSig.jar ：基于官方签名相关的工具apksigner的源码改造的安卓签名校验工具
	│
	├── CheckAndroidV2Signature ：CheckAndroidV2Signature.jar的源码
	│
	├── GetApkInfo.jar ：一款基于Java环境的读取apk的包名、版本号、签名、是否使用V2签名，V2签名校验是否通过的工具
	│
	├── GetApkInfo ： getPackageInfo.jar的源码
	│
	├── apksig ： Android SDK Build Tools中关于签名相关的工具apksigner的源码（提供了V2签名以及校验的方法）
	│
	└── README.md
	
**备注：除了根目录，每个子项目下面都有对应功能介绍相关的ReadMe文件，如果想了解具体项目的详细信息，可以进入子项目查看**
	
## 使用方法

这里仅列出主要操作的使用方法，详细内容请查看对应目录的README文件。

### AndroidGetSignature.apk 

安装AndroidGetSignature.apk 后输入包名，点击获取

### AXMLPrinter2_zixie.jar 

非可执行jar，主要是对官方工具AXMLPrinter2针对不同api下AndroidMainfest编码不同导致解析异常的优化，解决[https://github.com/bihe0832/Android-GetAPKInfo/issues/1](https://github.com/bihe0832/Android-GetAPKInfo/issues/1) 和 [https://github.com/bihe0832/Android-GetAPKInfo/issues/5](https://github.com/bihe0832/Android-GetAPKInfo/issues/5)遇到的问题

### CheckAndroidV2Signature.jar

	➜  java -jar ./CheckAndroidV2Signature.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	{"ret":0,"msg":"ok","isV2":false,"isV2OK":false}

### CheckAndroidV2Signature.jar

	➜  java -jar ./CheckAndroidV2Signature.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	{"ret":0,"msg":"","isV1OK":true,"isV2":false,"isV2OK":false,"keystoreMd5":"252e3ded833125ed3e3bb010bc24f4dc"}
	
### getPackageInfo.jar

	➜  java -jar ./getPackageInfo.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	
	执行结果: 成功
	应用信息:
	  包名: com.huohoubrowser
	  版本名: 4.0.1.8
	  版本号: 4018
	  签名文件MD5: f430582429f49b685c3572ba28995e39
	  V1签名验证通过: false
	  使用V2签名: false
	  V2签名验证通过: false
	  签名验证失败原因: ERROR: JAR signer CERT.RSA: JAR signature META-INF/CERT.SF indicates the APK is signed using APK Signature Scheme v2 but no such signature was found. Signature stripped?

### getMorePackageInfo.jar

➜  java -jar ./getMorePackageInfo.jar ./test.apk
	  
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
	      
## 相关文章

- [一款基于Java环境的读取应用包名、签名、是否V2签名等基本信息的工具](http://blog.bihe0832.com/java-getpackageinfo.html)

- [关于Android的APK Signature Scheme v2签名相关的资料汇总](http://blog.bihe0832.com/android-v2.html)

- [Android的APK Signature Scheme v2签名及一款基于Java环境的校验工具介绍](http://blog.bihe0832.com/android-v2-signature.html)

- [如何使用Eclipse开发可执行Jar程序，并生成混淆过的jar程序](http://blog.bihe0832.com/runnable-jar.html)

- [使用Android Studio开发可独立运行（runnable）混淆过的Jar程序](http://blog.bihe0832.com/as-runnable-jar.html)

- [一个关于APK Signature Scheme v2签名的神奇bug定位经历](http://blog.bihe0832.com/android-v2-issue.html)