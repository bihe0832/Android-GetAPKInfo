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

package com.android.apksig.internal.asn1.ber;

import static com.android.apksig.internal.test.MoreAsserts.assertByteBufferEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.android.apksig.internal.util.HexEncoding;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BerDataValueTest {
    private static final BerDataValue TEST_VALUE1 =
            new BerDataValue(
                    ByteBuffer.wrap(HexEncoding.decode("aa")),
                    ByteBuffer.wrap(HexEncoding.decode("bb")),
                    BerEncoding.TAG_CLASS_UNIVERSAL,
                    true,
                    BerEncoding.TAG_NUMBER_SEQUENCE);

    private static final BerDataValue TEST_VALUE2 =
            new BerDataValue(
                    ByteBuffer.wrap(HexEncoding.decode("cc")),
                    ByteBuffer.wrap(HexEncoding.decode("dd")),
                    BerEncoding.TAG_CLASS_CONTEXT_SPECIFIC,
                    false,
                    BerEncoding.TAG_NUMBER_OCTET_STRING);

    @Test
    public void testGetTagClass() {
        assertEquals(BerEncoding.TAG_CLASS_UNIVERSAL, TEST_VALUE1.getTagClass());
        assertEquals(BerEncoding.TAG_CLASS_CONTEXT_SPECIFIC, TEST_VALUE2.getTagClass());
    }

    @Test
    public void testIsConstructed() {
        assertTrue(TEST_VALUE1.isConstructed());
        assertFalse(TEST_VALUE2.isConstructed());
    }

    @Test
    public void testGetTagNumber() {
        assertEquals(BerEncoding.TAG_NUMBER_SEQUENCE, TEST_VALUE1.getTagNumber());
        assertEquals(BerEncoding.TAG_NUMBER_OCTET_STRING, TEST_VALUE2.getTagNumber());
    }

    @Test
    public void testGetEncoded() {
        assertByteBufferEquals(HexEncoding.decode("aa"), TEST_VALUE1.getEncoded());
        assertByteBufferEquals(HexEncoding.decode("cc"), TEST_VALUE2.getEncoded());
    }

    @Test
    public void testGetEncodedReturnsSlice() {
        // Assert that changing the position of returned ByteBuffer does not affect ByteBuffers
        // returned in the future
        ByteBuffer encoded = TEST_VALUE1.getEncoded();
        assertByteBufferEquals(HexEncoding.decode("aa"), encoded);
        encoded.position(encoded.limit());
        assertByteBufferEquals(HexEncoding.decode("aa"), TEST_VALUE1.getEncoded());
    }

    @Test
    public void testGetEncodedContents() {
        assertByteBufferEquals(HexEncoding.decode("bb"), TEST_VALUE1.getEncodedContents());
        assertByteBufferEquals(HexEncoding.decode("dd"), TEST_VALUE2.getEncodedContents());
    }

    @Test
    public void testGetEncodedContentsReturnsSlice() {
        // Assert that changing the position of returned ByteBuffer does not affect ByteBuffers
        // returned in the future
        ByteBuffer encoded = TEST_VALUE1.getEncodedContents();
        assertByteBufferEquals(HexEncoding.decode("bb"), encoded);
        encoded.position(encoded.limit());
        assertByteBufferEquals(HexEncoding.decode("bb"), TEST_VALUE1.getEncodedContents());
    }

    @Test
    public void testDataValueReader() throws BerDataValueFormatException {
        BerDataValueReader reader = TEST_VALUE1.dataValueReader();
        assertSame(TEST_VALUE1, reader.readDataValue());
        assertNull(reader.readDataValue());
        assertNull(reader.readDataValue());
    }

    @Test
    public void testContentsReader() throws BerDataValueFormatException {
        BerDataValue dataValue =
                new BerDataValue(
                        ByteBuffer.allocate(0),
                        ByteBuffer.wrap(HexEncoding.decode("300203040500")),
                        BerEncoding.TAG_CLASS_UNIVERSAL,
                        true,
                        BerEncoding.TAG_NUMBER_SEQUENCE);
        BerDataValueReader reader = dataValue.contentsReader();
        assertEquals(ByteBufferBerDataValueReader.class, reader.getClass());
        assertByteBufferEquals(HexEncoding.decode("30020304"), reader.readDataValue().getEncoded());
        assertByteBufferEquals(HexEncoding.decode("0500"), reader.readDataValue().getEncoded());
        assertNull(reader.readDataValue());
    }
}
