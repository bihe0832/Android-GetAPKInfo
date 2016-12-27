# AndroidGetAPKInfo

## 项目简介

作为基于Android平台的渠道SDK的开发者或者联调同学每天都会面对大量的apk，需要检查他们的包名、版本、应用签名等信息，尤其现在Android-V2Sginature带来的问题更多，为了提高工作效率，整理了一个获取应用基本信息的系列工具集。

## 目录结构

	├── AndroidGetSignature : 一款基于Android Studio开发的通过包名获取apk签名的应用
	│
	├── CheckAndroidV2Signature ：一款基于Java环境的校验APK是否使用Android-V2签名，如果使用V2，V2校验是否通过的工具
	│
	├── GetApkInfo ： 一款基于Java环境的获取应用基本信息（包名、版本号、签名、是否V2，V2是否通过）的工具
	│
	└── README.md
	
## 其他

### 相关文章

以下文章为个人博客中关于上述内容的对应介绍，除部分原理或者背景相关的内容外，大部分内容与github中README一致。

- [一款基于Java环境的读取应用包名、签名、是否V2签名等基本信息的工具](http://blog.bihe0832.com/java-getpackageinfo.html)

- [Android的APK Signature Scheme v2签名及一款基于Java环境的校验工具介绍](http://blog.bihe0832.com/android-v2-signature.html)

- []()