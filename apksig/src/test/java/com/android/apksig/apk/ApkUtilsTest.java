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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
}
