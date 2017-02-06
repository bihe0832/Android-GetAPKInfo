package com.bihe0832.packageinfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;

import org.json.JSONObject;

import jdk.nashorn.api.scripting.JSObject;

import com.bihe0832.checksignature.CheckAndroidSignature;
import com.bihe0832.checksignature.ApkSignatureSchemeV2Verifier.SignatureNotFoundException;
import com.bihe0832.packageinfo.bean.ApkInfo;
import com.bihe0832.packageinfo.getSignature.GetSignature;
import com.bihe0832.packageinfo.utils.ApkUtil;


public class Main {
	private static final int VERSION_CODE = 3;
	private static final String VERSION_NAME = "1.0.2";
	
	//文件路径错误
	private static final int RET_FILE_NOT_FOUND = -1;
	//文件类型错误
	private static final int RET_FILE_NOT_GOOD = -2;
	//获取系统异常
	private static final int RET_GET_INFO_BAD = -3;
	
	public static void main(String args[]) { 
		if(args.length > 0){
			if(null != args[0] && args[0].length() > 0){
				if(args[0].toLowerCase().startsWith("--help")){
					showHelp();
				}else if(args[0].toLowerCase().startsWith("--version")){
					showVersion();
				}else if(args[0].toLowerCase().endsWith(".apk")){
					getApkInfo(args[0]);
				}else{
					showFailedCheckResult(RET_FILE_NOT_GOOD, args[0] +"is not an android apk file");
				}
			}else{
				showHelp();
			}
		}else{
			showHelp();
		}
    } 
	
	private static void showHelp(){
		StringBuilder sb = new StringBuilder();
		sb.append("\n usage: java -jar ./getPackageInfo.jar [--version] [--help] [filePath]\n\n");
		sb.append("such as:\n\n");
		sb.append("\t java -jar ./getPackageInfo.jar --version \n");
		sb.append("\t java -jar ./getPackageInfo.jar --help \n");
		sb.append("\t java -jar ./getPackageInfo.jar ./test.apk \n\n");
		System.out.println(sb.toString());
	}
	
	private static void showVersion(){
		StringBuilder sb = new StringBuilder("com.bihe0832.getPackageInfo version " + VERSION_NAME + " (getPackageInfo - " + VERSION_CODE + ")\n");
		sb.append("homepage : https://github.com/bihe0832/AndroidGetAPKInfo \n");
		sb.append("blog : http://blog.bihe0832.com \n");
		sb.append("github : https://github.com/bihe0832 \n");
		System.out.println(sb.toString());
	}

	private static void getApkInfo(String filePath){
		ApkInfo info = new ApkInfo();
		try {
			ApkUtil.getApkInfo(filePath, info);
		} catch(Exception e){
			showFailedCheckResult(RET_GET_INFO_BAD,"get channel and apkinfo failed, throw an Exception");
			return;
		}
		String v2Signature = CheckAndroidSignature.checkSig(filePath);
		
		try{
			JSONObject jsonobject = new JSONObject(v2Signature);  
			info.isV2Signature = jsonobject.getBoolean(CheckAndroidSignature.KEY_RESULT_IS_V2);
			info.isV2SignatureOK = jsonobject.getBoolean(CheckAndroidSignature.KEY_RESULT_IS_V2_OK);
			info.v2CheckErrorInfo = jsonobject.getString(CheckAndroidSignature.KEY_RESULT_MSG);
			info.signature = GetSignature.getApkSignInfo(filePath);
		}catch(Exception e){
			showFailedCheckResult(RET_GET_INFO_BAD,"get signature failed, throw an Exception");
			return;
		}
		showSuccssedCheckResult(info);
	}
	
	private static void showSuccssedCheckResult(ApkInfo info){
		System.out.println("执行结果: 成功"); 
		System.out.println("应用信息: \n" + info.toString());
	}
	
	private static void showFailedCheckResult(int ret,String Msg){
		System.out.println("执行结果: 失败("+ ret+")"); 
		System.out.println("错误信息:" + Msg);
	}

}
