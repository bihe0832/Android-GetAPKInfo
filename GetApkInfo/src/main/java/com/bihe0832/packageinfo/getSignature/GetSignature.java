package com.bihe0832.packageinfo.getSignature;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class GetSignature {
	
	public GetSignature() {
    }

    public static String getApkSignInfo(String apkFilePath, boolean showException) {
        byte[] readBuffer = new byte[8192];
        Certificate[] certs = null;

        try {
            JarFile e = new JarFile(apkFilePath);
            Enumeration entries = e.entries();

            while(entries.hasMoreElements()) {
                JarEntry je = (JarEntry)entries.nextElement();
                if(!je.isDirectory() && !je.getName().startsWith("META-INF/")) {
                    Certificate[] localCerts = loadCertificates(e, je, readBuffer);
                    boolean found = false;
                    if(certs == null) {
                        certs = localCerts;
                    } else {
                        for(int i = 0; i < certs.length; ++i) {
                            for(int j = 0; j < localCerts.length; ++j) {
                                if(certs[i] != null && certs[i].equals(localCerts[j])) {
                                    found = true;
                                    break;
                                }
                            }

                            if(certs.length != localCerts.length) {
                                e.close();
                                return null;
                            }
                        }
                    }

                    if(found) {
                        break;
                    }
                }
            }

            e.close();
            return getSignValidString(certs[0].getEncoded());
        } catch (Exception var10) {
            if (showException){
                var10.printStackTrace();
            }
            return "get signInfo failed, please use --debug get more info";
        }
    }

    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            InputStream e = jarFile.getInputStream(je);

            while(e.read(readBuffer, 0, readBuffer.length) != -1) {
                ;
            }

            e.close();
            return je != null ? je.getCertificates():null;
        } catch (Exception var4) {
            var4.printStackTrace();
            System.err.println("Exception reading " + je.getName() + " in " + jarFile.getName() + ": " + var4);
            return null;
        }
    }

    public static String toHexString(byte[] keyData) {
        if(keyData == null) {
            return null;
        } else {
            int expectedStringLen = keyData.length * 2;
            StringBuilder sb = new StringBuilder(expectedStringLen);

            for(int i = 0; i < keyData.length; ++i) {
                String hexStr = Integer.toString(keyData[i] & 255, 16);
                if(hexStr.length() == 1) {
                    hexStr = "0" + hexStr;
                }

                sb.append(hexStr);
            }

            return sb.toString();
        }
    }

    private static String getSignValidString(byte[] sign) throws NoSuchAlgorithmException {
        MessageDigest alga = null;
        alga = MessageDigest.getInstance("MD5");
        alga.update(sign);
        return toHexString(alga.digest());
    }
}