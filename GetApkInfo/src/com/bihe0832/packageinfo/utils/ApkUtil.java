package com.bihe0832.packageinfo.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import com.bihe0832.packageinfo.bean.ApkInfo;

public class ApkUtil {
	
	private static final Namespace NS = Namespace.getNamespace("http://schemas.android.com/apk/res/android");
	
	public static void getApkInfo(String apkPath, ApkInfo info){
		SAXBuilder builder = new SAXBuilder();
		Document document = null;
		try{
			document = builder.build(getXmlInputStream(apkPath));
		}catch (Exception e) {
			e.printStackTrace();
		}
		Element root = document.getRootElement();
		info.versionCode = root.getAttributeValue("versionCode",NS);
		info.versionName = root.getAttributeValue("versionName", NS);
		String s = root.getAttributes().toString();
		String c[] = s.split(",");
		for(String a: c){
			if(a.contains("package")){
				info.packageName = a.substring(a.indexOf("package=\"")+9, a.lastIndexOf("\""));
			}
		}
	}

	private static InputStream getXmlInputStream(String apkPath) {
		InputStream inputStream = null;
		InputStream xmlInputStream = null;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(apkPath);
			ZipEntry zipEntry = new ZipEntry("AndroidManifest.xml");
			inputStream = zipFile.getInputStream(zipEntry);
			AXMLPrinter xmlPrinter = new AXMLPrinter();
			xmlPrinter.startPrinf(inputStream);
			xmlInputStream = new ByteArrayInputStream(xmlPrinter.getBuf().toString().getBytes("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
			try {
				inputStream.close();
				zipFile.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return xmlInputStream;
	}

}
