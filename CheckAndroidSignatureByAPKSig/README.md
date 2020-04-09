## 简介

基于官方签名相关的工具apksigner的源码改造的安卓签名校验工具
     
## 使用事例

#### 查看帮助

	➜  java -jar ./CheckAndroidSignature.jar
	usage:
	
		java -jar ./CheckAndroidSignature.jar <command> [filePath]
		java -jar ./CheckAndroidSignature.jar --version
		java -jar ./CheckAndroidSignature.jar --help
	
	such as:
	
	
		java -jar ./CheckAndroidSignature.jar ./test.apk
		java -jar ./CheckAndroidSignature.jar --version
		java -jar ./CheckAndroidSignature.jar --help
	
	after check,the result will be a string json such as:
	
	{"ret":0,"msg":"","isV1OK":true,"isV2":false,"isV2OK":false,"keystoreMd5":"252e3ded833125ed3e3bb010bc24f4dc"}
	
		 ret: result code for check
	
			  0 : check File signature success
			 -1 : file not found
			 -2 : file not an Android APK file
			 -3 : check File signature error ,retry again
	
		 msg: result msg for check
		 isV1OK: whether the file's signature is ok or not
		 isV2: whether the file is use Android-V2 signature or not
		 isV2OK: whether the file's Android-V2 signature is ok or not
			
			
#### 查看版本

	➜  java -jar ./CheckAndroidSignature.jar --version

	com.bihe0832.CheckAndroidSignature version 2.0 (CheckAndroidSignature - 6)
	
	homepage : https://github.com/bihe0832/AndroidGetAPKInfo
	blog : http://blog.bihe0832.com
	github : https://github.com/bihe0832
		
#### 查看应用信息

	➜  java -jar ./CheckAndroidSignature.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	{"ret":0,"msg":"","isV1OK":false,"isV2":true,"isV2OK":true,"isV3":true,"isV3OK":true,"keystoreMd5":"80fa5a8552e418f6bd805c65bcddf4c8"}

