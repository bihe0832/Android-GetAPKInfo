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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.android.apksig.internal.util.HexEncoding;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Asn1BerParserTest {
    @Test(expected = NullPointerException.class)
    public void testNullInput() throws Exception {
        parse((ByteBuffer) null, EmptySequence.class);
    }

    @Test(expected = Asn1DecodingException.class)
    public void testEmptyInput() throws Exception {
        parse("", EmptySequence.class);
    }

    @Test
    public void testEmptySequence() throws Exception {
        // Empty SEQUENCE (0x3000) followed by garbage (0x12345678)
        ByteBuffer input = ByteBuffer.wrap(HexEncoding.decode("300012345678"));
        EmptySequence container = parse(input, EmptySequence.class);
        assertNotNull(container);
        // Check that input position has been advanced appropriately
        assertEquals(2, input.position());
    }

    @Test
    public void testOctetString() throws Exception {
        assertEquals(
                "123456",
                HexEncoding.encode(parse("30050403123456", SequenceWithOctetString.class).buf));
        assertEquals(
                "", HexEncoding.encode(parse("30020400", SequenceWithOctetString.class).buf));
    }

    @Test
    public void testBitString() throws Exception {
        assertEquals(
                "123456",
                HexEncoding.encode(parse("30050303123456", SequenceWithBitString.class).buf));
        assertEquals(
                "", HexEncoding.encode(parse("30020300", SequenceWithBitString.class).buf));
    }

    @Test
    public void testBoolean() throws Exception {
        assertEquals(false, parse("3003010100", SequenceWithBoolean.class).value);
        assertEquals(true, parse("3003010101", SequenceWithBoolean.class).value);
        assertEquals(true, parse("30030101FF", SequenceWithBoolean.class).value);
    }

    @Test
    public void testUTCTime() throws Exception {
        assertEquals("1212211221Z",
                parse("300d170b313231323231313232315a", SequenceWithUTCTime.class).value);
        assertEquals("9912312359Z",
                parse("300d170b393931323331323335395a", SequenceWithUTCTime.class).value);
    }

    @Test
    public void testGeneralizedTime() throws Exception {
        assertEquals("201212211220.999-07", parse("301518133230313231323231313232302e3939392d3037",
                SequenceWithGeneralizedTime.class).value);
        assertEquals("20380119031407.000+00",
                parse("3017181532303338303131393033313430372e3030302b3030",
                        SequenceWithGeneralizedTime.class).value);
    }

    @Test
    public void testInteger() throws Exception {
        // Various Java types decoded from INTEGER
        // Empty SEQUENCE (0x3000) followed by garbage (0x12345678)
        SequenceWithIntegers container =
                parse("301e"
                        + "0201ff" // -1
                        + "0207ff123456789abc" // -7f123456789abc
                        + "0200" // 0
                        + "020280ff" // -255
                        + "020a00000000000000001234", // 0x1234
                        SequenceWithIntegers.class);
        assertEquals(-1, container.n1);
    }

    @Test
    public void testOid() throws Exception {
        // Empty OID
        try {
            parse("30020600", SequenceWithOid.class);
            fail();
        } catch (Asn1DecodingException expected) {}


        assertEquals("2.100.3", parse("30050603813403", SequenceWithOid.class).oid);
        assertEquals(
                "2.16.840.1.101.3.4.2.1",
                parse("300b0609608648016503040201", SequenceWithOid.class).oid);
    }

    @Test
    public void testSequenceOf() throws Exception {
        assertEquals(2, parse("3006300430003000", SequenceWithSequenceOf.class).values.size());
    }

    @Test
    public void testSetOf() throws Exception {
        assertEquals(2, parse("3006310430003000", SequenceWithSetOf.class).values.size());
    }

    @Test
    public void testUnencodedContainer() throws Exception {
        SequenceWithSequenceOfUnencodedContainers seq = parse("300C300A31023000310430003000",
                SequenceWithSequenceOfUnencodedContainers.class);
        assertEquals(2, seq.containers.size());
        assertEquals(1, seq.containers.get(0).values.size());
        assertEquals(2, seq.containers.get(1).values.size());
    }

    @Test
    public void testImplicitOptionalField() throws Exception {
        // Optional field f2 missing in the input
        SequenceWithImplicitOptionalField seq =
                parse("300602010d02012a", SequenceWithImplicitOptionalField.class);
        assertEquals(13, seq.f1.intValue());
        assertNull(seq.f2);
        assertEquals(42, seq.f3.intValue());

        // Optional field f2 present in the input
        seq = parse("300a02010da102ffff02012a", SequenceWithImplicitOptionalField.class);
        assertEquals(13, seq.f1.intValue());
        assertEquals(-1, seq.f2.intValue());
        assertEquals(42, seq.f3.intValue());
    }


    @Test
    public void testExplicitOptionalField() throws Exception {
        // Optional field f2 missing in the input
        SequenceWithExplicitOptionalField seq =
                parse("300602010d02012a", SequenceWithExplicitOptionalField.class);
        assertEquals(13, seq.f1.intValue());
        assertNull(seq.f2);
        assertEquals(42, seq.f3.intValue());

        // Optional field f2 present in the input
        seq = parse("300c02010da1040202ffff02012a", SequenceWithExplicitOptionalField.class);
        assertEquals(13, seq.f1.intValue());
        assertEquals(-1, seq.f2.intValue());
        assertEquals(42, seq.f3.intValue());
    }

    @Test
    public void testChoiceWithDifferentTypedOptions() throws Exception {
        // The CHOICE can be either an INTEGER or an OBJECT IDENTIFIER

        // INTEGER
        ChoiceWithTwoOptions c = parse("0208ffffffffffffffff", ChoiceWithTwoOptions.class);
        assertNull(c.oid);
        assertEquals(-1, c.num.intValue());

        // OBJECT IDENTIFIER
        c = parse("060100", ChoiceWithTwoOptions.class);
        assertEquals("0.0", c.oid);
        assertNull(c.num);

        // Empty input
        try {
            parse("", ChoiceWithTwoOptions.class);
            fail();
        } catch (Asn1DecodingException expected) {}

        // Neither of the options match
        try {
            // Empty SEQUENCE
            parse("3000", ChoiceWithTwoOptions.class);
            fail();
        } catch (Asn1DecodingException expected) {}
    }

    @Test
    public void testChoiceWithSameTypedOptions() throws Exception {
        // The CHOICE can be either a SEQUENCE, an IMPLICIT SEQUENCE, or an EXPLICIT SEQUENCE

        // SEQUENCE
        ChoiceWithThreeSequenceOptions c = parse("3000", ChoiceWithThreeSequenceOptions.class);
        assertNotNull(c.s1);
        assertNull(c.s2);
        assertNull(c.s3);

        // IMPLICIT [0] SEQUENCE
        c = parse("a000", ChoiceWithThreeSequenceOptions.class);
        assertNull(c.s1);
        assertNotNull(c.s2);
        assertNull(c.s3);

        // EXPLICIT [0] SEQUENCE
        c = parse("a1023000", ChoiceWithThreeSequenceOptions.class);
        assertNull(c.s1);
        assertNull(c.s2);
        assertNotNull(c.s3);

        // INTEGER -- None of the options match
        try {
            parse("02010a", ChoiceWithThreeSequenceOptions.class);
            fail();
        } catch (Asn1DecodingException expected) {}
    }

    @Test(expected = Asn1DecodingException.class)
    public void testChoiceWithClashingOptions() throws Exception {
        // The CHOICE is between INTEGER and INTEGER which clash
        parse("0200", ChoiceWithClashingOptions.class);
    }

    @Test
    public void testPrimitiveIndefiniteLengthEncodingWithGarbage() throws Exception {
        // Indefinite length INTEGER containing what may look like a malformed definite length
        // INTEGER, followed by an INTEGER. This tests that contents of indefinite length encoded
        // primitive (i.e., not constructed) data values must not be parsed to locate the 0x00 0x00
        // terminator.
        ByteBuffer input = ByteBuffer.wrap(HexEncoding.decode("0280020401000002010c"));
        ChoiceWithTwoOptions c = parse(input, ChoiceWithTwoOptions.class);
        // Check what's remaining in the input buffer
        assertEquals("02010c", HexEncoding.encode(input));
        // Check what was consumed
        assertEquals(0x020401, c.num.intValue());

        // Indefinite length INTEGER containing what may look like a malformed indefinite length
        // INTEGER, followed by an INTEGER
        input = ByteBuffer.wrap(HexEncoding.decode("0280028001000002010c"));
        c = parse(input, ChoiceWithTwoOptions.class);
        // Check what's remaining in the input buffer
        assertEquals("02010c", HexEncoding.encode(input));
        // Check what was consumed
        assertEquals(0x028001, c.num.intValue());
    }

    @Test
    public void testConstructedIndefiniteLengthEncodingWithoutNestedIndefiniteLengthDataValues()
            throws Exception {
        // Indefinite length SEQUENCE containing an INTEGER whose encoding contains 0x00 0x00 which
        // can be misinterpreted as indefinite length encoding terminator of the SEQUENCE, followed
        // by an INTEGER
        ByteBuffer input = ByteBuffer.wrap(HexEncoding.decode("308002020000000002010c"));
        SequenceWithAsn1Opaque c = parse(input, SequenceWithAsn1Opaque.class);
        // Check what's remaining in the input buffer
        assertEquals("02010c", HexEncoding.encode(input));
        // Check what was read
        assertEquals("02020000", HexEncoding.encode(c.obj.getEncoded()));
    }

    @Test
    public void testConstructedIndefiniteLengthEncodingWithNestedIndefiniteLengthDataValues()
            throws Exception {
        // Indefinite length SEQUENCE containing two INTEGER fields using indefinite
        // length encoding, followed by an INTEGER. This tests that the 0x00 0x00 terminators used
        // by the encoding of the two INTEGERs are not confused for the 0x00 0x00 terminator of the
        // SEQUENCE.
        ByteBuffer input =
                ByteBuffer.wrap(HexEncoding.decode("308002800300000280030000020103000002010c"));
        SequenceWithAsn1Opaque c = parse(input, SequenceWithAsn1Opaque.class);
        // Check what's remaining in the input buffer
        assertEquals("02010c", HexEncoding.encode(input));
        // Check what was consumed
        assertEquals("0280030000", HexEncoding.encode(c.obj.getEncoded()));
    }

    @Test(expected = Asn1DecodingException.class)
    public void testConstructedIndefiniteLengthEncodingWithGarbage() throws Exception {
        // Indefinite length SEQUENCE containing an indefinite length encoded SEQUENCE containing
        // garbage which doesn't parse as BER, followed by an INTEGER. This tests that contents of
        // the SEQUENCEs must be parsed to establish where their 0x00 0x00 terminators are located.
        ByteBuffer input = ByteBuffer.wrap(HexEncoding.decode("3080308002040000000002010c"));
        parse(input, SequenceWithAsn1Opaque.class);
    }

    private static <T> T parse(String hexEncodedInput, Class<T> containerClass)
            throws Asn1DecodingException {
        ByteBuffer input =
                (hexEncodedInput == null)
                        ? null : ByteBuffer.wrap(HexEncoding.decode(hexEncodedInput));
        return parse(input, containerClass);
    }

    private static <T> T parse(ByteBuffer input, Class<T> containerClass)
            throws Asn1DecodingException {
        return Asn1BerParser.parse(input, containerClass);
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class EmptySequence {}

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithIntegers {
        @Asn1Field(index = 1, type = Asn1Type.INTEGER)
        public int n1;

        @Asn1Field(index = 2, type = Asn1Type.INTEGER)
        public long n2;

        @Asn1Field(index = 3, type = Asn1Type.INTEGER)
        public Integer n3;

        @Asn1Field(index = 4, type = Asn1Type.INTEGER)
        public Long n4;

        @Asn1Field(index = 5, type = Asn1Type.INTEGER)
        public BigInteger n5;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithOid {
        @Asn1Field(index = 0, type = Asn1Type.OBJECT_IDENTIFIER)
        public String oid;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithImplicitOptionalField {
        @Asn1Field(index = 1, type = Asn1Type.INTEGER)
        public Integer f1;

        @Asn1Field(index = 2, type = Asn1Type.INTEGER, optional = true,
                tagging = Asn1Tagging.IMPLICIT, tagNumber = 1)
        public Integer f2;

        @Asn1Field(index = 3, type = Asn1Type.INTEGER)
        public Integer f3;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithExplicitOptionalField {
        @Asn1Field(index = 1, type = Asn1Type.INTEGER)
        public Integer f1;

        @Asn1Field(index = 2, type = Asn1Type.INTEGER, optional = true,
                tagging = Asn1Tagging.EXPLICIT, tagNumber = 1)
        public Integer f2;

        @Asn1Field(index = 3, type = Asn1Type.INTEGER)
        public Integer f3;
    }

    @Asn1Class(type = Asn1Type.CHOICE)
    public static class ChoiceWithTwoOptions {
        @Asn1Field(type = Asn1Type.OBJECT_IDENTIFIER)
        public String oid;

        @Asn1Field(type = Asn1Type.INTEGER)
        public Integer num;
    }

    @Asn1Class(type = Asn1Type.CHOICE)
    public static class ChoiceWithThreeSequenceOptions {
        @Asn1Field(type = Asn1Type.SEQUENCE)
        public EmptySequence s1;

        @Asn1Field(type = Asn1Type.SEQUENCE, tagging = Asn1Tagging.IMPLICIT, tagNumber = 0)
        public EmptySequence s2;

        @Asn1Field(type = Asn1Type.SEQUENCE, tagging = Asn1Tagging.EXPLICIT, tagNumber = 1)
        public EmptySequence s3;
    }

    @Asn1Class(type = Asn1Type.CHOICE)
    public static class ChoiceWithClashingOptions {
        @Asn1Field(type = Asn1Type.INTEGER)
        public int n1;

        @Asn1Field(type = Asn1Type.INTEGER)
        public Integer n2;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithOctetString {
        @Asn1Field(index = 0, type = Asn1Type.OCTET_STRING)
        public ByteBuffer buf;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithBitString {
        @Asn1Field(index = 0, type = Asn1Type.BIT_STRING)
        public ByteBuffer buf;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithSequenceOf {
        @Asn1Field(index = 0, type = Asn1Type.SEQUENCE_OF)
        public List<EmptySequence> values;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithSetOf {
        @Asn1Field(index = 0, type = Asn1Type.SET_OF)
        public List<EmptySequence> values;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithAsn1Opaque {
        @Asn1Field(type = Asn1Type.ANY)
        public Asn1OpaqueObject obj;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithSequenceOfUnencodedContainers {
        @Asn1Field(type = Asn1Type.SEQUENCE_OF)
        public List<UnencodedContainerWithSetOf> containers;
    }

    @Asn1Class(type = Asn1Type.UNENCODED_CONTAINER)
    public static class UnencodedContainerWithSetOf {
        @Asn1Field(type = Asn1Type.SET_OF)
        public List<EmptySequence> values;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithBoolean {
        @Asn1Field(type = Asn1Type.BOOLEAN)
        public boolean value;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithUTCTime {
        @Asn1Field(type = Asn1Type.UTC_TIME)
        public String value;
    }

    @Asn1Class(type = Asn1Type.SEQUENCE)
    public static class SequenceWithGeneralizedTime {
        @Asn1Field(type = Asn1Type.GENERALIZED_TIME)
        public String value;
    }
}
