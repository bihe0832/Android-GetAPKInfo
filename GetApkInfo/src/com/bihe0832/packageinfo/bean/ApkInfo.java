package com.bihe0832.packageinfo.bean;

public class ApkInfo {

	public String versionCode = "";
	public String versionName = "";
	public String packageName = "";
	public String signature = "";
	public boolean isV1SignatureOK = false;
	public boolean isV2Signature = false;
	public boolean isV2SignatureOK = false;
	public String v2CheckErrorInfo = "";
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("  包名: " + packageName + "\n");
		sb.append("  版本名: " + versionName + "\n");
		sb.append("  版本号: " + versionCode + "\n");
		sb.append("  签名文件MD5: " + signature + "\n");
		sb.append("  V1签名验证通过: " + isV1SignatureOK + "\n");
		sb.append("  使用V2签名: " + isV2Signature + "\n");
		sb.append("  V2签名验证通过: " + isV2SignatureOK + "\n");
		if(!isV1SignatureOK || (isV2Signature && !isV2SignatureOK)){
			sb.append("  签名验证失败原因: " + v2CheckErrorInfo + "\n");
		}
		return sb.toString();
	}
	
}
