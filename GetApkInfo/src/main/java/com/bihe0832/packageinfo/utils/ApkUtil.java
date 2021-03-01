package com.bihe0832.packageinfo.utils;

import com.bihe0832.packageinfo.bean.ApkInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;


public class ApkUtil {

    private static final Namespace NS = Namespace.getNamespace("http://schemas.android.com/apk/res/android");

    public static void updateAPKInfo(String apkPath, ApkInfo info, boolean showException) {
        SAXBuilder builder = new SAXBuilder();
        Document document = null;
        InputStream stream = null;
        try {
            stream = new ByteArrayInputStream(
                    AXMLPrinter.getManifestXMLFromAPK(apkPath).getBytes(StandardCharsets.UTF_8));
            document = builder.build(stream);
            Element root = document.getRootElement();
            info.versionCode = root.getAttributeValue("versionCode", NS);
            info.versionName = root.getAttributeValue("versionName", NS);
            String s = root.getAttributes().toString();
            String c[] = s.split(",");
            for (String a : c) {
                if (a.contains("package")) {
                    info.packageName = a.substring(a.indexOf("package=\"") + 9, a.lastIndexOf("\""));
                }
            }

            List booklist = root.getChildren("uses-sdk");
            Element book = (Element) booklist.get(0);
            info.minSdkVersion = book.getAttributeValue("minSdkVersion", NS);
            info.targetSdkVersion = book.getAttributeValue("targetSdkVersion", NS);

            booklist = root.getChildren("uses-permission");
            for (Iterator iter = booklist.iterator(); iter.hasNext(); ) {
                Element tempBook = (Element) iter.next();
                info.permissions.add(tempBook.getAttributeValue("name", NS));
            }
        } catch (Exception e) {
            if (showException) {
                e.printStackTrace();
            }
        } finally {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
