package com.bihe0832.checksignature;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.cert.CertificateEncodingException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ApkSignatureGetSig {

	private static final Object mSync = new Object();
	private static WeakReference<byte[]> mReadBuffer;

	public static void main(String[] args) {
		// if (args.length < 1) {
		// System.out.println("Usage: java -jar GetAndroidSig.jar <apk/jar>");
		// System.exit(-1);
		// }
		//
		// System.out.println(args[0]);

		// To char:
		// 3082023f308201a8a00302010202044c984ccc300d06092a864886f70d01010505003064310b300906035504061302434e3110300e060355040813074a69616e6773753110300e060355040713074e616e6a696e6731153013060355040a130c456d6167736f667477617265310d300b060355040b13045469616e310b3009060355040313024c75301e170d3130303932313036313232385a170d3338303230363036313232385a3064310b300906035504061302434e3110300e060355040813074a69616e6773753110300e060355040713074e616e6a696e6731153013060355040a130c456d6167736f667477617265310d300b060355040b13045469616e310b3009060355040313024c7530819f300d06092a864886f70d010101050003818d0030818902818100835c192e7385ff63ab7bc8469df0224caac1eeea054e6a9bca9d7f3915db090b2bc3cde0f587da732fe45ce55dba30fe3cda5dfbb9797d7b05d59794916d61d5678b3a40722eb09ede89f1e4135a289a8a8464de19d6aab2f2bd8a702e6f53107ef51f25985bdca1a8572eed13827aaf96f8fcfaefe00d31881058134964fd970203010001300d06092a864886f70d01010505000381810072881563e0b07637bf03a6862e3dd9e7dd7186a3355639937748a686119ad59a612a95c6eb8b87b05d0353fc69eefe1b195eafaa08c08f1bf4d20659821ed67fd93d387912af03589d42551affbb6bdfdf81c4e702b32df611a9fcc8ad309edc02d694c948690258245e429bfd0049fd65e284c35d86e046a8abb0a4ee218eff

		String mArchiveSourcePath = args[0];

		WeakReference<byte[]> readBufferRef;
		byte[] readBuffer = null;
		synchronized (mSync) {
			readBufferRef = mReadBuffer;
			if (readBufferRef != null) {
				mReadBuffer = null;
				readBuffer = readBufferRef.get();
			}
			if (readBuffer == null) {
				readBuffer = new byte[8192];
				readBufferRef = new WeakReference<byte[]>(readBuffer);
			}
		}

		try {
			JarFile jarFile = new JarFile(mArchiveSourcePath);
			java.security.cert.Certificate[] certs = null;

			Enumeration entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry je = (JarEntry) entries.nextElement();
				if (je.isDirectory()) {
					continue;
				}
				if (je.getName().startsWith("META-INF/")) {
					continue;
				}
				java.security.cert.Certificate[] localCerts = loadCertificates(
						jarFile, je, readBuffer);
				if (true) {
					System.out.println("File " + mArchiveSourcePath + " entry "
							+ je.getName() + ": certs=" + certs + " ("
							+ (certs != null ? certs.length : 0) + ")");
				}
				if (localCerts == null) {
					System.err.println("Package has no certificates at entry "
							+ je.getName() + "; ignoring!");
					jarFile.close();
					return;
				} else if (certs == null) {
					certs = localCerts;
				} else {
					// Ensure all certificates match.
					for (int i = 0; i < certs.length; i++) {
						boolean found = false;
						for (int j = 0; j < localCerts.length; j++) {
							if (certs[i] != null
									&& certs[i].equals(localCerts[j])) {
								found = true;
								break;
							}
						}
						if (!found || certs.length != localCerts.length) {
							System.err
									.println("Package has mismatched certificates at entry "
											+ je.getName() + "; ignoring!");
							jarFile.close();
							return; // false
						}
					}
				}
			}

			jarFile.close();

			synchronized (mSync) {
				mReadBuffer = readBufferRef;
			}

			if (certs != null && certs.length > 0) {
				final int N = certs.length;

				for (int i = 0; i < N; i++) {
					String charSig = new String(toChars(certs[i].getEncoded()));
					System.out.println("Cert#: " + i + "  Type:"
							+ certs[i].getType() + "\nPublic key: "
							+ certs[i].getPublicKey() + "\nHash code: "
							+ certs[i].hashCode() + " / 0x"
							+ Integer.toHexString(certs[i].hashCode())
							+ "\nTo char: " + charSig);
				}
			} else {
				System.err.println("Package has no certificates; ignoring!");
				return;
			}
		} catch (CertificateEncodingException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			System.err.println("Exception reading " + mArchiveSourcePath + "\n"
					+ e);
			return;
		} catch (RuntimeException e) {
			System.err.println("Exception reading " + mArchiveSourcePath + "\n"
					+ e);
			return;
		}
	}

	private static char[] toChars(byte[] mSignature) {
		byte[] sig = mSignature;
		final int N = sig.length;
		final int N2 = N * 2;
		char[] text = new char[N2];

		for (int j = 0; j < N; j++) {
			byte v = sig[j];
			int d = (v >> 4) & 0xf;
			text[j * 2] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
			d = v & 0xf;
			text[j * 2 + 1] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
		}

		return text;
	}

	private static java.security.cert.Certificate[] loadCertificates(
			JarFile jarFile, JarEntry je, byte[] readBuffer) {
		try {
			// We must read the stream for the JarEntry to retrieve
			// its certificates.
			InputStream is = jarFile.getInputStream(je);
			while (is.read(readBuffer, 0, readBuffer.length) != -1) {
				// not using
			}
			is.close();

			return (java.security.cert.Certificate[]) (je != null ? je
					.getCertificates() : null);
		} catch (IOException e) {
			System.err.println("Exception reading " + je.getName() + " in "
					+ jarFile.getName() + ": " + e);
		}
		return null;
	}
}
