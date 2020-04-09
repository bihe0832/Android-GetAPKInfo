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

package com.android.apksig.internal.util;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.android.apksig.util.DataSources;
import com.android.apksig.util.DataSinks;
import com.android.apksig.util.ReadableDataSink;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.ByteBuffer;

/** Unit tests for {@link ChainedDataSource}. */
@RunWith(JUnit4.class)
public final class ChainedDataSourceTest {

    private ChainedDataSource mChain;

    @Before public void setUp() {
        mChain = new ChainedDataSource(
                DataSources.asDataSource(ByteBuffer.wrap("12".getBytes(US_ASCII))),
                DataSources.asDataSource(ByteBuffer.wrap("34567".getBytes(US_ASCII))),
                DataSources.asDataSource(ByteBuffer.wrap("".getBytes(US_ASCII))),
                DataSources.asDataSource(ByteBuffer.wrap("890".getBytes(US_ASCII))),
                DataSources.asDataSource(ByteBuffer.wrap("".getBytes(US_ASCII))));
        assertEquals(10, mChain.size());
    }

    @Test public void feedAllPossibleRanges() throws Exception {
        for (int begin = 0; begin < mChain.size(); begin++) {
            for (int end = begin + 1; end < mChain.size(); end++) {
                int size = end - begin;
                ReadableDataSink sink = DataSinks.newInMemoryDataSink(size);
                mChain.feed(begin, size, sink);
                assertByteBufferEquals(
                        ByteBuffer.wrap("1234567890".substring(begin, end).getBytes(US_ASCII)),
                        sink.getByteBuffer(0, size));
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void feedMoreThanAvailable() throws Exception {
        mChain.feed(0, mChain.size() + 1, DataSinks.newInMemoryDataSink(3));
    }

    @Test public void getByteBufferFromAllPossibleRanges() throws Exception {
        for (int begin = 0; begin < mChain.size(); begin++) {
            for (int end = begin + 1; end < mChain.size(); end++) {
                int size = end - begin;
                ByteBuffer buffer = mChain.getByteBuffer(begin, size);
                assertByteBufferEquals(
                        ByteBuffer.wrap("1234567890".substring(begin, end).getBytes(US_ASCII)),
                        buffer);
            }
        }
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void getByteBufferForMoreThanAvailable() throws Exception {
        mChain.getByteBuffer(0, (int) mChain.size() + 1);
    }

    @Test
    public void copyTo() throws Exception {
        for (int begin = 0; begin < mChain.size(); begin++) {
            for (int end = begin + 1; end < mChain.size(); end++) {
                int size = end - begin;
                ByteBuffer buffer = ByteBuffer.allocate(size);
                mChain.copyTo(begin, size, buffer);
                assertEquals(size, buffer.position());

                buffer.rewind();
                assertByteBufferEquals(
                        ByteBuffer.wrap("1234567890".substring(begin, end).getBytes(US_ASCII)),
                        buffer);
            }
        }
    }

    @Test
    public void slice() throws Exception {
        for (int begin = 0; begin < mChain.size(); begin++) {
            for (int end = begin + 1; end < mChain.size(); end++) {
                int size = end - begin;
                ByteBuffer buffer = mChain.slice(begin, size).getByteBuffer(0, size);

                assertByteBufferEquals(
                        ByteBuffer.wrap("1234567890".substring(begin, end).getBytes(US_ASCII)),
                        buffer);
            }
        }
    }

    private void assertByteBufferEquals(ByteBuffer buffer, ByteBuffer buffer2) {
        assertTrue(buffer.toString() + " vs " + buffer2.toString() + ", byte array: " +
                HexEncoding.encode(buffer.array()) + " vs " + HexEncoding.encode(buffer2.array()),
                buffer.compareTo(buffer2) == 0);
    }
}
