package com.bihe0832.checksignature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;

import com.bihe0832.checksignature.ApkSignatureSchemeV2Verifier.SignatureNotFoundException;


/**
 * 
 * @author bihe0832 blog.bihe0832.com
 * @version 1
 * @version 1.0.0
 *
 */
public class CheckAndroidSignature {
	
	private static final int VERSION_CODE = 2;
	private static final String VERSION_NAME = "1.0.1";
	
	//成功
	private static final int RET_OK = 0;
	//文件路径错误
	private static final int RET_FILE_NOT_FOUND = -1;
	//文件类型错误
	private static final int RET_FILE_NOT_GOOD = -2;
	//获取签名异常
	private static final int RET_GET_SIG_BAD = -3;

	
	public static void main(String args[]) { 
		if(args.length > 0){
			String para = args[0];
			if(null != para && para.length() > 0){
				if(para.toLowerCase().startsWith("--help")){
					showHelp();
				}else if(para.toLowerCase().startsWith("--version")){
					showVersion();
				}else if(para.toLowerCase().endsWith(".apk")){
					checkSig(para);
				}else{
					showFailedCheckResult(RET_FILE_NOT_GOOD, para +"is not an android apk file");
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
		sb.append("\nusage: java -jar ./CheckAndroidV2Signature.jar [--version] [--help] [filePath]\n\n");
		sb.append("such as:\n\n");
		sb.append("\t java -jar ./CheckAndroidV2Signature.jar --version \n");
		sb.append("\t java -jar ./CheckAndroidV2Signature.jar --help \n");
		sb.append("\t java -jar ./CheckAndroidV2Signature.jar ./test.apk \n\n");
		sb.append("after check,the result will be a string json such as:\n\n");
		sb.append("\t {\"ret\":0,\"msg\":\"ok\",\"isV2\":true,\"isV2OK\":true} \n\n");
		sb.append("\t ret: result code for check\n\n");
		sb.append("\t\t 0 : command exec succ \n");
		sb.append("\t\t -1 : file not found\n");
		sb.append("\t\t -2 : file not an Android APK file\n");
		sb.append("\t\t -3 : check File signature error ,retry again \n\n");
		sb.append("\t msg: result msg for check\n");
		sb.append("\t isV2: whether the file is use Android-V2 signature or not\n");
		sb.append("\t isV2OK: whether the file's Android-V2 signature is ok or not\n");
		System.out.println(sb.toString());
	}
	
	private static void showVersion(){
		StringBuilder sb = new StringBuilder("com.tencent.ysdk.CheckAndroidV2Signature version " + VERSION_NAME + " (CheckAndroidV2Signature - " + VERSION_CODE + ")\n");
		sb.append("homepage : https://github.com/bihe0832/AndroidGetAPKInfo \n");
		sb.append("blog : http://blog.bihe0832.com \n");
		sb.append("github : https://github.com/bihe0832 \n");
		System.out.println(sb.toString());
	}

	private static void checkSig(String filePath){
		try {
			boolean isV2 = ApkSignatureSchemeV2Verifier.hasSignature(filePath);
			if(isV2){
				X509Certificate[][] isV2OK = ApkSignatureSchemeV2Verifier.verify(filePath);
				if(isV2OK.length > 0){
					showSuccssedCheckResult(RET_OK,"ok",true,true);
				}else{
					showSuccssedCheckResult(RET_OK,"ok",true,false);
				}
			}else{
				showSuccssedCheckResult(RET_OK,"ok",false,false);
			}
		}catch (FileNotFoundException e) {
			showFailedCheckResult(RET_FILE_NOT_FOUND,"get signature failed, File:"+ filePath +" Not Found");
		}catch (IOException e) {
			showFailedCheckResult(RET_GET_SIG_BAD,"get signature failed, throw an IOException");
		}catch (SignatureNotFoundException e) {
			showSuccssedCheckResult(RET_OK,"the APK is not signed using APK Signature Scheme v2",true,false);
		}catch (SecurityException e) {
			showSuccssedCheckResult(RET_OK,"get signature failed, throw an SecurityException",true,false);
		}
	}
	
	private static void showSuccssedCheckResult(int ret,String Msg, boolean isV2, boolean isV2Ok){
		System.out.println("{\"ret\":" + ret + ",\"msg\":\"" + Msg + "\",\"isV2\":" + isV2 + ",\"isV2OK\":" + isV2Ok + "}"); 
	}
	
	private static void showFailedCheckResult(int ret,String Msg){
		System.out.println("{\"ret\":" + ret + ",\"msg\":\"" + Msg + "\"}"); 
	}
}
