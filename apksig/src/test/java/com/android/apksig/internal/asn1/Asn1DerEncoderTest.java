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

package com.android.apksig.internal.asn1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import com.android.apksig.internal.util.HexEncoding;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Asn1DerEncoderTest {
    @Test
    public void testInteger() throws Exception {
        assertEquals("3003020100", encodeToHex(new SequenceWithInteger(0)));
        assertEquals("300302010c", encodeToHex(new SequenceWithInteger(12)));
        assertEquals("300302017f", encodeToHex(new SequenceWithInteger(0x7f)));
        assertEquals("3004020200ff", encodeToHex(new SequenceWithInteger(0xff)));
        assertEquals("30030201ff", encodeToHex(new SequenceWithInteger(-1)));
        assertEquals("3003020180", encodeToHex(new SequenceWithInteger(-128)));
        assertEquals("3005020300ffee", encodeToHex(new SequenceWithInteger(0xffee)));
        assertEquals("300602047fffffff", encodeToHex(new SequenceWithInteger(Integer.MAX_VALUE)));
        assertEquals("3006020480000000", encodeToHex(new SequenceWithInteger(Integer.MIN_VALUE)));
    }

    @Test
    public void testOctetString() throws Exception {
        assertEquals(
                "30050403010203",
                encodeToHex(
                        new SequenceWithByteBufferOctetString(
                                ByteBuffer.wrap(new byte[] {1, 2, 3}))));
        assertEquals(
                "30030401ff",
                encodeToHex(
                        new SequenceWithByteBufferOctetString(
                                ByteBuffer.wrap(new byte[] {(byte) 0xff}))));

        assertEquals(
                "30020400",
                encodeToHex(
                        new SequenceWithByteBufferOctetString(ByteBuffer.wrap(new byte[0]))));
    }

    @Test
    public void testBitString() throws Exception {
        assertEquals(
                "30050303010203",
                encodeToHex(
                        new SequenceWithByteBufferBitString(
                                ByteBuffer.wrap(new byte[] {1, 2, 3}))));
        assertEquals(
                "30030301ff",
                encodeToHex(
                        new SequenceWithByteBufferBitString(
                                ByteBuffer.wrap(new byte[] {(byte) 0xff}))));

        assertEquals(
                "30020300",
                encodeToHex(
                        new SequenceWithByteBufferBitString(ByteBuffer.wrap(new byte[0]))));
    }


    @Test
    public void testOid() throws Exception {
        assertEquals("3003060100", encodeToHex(new SequenceWithOid("0.0")));
        assertEquals(
                "300b06092b0601040182371514",
                encodeToHex(new SequenceWithOid("1.3.6.1.4.1.311.21.20")));
        assertEquals(
                "300b06092a864886f70d010701",
                encodeToHex(new SequenceWithOid("1.2.840.113549.1.7.1")));
        assertEquals(
                "300b0609608648016503040201",
                encodeToHex(new SequenceWithOid("2.16.840.1.101.3.4.2.1")));
    }

    @Test
    public void testChoice() throws Exception {
        assertEquals("0201ff", encodeToHex(Choice.of(-1)));
        assertEquals("80092b0601040182371514", encodeToHex(Choice.of("1.3.6.1.4.1.311.21.20")));
    }

    @Test(expected = Asn1EncodingException.class)
    public void testChoiceWithNoFieldsSet() throws Exception {
        // CHOICE is required to have exactly one field set
        encode(new Choice(null, null));
    }

    @Test(expected = Asn1EncodingException.class)
    public void testChoiceWithMultipleFieldsSet() throws Exception {
        // CHOICE is required to have exactly one field set
        encode(new Choice(123, "1.3.6.1.4.1.311.21.20"));
    }

    @Test
    public void testSetOf() throws Exception {
        assertEquals("3009310702010a020200ff", encodeToHex(SetOfIntegers.of(0x0a, 0xff)));
        // Reordering the elements of the set should not make a difference to the resulting encoding
        assertEquals("3009310702010a020200ff", encodeToHex(SetOfIntegers.of(0xff, 0x0a)));

        assertEquals(
                "300e310c02010a020200ff0203112233",
                encodeToHex(SetOfIntegers.of(0xff, 0x0a, 0x112233)));
    }

    @Test
    public void testSequence() throws Exception {
        assertEquals(
                "30080201000601000400",
                encodeToHex(new Sequence(BigInteger.ZERO, "0.0", new byte[0])));
        // Optional OBJECT IDENTIFIER not set
        assertEquals(
                "30050201000400",
                encodeToHex(new Sequence(BigInteger.ZERO, null, new byte[0])));
        // Required INTEGER not set
        try {
            assertEquals(
                    "30050201000400",
                    encodeToHex(new Sequence(null, "0.0", new byte[0])));
            fail();
        } catch (Asn1EncodingException expected) {}
    }

    @Test
    public void testAsn1Class() throws Exception {
        assertEquals(
                "30053003060100",
                encodeToHex(new SequenceWithAsn1Class(new SequenceWithOid("0.0"))));
    }

    @Test
    public void testOpaque() throws Exception {
        assertEquals(
                "3003060100",
                encodeToHex(new SequenceWithOpaque(
                        new Asn1OpaqueObject(new byte[] {0x06, 0x01, 0x00}))));
    }

    @Test
    public void testBoolean() throws Exception {
        assertEquals("3003010100", encodeToHex(new SequenceWithBoolean(false)));
        String value = encodeToHex(new SequenceWithBoolean(true));
        // The encoding of a true value can be any non-zero value so verify the static portion of
        // the encoding of a sequeuence with a boolean, then verify the last byte is non-zero
        assertEquals("The encoding of a sequence with a boolean is not the expected length.", 10,
                value.length());
        assertEquals(
                "The prefix of the encoding of a sequence with a boolean is not the expected "
                        + "value.",
                "30030101", value.substring(0, 8));
        assertNotEquals("The encoding of true should be non-zero.", "00", value.substring(8));
    }

    @Test
    public void testUTCTime() throws Exception {
        assertEquals("300d170b313231323231313232315a",
                encodeToHex(new SequenceWithUTCTime("1212211221Z")));
        assertEquals("300d170b393931323331323335395a",
                encodeToHex(new SequenceWithUTCTime("9912312359Z")));
    }

    @Test
    public void testGeneralizedTime() throws Exception {
        assertEquals("301518133230313231323231313232302e3939392d3037",
                encodeToHex(new SequenceWithGeneralizedTime("201212211220.999-07")));
        assertEquals("3017181532303338303131393033313430372e3030302b3030",
                encodeToHex(new SequenceWithGeneralizedTime("20380119031407.000+00")));
    }

    @Test
    public void testUnencodedContainer() throws Exception {
        assertEquals("30233021310b30030201003004020200ff310830060204800000003108300602047fffffff",
                encodeToHex(
                        new SequenceWithSequenceOfUnencodedContainers(
                                Arrays.asList(
                                        new UnencodedContainerWithSetOfIntegers(
                                                Arrays.asList(
                                                        new SequenceWithInteger(0),
                                                        new SequenceWithInteger(255))),
                                        new UnencodedContainerWithSetOfIntegers(
                                                Arrays.asList(
                                                        new SequenceWithInteger(
                                                                Integer.MIN_VALUE))),
                                        new UnencodedContainerWithSetOfIntegers(
                                                Arrays.asList(
                                                        new SequenceWithInteger(
                                                                Integer.MAX_VALUE)))))));
    }

    private static byte[] encode(Object obj) throws Asn1EncodingException {
        return Asn1DerEncoder.encode(obj);
    }

    private static String encodeToHex(Object obj) throws Asn1EncodingException {
        return HexEncoding.encode(encode(obj));
    }


    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithInteger {

        @Asn1Field(index = 1, type = Asn1Type.INTEGER)
        public int num;

        public SequenceWithInteger(int num) {
            this.num = num;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithOid {

        @Asn1Field(index = 1, type = Asn1Type.OBJECT_IDENTIFIER)
        public String oid;

        public SequenceWithOid(String oid) {
            this.oid = oid;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithByteBufferOctetString {

        @Asn1Field(index = 1, type = Asn1Type.OCTET_STRING)
        public ByteBuffer data;

        public SequenceWithByteBufferOctetString(ByteBuffer data) {
            this.data = data;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithByteBufferBitString {

        @Asn1Field(index = 1, type = Asn1Type.BIT_STRING)
        public ByteBuffer data;

        public SequenceWithByteBufferBitString(ByteBuffer data) {
            this.data = data;
        }
    }

    @Asn1Class(type = Asn1Type.CHOICE)
    public static class Choice {

        @Asn1Field(type = Asn1Type.INTEGER)
        public Integer num;

        @Asn1Field(type = Asn1Type.OBJECT_IDENTIFIER, tagging = Asn1Tagging.IMPLICIT, tagNumber = 0)
        public String oid;

        public Choice(Integer num, String oid) {
            this.num = num;
            this.oid = oid;
        }

        public static Choice of(int num) {
            return new Choice(num, null);
        }

        public static Choice of(String oid) {
            return new Choice(null, oid);
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SetOfIntegers {
        @Asn1Field(type = Asn1Type.SET_OF, elementType = Asn1Type.INTEGER)
        public List<Integer> values;

        public static SetOfIntegers of(Integer... values) {
            SetOfIntegers result = new SetOfIntegers();
            result.values = Arrays.asList(values);
            return result;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class Sequence {
        @Asn1Field(type = Asn1Type.INTEGER, index = 0)
        public BigInteger num;

        @Asn1Field(type = Asn1Type.OBJECT_IDENTIFIER, index = 1, optional = true)
        public String oid;

        @Asn1Field(type = Asn1Type.OCTET_STRING, index = 2)
        public byte[] octets;

        public Sequence(BigInteger num, String oid, byte[] octets) {
            this.num = num;
            this.oid = oid;
            this.octets = octets;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithAsn1Class {
        @Asn1Field(type = Asn1Type.SEQUENCE)
        public SequenceWithOid seqWithOid;

        public SequenceWithAsn1Class(SequenceWithOid seqWithOid) {
            this.seqWithOid = seqWithOid;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithOpaque {
        @Asn1Field(type = Asn1Type.ANY)
        public Asn1OpaqueObject obj;

        public SequenceWithOpaque(Asn1OpaqueObject obj) {
            this.obj = obj;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithBoolean {

        @Asn1Field(index = 1, type = Asn1Type.BOOLEAN)
        public boolean value;

        public SequenceWithBoolean(boolean value) {
            this.value = value;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithUTCTime {

        @Asn1Field(index = 1, type = Asn1Type.UTC_TIME)
        public String utcTime;

        public SequenceWithUTCTime(String utcTime) {
            this.utcTime = utcTime;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithGeneralizedTime {

        @Asn1Field(index = 1, type = Asn1Type.GENERALIZED_TIME)
        public String generalizedTime;

        public SequenceWithGeneralizedTime(String generalizedTime) {
            this.generalizedTime = generalizedTime;
        }
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithSequenceOfUnencodedContainers {
        @Asn1Field(index = 1, type = Asn1Type.SEQUENCE_OF)
        public List<UnencodedContainerWithSetOfIntegers> containers;

        public SequenceWithSequenceOfUnencodedContainers(
                List<UnencodedContainerWithSetOfIntegers> containers) {
            this.containers = containers;
        }
    }

    @Asn1Class(type = Asn1Type.UNENCODED_CONTAINER)
    public static class UnencodedContainerWithSetOfIntegers {
        @Asn1Field(index = 1, type = Asn1Type.SET_OF)
        public List<SequenceWithInteger> values;

        public UnencodedContainerWithSetOfIntegers(List<SequenceWithInteger> values) {
            this.values = values;
        }
    }
}
