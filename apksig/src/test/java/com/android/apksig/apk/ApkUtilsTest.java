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

package com.android.apksig.apk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.android.apksig.ApkSigner;
import com.android.apksig.internal.util.HexEncoding;
import com.android.apksig.internal.util.Resources;
import com.android.apksig.util.DataSources;

@RunWith(JUnit4.class)
public class ApkUtilsTest {

    @Test
    public void testGetMinSdkVersionForValidCodename() throws Exception {
        assertEquals(1, ApkUtils.getMinSdkVersionForCodename("AAAA"));
        assertEquals(2, ApkUtils.getMinSdkVersionForCodename("CUPCAKE"));
        assertEquals(7, ApkUtils.getMinSdkVersionForCodename("FROYO"));
        assertEquals(23, ApkUtils.getMinSdkVersionForCodename("N"));
        assertEquals(23, ApkUtils.getMinSdkVersionForCodename("NMR1"));
        assertEquals(25, ApkUtils.getMinSdkVersionForCodename("OMG"));
        // Speculative: Q should be 27 or higher (not yet known at the time of writing)
        assertEquals(27, ApkUtils.getMinSdkVersionForCodename("QQQ"));
    }

    @Test(expected = CodenameMinSdkVersionException.class)
    public void testGetMinSdkVersionForEmptyCodename() throws Exception {
        ApkUtils.getMinSdkVersionForCodename("");
    }

    @Test(expected = CodenameMinSdkVersionException.class)
    public void testGetMinSdkVersionForUnexpectedCodename() throws Exception {
        ApkUtils.getMinSdkVersionForCodename("1ABC");
    }

    @Test
    public void testGetMinSdkVersionFromBinaryAndroidManifest() throws Exception {
        ByteBuffer manifest = getAndroidManifest("original.apk");
        assertEquals(23, ApkUtils.getMinSdkVersionFromBinaryAndroidManifest(manifest));
    }

    @Test
    public void testGetDebuggableFromBinaryAndroidManifest() throws Exception {
        ByteBuffer manifest = getAndroidManifest("original.apk");
        assertFalse(ApkUtils.getDebuggableFromBinaryAndroidManifest(manifest));

        manifest = getAndroidManifest("debuggable-boolean.apk");
        assertTrue(ApkUtils.getDebuggableFromBinaryAndroidManifest(manifest));

        // android:debuggable value is a resource reference -- this must be rejected
        manifest = getAndroidManifest("debuggable-resource.apk");
        try {
            ApkUtils.getDebuggableFromBinaryAndroidManifest(manifest);
            fail();
        } catch (ApkFormatException expected) {}
    }

    @Test
    public void testGetPackageNameFromBinaryAndroidManifest() throws Exception {
        ByteBuffer manifest = getAndroidManifest("original.apk");
        assertEquals(
                "android.appsecurity.cts.tinyapp",
                ApkUtils.getPackageNameFromBinaryAndroidManifest(manifest));
    }

    @Test
    public void testGetAndroidManifest() throws Exception {
        ByteBuffer manifest = getAndroidManifest("original.apk");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(manifest);
        byte[] actualDigest = md.digest();
        assertEquals(
                "8b3de63a282652221162cdc327f424924ac3c7c24e642035975a1ee7a395c4dc",
                HexEncoding.encode(actualDigest));
    }

    private static ByteBuffer getAndroidManifest(String apkResourceName)
            throws IOException, ApkFormatException {
        return getAndroidManifest(getResource(apkResourceName));
    }

    private static ByteBuffer getAndroidManifest(byte[] apk)
            throws IOException, ApkFormatException {
        return ApkUtils.getAndroidManifest(DataSources.asDataSource(ByteBuffer.wrap(apk)));
    }

    private static byte[] getResource(String resourceName) throws IOException {
        return Resources.toByteArray(ApkSigner.class, resourceName);
    }
}
