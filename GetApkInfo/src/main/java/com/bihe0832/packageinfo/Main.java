package com.bihe0832.packageinfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import com.android.apksigner.ApkSignerTool;
import com.bihe0832.packageinfo.bean.ApkInfo;
import com.bihe0832.packageinfo.getSignature.GetSignature;
import com.bihe0832.packageinfo.utils.ApkUtil;


public class Main {

	private static final int VERSION_CODE = 6;
	private static final String VERSION_NAME = "1.1.3";
	private static final String HELP_PAGE_GENERAL = "help.txt";
	private static final String VERSION_PAGE_GENERAL = "help_version.txt";
	private static boolean sShowDebug = false;
	private static final int RET_FILE_NOT_GOOD = -2;
	private static final int RET_GET_INFO_BAD = -3;
	public static void main(String[] params) throws Exception {
        if ((params.length == 0)) {
            printUsage(HELP_PAGE_GENERAL);
            return;
        }

		if (params[params.length - 1].toLowerCase().startsWith("--debug")) {
			sShowDebug = true;
		}

		if (params[0].toLowerCase().startsWith("--help")) {
            printUsage(HELP_PAGE_GENERAL);
            return;
        } else if (params[0].toLowerCase().startsWith("--version")) {
    		System.out.println(Main.class.toString() + " version " + VERSION_NAME + " (CheckAndroidV2Signature - " + VERSION_CODE + ")\n");
    		printUsage(VERSION_PAGE_GENERAL);
            return;
        } else if(params[0].toLowerCase().endsWith(".apk")){
        	getApkInfo(params[0]);
            return;
        }else{
			showFailedCheckResult(RET_FILE_NOT_GOOD, params[0] +"is not an android apk file");
			return;
		}
        
    } 
	
	private static void getApkInfo(String filePath){
		ApkInfo info = new ApkInfo();
		try {
			ApkUtil.getApkInfo(filePath, info, sShowDebug);
		} catch(Exception e){
			showFailedCheckResult(RET_GET_INFO_BAD,"get apkinfo failed, throw an Exception ;please use --debug get more info");
			return;
		}
		String v2Signature = ApkSignerTool.verify(filePath, sShowDebug);
		try{
			JSONObject jsonobject = new JSONObject(v2Signature);
			info.isV1SignatureOK = jsonobject.getBoolean(ApkSignerTool.KEY_RESULT_IS_V1_OK);
			info.isV2Signature = jsonobject.getBoolean(ApkSignerTool.KEY_RESULT_IS_V2);
			info.isV2SignatureOK = jsonobject.getBoolean(ApkSignerTool.KEY_RESULT_IS_V2_OK);
			info.v2CheckErrorInfo = jsonobject.getString(ApkSignerTool.KEY_RESULT_MSG);
			info.signature = GetSignature.getApkSignInfo(filePath, sShowDebug);
			info.v2CheckErrorInfo = jsonobject.getString(ApkSignerTool.KEY_RESULT_MSG);
		}catch(Exception e){
			showFailedCheckResult(RET_GET_INFO_BAD,"get apk info failed, throw an Exception;please use --debug get more info");
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

	private static void printUsage(String page) {
        try (BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                Main.class.getResourceAsStream(page),
                                StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + page + " resource");
        }
    }

    /**
     * Indicates that there is an issue with command-line parameters provided to this tool.
     */
    private static class ParameterException extends Exception {
        private static final long serialVersionUID = 1L;

        ParameterException(String message) {
            super(message);
        }
    }
}
