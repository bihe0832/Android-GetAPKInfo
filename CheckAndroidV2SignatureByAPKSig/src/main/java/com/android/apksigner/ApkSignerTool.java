/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.apksigner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

import com.android.apksig.ApkVerifier;

/**
 * Command-line tool for signing APKs and for checking whether an APK's signature are expected to
 * verify on Android devices.
 */
public class ApkSignerTool {

    private static final int VERSION_CODE = 5;
	private static final String VERSION_NAME = "1.1.1";
	private static final String HELP_PAGE_GENERAL = "help.txt";
	private static final String VERSION_PAGE_GENERAL = "help_version.txt";
	
	
	public static final String KEY_RESULT_RET = "ret";
	public static final String KEY_RESULT_MSG = "msg";
	public static final String KEY_RESULT_IS_V2 = "isV2";
	public static final String KEY_RESULT_IS_V2_OK = "isV2OK";
	public static final String KEY_RESULT_IS_V1_OK = "isV1OK";
	public static final String KEY_RESULT_KEYSTORE_MD5 = "keystoreMd5";
	//成功
	private static final int RET_OK = 0;
	//文件类型错误
	private static final int RET_FILE_NOT_GOOD = -2;
	//获取签名异常
	private static final int RET_GET_SIG_BAD = -3;
	private static boolean sShowDebug = false;

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
    		System.out.println("com.tencent.ysdk.CheckAndroidV2Signature version " + VERSION_NAME + " (CheckAndroidV2Signature - " + VERSION_CODE + ")\n");
    		printUsage(VERSION_PAGE_GENERAL);
            return;
        } else if(params[0].toLowerCase().endsWith(".apk")){
        	System.out.println(verify(params[0],sShowDebug));
            return;
        }else{
			System.out.println(getFailedCheckResult(RET_FILE_NOT_GOOD, params[0] +"is not an android apk file"));
			return;
		}
        
    }

	private static String getBothSuccssedCheckResult(int ret,String Msg, boolean isV1OK, boolean isV2, boolean isV2Ok,String keystoreMD5){
		return "{" +
					"\""+ KEY_RESULT_RET +"\":" + ret + 
					",\""+ KEY_RESULT_MSG +"\":\"" + Msg + 
					"\",\""+ KEY_RESULT_IS_V1_OK +"\":" + isV1OK + 
					",\""+ KEY_RESULT_IS_V2 +"\":" + isV2 + 
					",\""+ KEY_RESULT_IS_V2_OK +"\":" + isV2Ok + 
					",\""+ KEY_RESULT_KEYSTORE_MD5 +"\":\"" + keystoreMD5 + 
					"\""+ 
				"}"; 
	}
	
	private static String getFailedCheckResult(int ret,String Msg){
		return "{\""+ KEY_RESULT_RET +"\":" + ret + ",\""+ KEY_RESULT_MSG +"\":\"" + Msg + "\"}"; 
	}
	
    public static String verify(String apkPath, boolean showException){
        File inputApk = new File(apkPath);

        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(inputApk);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = null;
        String msg = "";
        String keystoreMD5 = "";
        try {
            result = apkVerifier.verify();
            List<X509Certificate> signerCerts = result.getSignerCertificates();
            
            int signerNumber = 0;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (X509Certificate signerCert : signerCerts) {
                signerNumber++;
                byte[] encodedCert = signerCert.getEncoded();
                keystoreMD5 = HexEncoding.encode(md5.digest(encodedCert));
            }
        } catch (Exception e) {
        	if(showException){
        		e.printStackTrace();
			}
			msg = e.getMessage();
		}

        if(null != result){
			for (ApkVerifier.IssueWithParams error : result.getErrors()) {
				 System.err.println("ERROR: " + error);
				 msg = msg + error.toString();
			}
			for (ApkVerifier.Result.V1SchemeSignerInfo signer : result.getV1SchemeSigners()) {
			    String signerName = signer.getName();
			    for (ApkVerifier.IssueWithParams error : signer.getErrors()) {
			    	msg = msg + "ERROR: JAR signer " + signerName + ": " + error;
			    }
			}
			for (ApkVerifier.Result.V2SchemeSignerInfo signer : result.getV2SchemeSigners()) {
			    String signerName = "signer #" + (signer.getIndex() + 1);
			    for (ApkVerifier.IssueWithParams error : signer.getErrors()) {
			    	msg = msg + "ERROR: APK Signature Scheme v2 " + signerName + ": " + error;
			    }
			}
	    	if(result.isVerified()){
	    		return getBothSuccssedCheckResult(RET_OK, msg, 
	    				result.isVerifiedUsingV1Scheme(), 
	    				result.isVerifiedUsingV2Scheme(), 
	    				result.isVerifiedUsingV2Scheme(),
	    				keystoreMD5);
	    	}else{
	    		return getBothSuccssedCheckResult(RET_GET_SIG_BAD, msg, 
	    				result.isVerifiedUsingV1Scheme(), 
	    				result.isVerifiedUsingV2Scheme(), 
	    				result.isVerifiedUsingV2Scheme(),
	    				keystoreMD5);
	    	}
        }else{
        	return getFailedCheckResult(RET_GET_SIG_BAD,msg);
        }
    }

    private static void printUsage(String page) {
        try (BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                ApkSignerTool.class.getResourceAsStream(page),
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
