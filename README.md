# AndroidGetAPKInfo

## 项目简介

作为基于Android平台的渠道SDK的开发者或者联调同学每天都会面对大量的apk，需要检查他们的包名（packageName）、版本(versionName\versionCode)、应用签名(Signature)等信息，尤其现在Android的V2签名认证(APK Signature Scheme v2)带来的问题更多，为了提高工作效率，整理了一个获取应用基本信息的系列工具集。

## 目录结构

	├── AndroidGetSignature.apk : 一款基于Android Studio开发的通过包名获取apk签名的应用
	│
	├── AndroidGetSignature : AndroidGetSignature.apk对应源码
	│
	├── CheckAndroidV2Signature.jar ：一款基于Java环境的校验APK是否使用Android-V2签名，如果使用V2，V2校验是否通过的工具
	│
	├── CheckAndroidV2Signature ：CheckAndroidV2Signature.jar的源码
	│
	├── getPackageInfo.jar ：一款基于Java环境的读取apk的包名、版本号、签名、是否使用V2签名，V2签名校验是否通过的工具
	│
	├── GetApkInfo ： getPackageInfo.jar的源码
	│
	├── apksig ： Android SDK Build Tools中关于签名相关的工具apksigner的源码（提供了V2签名以及校验的方法）
	│
	└── README.md
	
## 使用方法

这里仅列出主要操作的使用方法，详细内容请查看对应目录的README文件

### AndroidGetSignature.apk 

安装AndroidGetSignature.apk 后输入包名，点击获取

### CheckAndroidV2Signature.jar

	➜  java -jar ./CheckAndroidV2Signature.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	{"ret":0,"msg":"ok","isV2":false,"isV2OK":false}

### getPackageInfo.jar

	➜  java -jar ./getPackageInfo.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	
	执行结果: 成功
	应用信息:
	  包名: com.tencent.tmgp.yybtestsdk
	  版本名: 1.3.1
	  版本号: 1
	  渠道号: null
	  签名: 252e3ded833125ed3e3bb010bc24f4dc
	  使用V2签名: false
	  V2签名验证通过: false	
## 相关文章

以下文章为个人博客中关于上述内容的对应介绍，除部分原理或者背景相关的内容外，大部分内容与github中README一致。

- [一款基于Java环境的读取应用包名、签名、是否V2签名等基本信息的工具](http://blog.bihe0832.com/java-getpackageinfo.html)

- [Android的APK Signature Scheme v2签名及一款基于Java环境的校验工具介绍](http://blog.bihe0832.com/android-v2-signature.html)

- [如何生成混淆过的可执行的jar程序](http://blog.bihe0832.com/runnable-jar.html)
