/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.apksig.internal.test;

import static org.junit.Assert.assertArrayEquals;

import java.nio.ByteBuffer;

public abstract class MoreAsserts {
    private MoreAsserts() {}

    /**
     * Asserts that the contents of the provided {@code ByteBuffer} are as expected. This method
     * does not change the position or the limit of the provided buffer.
     */
    public static void assertByteBufferEquals(byte[] expected, ByteBuffer actual) {
        assertByteBufferEquals(null, expected, actual);
    }

    /**
     * Asserts that the contents of the provided {@code ByteBuffer} are as expected. This method
     * does not change the position or the limit of the provided buffer.
     */
    public static void assertByteBufferEquals(String message, byte[] expected, ByteBuffer actual) {
        byte[] actualArr;
        if ((actual.hasArray())
                && (actual.arrayOffset() == 0) && (actual.array().length == actual.remaining())) {
            actualArr = actual.array();
        } else {
            actualArr = new byte[actual.remaining()];
            int actualOriginalPos = actual.position();
            actual.get(actualArr);
            actual.position(actualOriginalPos);
        }
        assertArrayEquals(message, expected, actualArr);
    }
}
