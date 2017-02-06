package com.bihe0832.packageinfo.bean;

public class ApkInfo {

	public String versionCode = "";
	public String versionName = "";
	public String packageName = "";
	public String signature = "";
	public String v2Signature = "";
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("  包名: " + packageName + "\n");
		sb.append("  版本名: " + versionName + "\n");
		sb.append("  版本号: " + versionCode + "\n");
		sb.append("  签名: " + signature + "\n");
		sb.append("  V2签名验证结果: " + v2Signature + "\n");
		return sb.toString();
	}
	
}
