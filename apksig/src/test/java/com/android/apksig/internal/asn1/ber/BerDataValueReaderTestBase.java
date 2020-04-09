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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.android.apksig.internal.util.HexEncoding;
import org.junit.Test;

/**
 * Base class for unit tests of ASN.1 BER (see {@code X.690}) data value reader implementations.
 *
 * <p>Subclasses need to provide only an implementation of {@link #createReader(byte[])} and
 * subclass-specific tests.
 */
public abstract class BerDataValueReaderTestBase {

    /**
     * Returns a new reader initialized with the provided input.
     */
    protected abstract BerDataValueReader createReader(byte[] input);

    @Test
    public void testEmptyInput() throws Exception {
        assertNull(readDataValue(""));
    }

    @Test
    public void testEndOfInput() throws Exception {
        BerDataValueReader reader = createReader("3000"); // SEQUENCE with empty contents
        assertNotNull(reader.readDataValue());
        // End of input has been reached
        assertNull(reader.readDataValue());
        // Null should also be returned on consecutive invocations
        assertNull(reader.readDataValue());
    }

    @Test
    public void testSingleByteTagId() throws Exception {
        BerDataValue dataValue = readDataValue("1000");
        assertEquals(BerEncoding.TAG_CLASS_UNIVERSAL, dataValue.getTagClass());
        assertFalse(dataValue.isConstructed());
        assertEquals(0x10, dataValue.getTagNumber());

        dataValue = readDataValue("3900");
        assertEquals(BerEncoding.TAG_CLASS_UNIVERSAL, dataValue.getTagClass());
        assertTrue(dataValue.isConstructed());
        assertEquals(0x19, dataValue.getTagNumber());

        dataValue = readDataValue("6700");
        assertEquals(BerEncoding.TAG_CLASS_APPLICATION, dataValue.getTagClass());
        assertTrue(dataValue.isConstructed());
        assertEquals(7, dataValue.getTagNumber());

        dataValue = readDataValue("8600");
        assertEquals(BerEncoding.TAG_CLASS_CONTEXT_SPECIFIC, dataValue.getTagClass());
        assertFalse(dataValue.isConstructed());
        assertEquals(6, dataValue.getTagNumber());

        dataValue = readDataValue("fe00");
        assertEquals(BerEncoding.TAG_CLASS_PRIVATE, dataValue.getTagClass());
        assertTrue(dataValue.isConstructed());
        assertEquals(0x1e, dataValue.getTagNumber());
    }

    @Test
    public void testHighTagNumber() throws Exception {
        assertEquals(7, readDataValue("3f0700").getTagNumber());
        assertEquals(7, readDataValue("3f800700").getTagNumber());
        assertEquals(7, readDataValue("3f80800700").getTagNumber());
        assertEquals(7, readDataValue("3f8080800700").getTagNumber());
        assertEquals(7, readDataValue("3f808080808080808080808080808080800700").getTagNumber());
        assertEquals(375, readDataValue("3f827700").getTagNumber());
        assertEquals(268435455, readDataValue("3fffffff7f00").getTagNumber());
        assertEquals(Integer.MAX_VALUE, readDataValue("3f87ffffff7f00").getTagNumber());
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testHighTagNumberTooLarge() throws Exception {
        readDataValue("3f888080800000"); // Integer.MAX_VALUE + 1
    }

    // @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedHighTagNumberLastOctetMissing() throws Exception {
        readDataValue("9f80"); // terminating octet must not have the highest bit set
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedBeforeFirstLengthOctet() throws Exception {
        readDataValue("30");
    }

    @Test
    public void testShortFormLength() throws Exception {
        assertByteBufferEquals(new byte[0], readDataValue("3000").getEncodedContents());
        assertByteBufferEquals(
                HexEncoding.decode("010203"), readDataValue("3003010203").getEncodedContents());
    }

    @Test
    public void testLongFormLength() throws Exception {
        assertByteBufferEquals(new byte[0], readDataValue("308100").getEncodedContents());
        assertByteBufferEquals(
                HexEncoding.decode("010203"), readDataValue("30820003010203").getEncodedContents());
        assertEquals(
                255,
                readDataValue(concat(HexEncoding.decode("3081ff"), new byte[255]))
                        .getEncodedContents().remaining());
        assertEquals(
                0x110,
                readDataValue(concat(HexEncoding.decode("30820110"), new byte[0x110]))
                        .getEncodedContents().remaining());
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedLongFormLengthBeforeFirstLengthByte() throws Exception {
        readDataValue("3081");
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedLongFormLengthLastLengthByteMissing() throws Exception {
        readDataValue("308200");
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testLongFormLengthTooLarge() throws Exception {
        readDataValue("3084ffffffff");
    }

    @Test
    public void testIndefiniteFormLength() throws Exception {
        assertByteBufferEquals(new byte[0], readDataValue("30800000").getEncodedContents());
        assertByteBufferEquals(
                HexEncoding.decode("020103"), readDataValue("30800201030000").getEncodedContents());
        assertByteBufferEquals(
                HexEncoding.decode(
                        "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"
                            + "000102030405060708090a0b0c0d0e0f"),
                readDataValue(
                        "0280"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "000102030405060708090a0b0c0d0e0f"
                                + "0000"
                        ).getEncodedContents());
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testDefiniteLengthContentsTruncatedBeforeFirstContentOctet() throws Exception {
        readDataValue("3001");
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testIndefiniteLengthContentsTruncatedBeforeFirstContentOctet() throws Exception {
        readDataValue("3080");
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedDefiniteLengthContents() throws Exception {
        readDataValue("30030102");
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testTruncatedIndefiniteLengthContents() throws Exception {
        readDataValue("308001020300");
    }

    @Test
    public void testEmptyDefiniteLengthContents() throws Exception {
        assertByteBufferEquals(new byte[0], readDataValue("3000").getEncodedContents());
    }

    @Test
    public void testEmptyIndefiniteLengthContents() throws Exception {
        assertByteBufferEquals(new byte[0], readDataValue("30800000").getEncodedContents());
    }

    @Test
    public void testPrimitiveIndefiniteLengthContentsMustNotBeParsed()
            throws Exception {
        // INTEGER (0x0203) followed by 0x010000. This could be misinterpreted as INTEGER
        // (0x0203000001) if the contents of the original INTEGER are parsed to find the 0x00 0x00
        // indefinite length terminator. Such parsing must not take place for primitive (i.e., not
        // constructed) values.
        assertEquals(
                "0203",
                HexEncoding.encode(readDataValue("028002030000010000").getEncodedContents()));
    }

    @Test
    public void testConstructedIndefiniteLengthContentsContainingIndefiniteLengthEncodedValues()
            throws Exception {
        // Indefinite length SEQUENCE containing elements which themselves use indefinite length
        // encoding, followed by INTEGER (0x0e).
        assertEquals(
                "3080028001000000000280020000",
                HexEncoding.encode(readDataValue(
                        "30803080028001000000000280020000000002010c").getEncodedContents()));
    }

    @Test(expected = BerDataValueFormatException.class)
    public void testConstructedIndefiniteLengthContentsContainingGarbage() throws Exception {
        // Indefinite length SEQUENCE containing truncated data value. Parsing is expected to fail
        // because the value of the sequence must be parsed (and this will fail because of garbage)
        // to establish where to look for the 0x00 0x00 indefinite length terminator of the
        // SEQUENCE.
        readDataValue("3080020a030000");
    }

    @Test
    public void testReadAdvancesPosition() throws Exception {
        BerDataValueReader reader = createReader("37018f050001020304");
        assertByteBufferEquals(HexEncoding.decode("37018f"), reader.readDataValue().getEncoded());
        assertByteBufferEquals(HexEncoding.decode("0500"), reader.readDataValue().getEncoded());
        assertByteBufferEquals(HexEncoding.decode("01020304"), reader.readDataValue().getEncoded());
        assertNull(reader.readDataValue());
    }

    private BerDataValueReader createReader(String hexEncodedInput) {
        return createReader(HexEncoding.decode(hexEncodedInput));
    }

    private BerDataValue readDataValue(byte[] input)
            throws BerDataValueFormatException {
        return createReader(input).readDataValue();
    }

    private BerDataValue readDataValue(String hexEncodedInput)
            throws BerDataValueFormatException {
        return createReader(hexEncodedInput).readDataValue();
    }

    private static byte[] concat(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1,  0, result, 0, arr1.length);
        System.arraycopy(arr2,  0, result, arr1.length, arr2.length);
        return result;
    }
}
