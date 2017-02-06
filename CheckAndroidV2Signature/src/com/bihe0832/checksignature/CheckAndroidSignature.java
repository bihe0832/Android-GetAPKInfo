package com.bihe0832.checksignature;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.bihe0832.checksignature.ApkSignatureSchemeV2Verifier.SignatureNotFoundException;

/**
 * 
 * @author bihe0832 blog.bihe0832.com
 * @version 1
 * @version 1.0.0
 *
 */
public class CheckAndroidSignature {
	
	private static final int VERSION_CODE = 3;
	private static final String VERSION_NAME = "1.0.2";
	
	public static final String KEY_RESULT_IS_V2 = "isV2";
	public static final String KEY_RESULT_IS_V2_OK = "isV2OK";
	public static final String KEY_RESULT_RET = "ret";
	public static final String KEY_RESULT_MSG = "msg";
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
					System.out.println(checkSig(para));
				}else{
					System.out.println(getFailedCheckResult(RET_FILE_NOT_GOOD, para +"is not an android apk file"));
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

	static final String SF_ATTRIBUTE_NAME_ANDROID_APK_SIGNED_NAME_STR = "X-Android-APK-Signed";
	
	public static String checkSig(String filePath){
		boolean isV2 = true;
		try {
			
			isV2 = ApkSignatureSchemeV2Verifier.hasSignature(filePath);
			if(!isV2){
				int apkSignatureNum = getApkSignSchemeVersion(filePath);
				isV2 = 2 == apkSignatureNum ? true : isV2;
			}
			
			if(isV2){
				X509Certificate[][] isV2OK = ApkSignatureSchemeV2Verifier.verify(filePath);
				if(isV2OK.length > 0){
					return getSuccssedCheckResult(RET_OK,"ok",true,true);
				}else{
					return getSuccssedCheckResult(RET_OK,"ok",true,false);
				}
			}else{
				return getSuccssedCheckResult(RET_OK,"ok",false,false);
			}
		}catch (FileNotFoundException e) {
			return getFailedCheckResult(RET_FILE_NOT_FOUND,"get signature failed, File:"+ filePath +" Not Found");
		}catch (IOException e) {
			return getFailedCheckResult(RET_GET_SIG_BAD,"get signature failed, throw an IOException");
		}catch (SignatureNotFoundException e) {
			if(isV2){
				return getSuccssedCheckResult(RET_GET_SIG_BAD,e.toString(),true,false);
			}else{
				return getSuccssedCheckResult(RET_OK,e.toString(),false,false);
			}
			
		}catch (SecurityException e) {
			return getSuccssedCheckResult(RET_OK,"get signature failed, throw an SecurityException",true,false);
		}
	}
	
	private static String getSuccssedCheckResult(int ret,String Msg, boolean isV2, boolean isV2Ok){
		return "{\""+ KEY_RESULT_RET +"\":" + ret + ",\""+ KEY_RESULT_MSG +"\":\"" + Msg + "\",\""+ KEY_RESULT_IS_V2 +"\":" + isV2 + ",\""+ KEY_RESULT_IS_V2_OK +"\":" + isV2Ok + "}"; 
	}
	
	private static String getFailedCheckResult(int ret,String Msg){
		return "{\""+ KEY_RESULT_RET +"\":" + ret + ",\""+ KEY_RESULT_MSG +"\":\"" + Msg + "\"}"; 
	}
	
	private static int getApkSignSchemeVersion(String apkFilePath) {
	    byte[] readBuffer = new byte[8192];
	    try {
	        JarFile e = new JarFile(apkFilePath);
	        Enumeration entries = e.entries();
	        while(entries.hasMoreElements()) {
	            JarEntry je = (JarEntry)entries.nextElement();
	            if(!je.isDirectory() && je.getName().startsWith("META-INF") && je.getName().endsWith(".SF")) {
	            	//FIX 临时方案：逐行读取，然后检查是不是包含两个key
	            	InputStream jarEntryInputStream = e.getInputStream(je);  
	                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(jarEntryInputStream));  
	                String readLine = null;  
	                while ((readLine = bufferedReader.readLine()) != null) {  
	                	if(readLine.contains(SF_ATTRIBUTE_NAME_ANDROID_APK_SIGNED_NAME_STR)){
	                		if(readLine.contains("2")){
	                			return 2;
	                		}else{
	                			return 1;
	                		}
	                	}
	                } 
	            }
	        }
	        e.close();
	    } catch (Exception var10) {
	        var10.printStackTrace();
	    }
		return 1;
	}
}
