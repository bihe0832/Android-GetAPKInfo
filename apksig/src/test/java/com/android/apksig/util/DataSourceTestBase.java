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

package com.android.apksig.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * Base class for testing implementations of {@link DataSource}. This class tests the contract of
 * {@code DataSource}.
 *
 * <p>To subclass, provide an implementation of {@link #createDataSource(byte[])} which returns
 * the implementation of {@code DataSource} you want to test.
 */
public abstract class DataSourceTestBase {

    /**
     * Returns a new {@link DataSource} containing the provided contents.
     */
    protected abstract CloseableWithDataSource createDataSource(byte[] contents) throws IOException;

    protected CloseableWithDataSource createDataSource(String contents) throws IOException {
        return createDataSource(contents.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testSize() throws Exception {
        try (CloseableWithDataSource c = createDataSource("Hello12345")) {
            DataSource ds = c.getDataSource();
            assertEquals(10, ds.size());
        }
    }

    @Test
    public void testSlice() throws Exception {
        try (CloseableWithDataSource c = createDataSource("Hello12345")) {
            DataSource ds = c.getDataSource();
            assertSliceEquals("123", ds, 5, 3);
            DataSource slice = ds.slice(3, 5);
            assertGetByteBufferEquals("lo123", slice, 0, 5);

            // Zero-length slices
            assertSliceEquals("", ds, 0, 0);
            assertSliceEquals("", ds, 1, 0);
            assertSliceEquals("", ds, ds.size() - 2, 0);
            assertSliceEquals("", ds, ds.size() - 1, 0);
            assertSliceEquals("", ds, ds.size(), 0);
            assertSliceEquals("", slice, 0, 0);
            assertSliceEquals("", slice, 1, 0);
            assertSliceEquals("", slice, slice.size() - 2, 0);
            assertSliceEquals("", slice, slice.size() - 1, 0);
            assertSliceEquals("", slice, slice.size(), 0);

            // Invalid slices
            assertSliceThrowsIOOB(ds, -1, 0);
            assertSliceThrowsIOOB(slice, -1, 0);
            assertSliceThrowsIOOB(ds, -1, 2);
            assertSliceThrowsIOOB(slice, -1, 2);
            assertSliceThrowsIOOB(ds, -1, 20);
            assertSliceThrowsIOOB(slice, -1, 20);
            assertSliceThrowsIOOB(ds, 1, 20);
            assertSliceThrowsIOOB(slice, 1, 20);
            assertSliceThrowsIOOB(ds, ds.size() + 1, 0);
            assertSliceThrowsIOOB(slice, slice.size() + 1, 0);
            assertSliceThrowsIOOB(ds, ds.size(), 1);
            assertSliceThrowsIOOB(slice, slice.size(), 1);
            assertSliceThrowsIOOB(ds, ds.size() - 1, -1);
            assertSliceThrowsIOOB(ds, slice.size() - 1, -1);
        }
    }

    @Test
    public void testGetByteBuffer() throws Exception {
        try (CloseableWithDataSource c = createDataSource("test1234")) {
            DataSource ds = c.getDataSource();
            assertGetByteBufferEquals("s", ds, 2, 1);
            DataSource slice = ds.slice(3, 4); // "t123"
            assertGetByteBufferEquals("2", slice, 2, 1);

            // Zero-length chunks
            assertEquals(0, ds.getByteBuffer(0, 0).capacity());
            assertEquals(0, ds.getByteBuffer(ds.size(), 0).capacity());
            assertEquals(0, ds.getByteBuffer(ds.size() - 1, 0).capacity());
            assertEquals(0, ds.getByteBuffer(ds.size() - 2, 0).capacity());
            assertEquals(0, slice.getByteBuffer(0, 0).capacity());
            assertEquals(0, slice.getByteBuffer(slice.size(), 0).capacity());
            assertEquals(0, slice.getByteBuffer(slice.size() - 1, 0).capacity());
            assertEquals(0, slice.getByteBuffer(slice.size() - 2, 0).capacity());

            // Invalid chunks
            assertGetByteBufferThrowsIOOB(ds, -1, 0);
            assertGetByteBufferThrowsIOOB(slice, -1, 0);
            assertGetByteBufferThrowsIOOB(ds, -1, 2);
            assertGetByteBufferThrowsIOOB(slice, -1, 2);
            assertGetByteBufferThrowsIOOB(ds, -1, 20);
            assertGetByteBufferThrowsIOOB(slice, -1, 20);
            assertGetByteBufferThrowsIOOB(ds, 1, 20);
            assertGetByteBufferThrowsIOOB(slice, 1, 20);
            assertGetByteBufferThrowsIOOB(ds, ds.size() + 1, 0);
            assertGetByteBufferThrowsIOOB(slice, slice.size() + 1, 0);
            assertGetByteBufferThrowsIOOB(ds, ds.size(), 1);
            assertGetByteBufferThrowsIOOB(slice, slice.size(), 1);
            assertGetByteBufferThrowsIOOB(ds, ds.size() - 1, -1);
            assertGetByteBufferThrowsIOOB(ds, slice.size() - 1, -1);
        }
    }

    @Test
    public void testFeed() throws Exception {
        try (CloseableWithDataSource c = createDataSource("test1234")) {
            DataSource ds = c.getDataSource();
            assertFeedEquals("23", ds, 5, 2);
            DataSource slice = ds.slice(1, 5); // "est12"
            assertFeedEquals("t", slice, 2, 1);

            // Zero-length chunks
            assertFeedEquals("", ds, 0, 0);
            assertFeedEquals("", ds, 1, 0);
            assertFeedEquals("", ds, ds.size() - 2, 0);
            assertFeedEquals("", ds, ds.size() - 1, 0);
            assertFeedEquals("", ds, ds.size(), 0);
            assertFeedEquals("", slice, 0, 0);
            assertFeedEquals("", slice, 2, 0);
            assertFeedEquals("", slice, slice.size() - 2, 0);
            assertFeedEquals("", slice, slice.size() - 1, 0);
            assertFeedEquals("", slice, slice.size(), 0);

            // Invalid chunks
            assertFeedThrowsIOOB(ds, -1, 0);
            assertFeedThrowsIOOB(slice, -1, 0);
            assertFeedThrowsIOOB(ds, -1, 2);
            assertFeedThrowsIOOB(slice, -1, 2);
            assertFeedThrowsIOOB(ds, -1, 10);
            assertFeedThrowsIOOB(slice, -1, 10);
            assertFeedThrowsIOOB(ds, 1, 10);
            assertFeedThrowsIOOB(slice, 1, 10);
            assertFeedThrowsIOOB(ds, ds.size() + 1, 0);
            assertFeedThrowsIOOB(slice, slice.size() + 1, 0);
            assertFeedThrowsIOOB(ds, ds.size(), 1);
            assertFeedThrowsIOOB(slice, slice.size(), 1);
            assertFeedThrowsIOOB(ds, ds.size() - 1, -1);
            assertFeedThrowsIOOB(ds, slice.size() - 1, -1);
        }
    }

    @Test
    public void testCopyTo() throws Exception {
        try (CloseableWithDataSource c = createDataSource("abcdefghijklmnop")) {
            DataSource ds = c.getDataSource();
            assertCopyToEquals("fgh", ds, 5, 3);
            DataSource slice = ds.slice(2, 7); // "cdefghi"
            assertCopyToEquals("efgh", slice, 2, 4);

            // Zero-length chunks
            assertCopyToEquals("", ds, 0, 0);
            assertCopyToEquals("", ds, 1, 0);
            assertCopyToEquals("", ds, ds.size() - 2, 0);
            assertCopyToEquals("", ds, ds.size() - 1, 0);
            assertCopyToEquals("", ds, ds.size(), 0);
            assertCopyToEquals("", slice, 0, 0);
            assertCopyToEquals("", slice, 2, 0);
            assertCopyToEquals("", slice, slice.size() - 2, 0);
            assertCopyToEquals("", slice, slice.size() - 1, 0);
            assertCopyToEquals("", slice, slice.size(), 0);

            // Invalid chunks
            assertCopyToThrowsIOOB(ds, -1, 0);
            assertCopyToThrowsIOOB(slice, -1, 0);
            assertCopyToThrowsIOOB(ds, -1, 2);
            assertCopyToThrowsIOOB(slice, -1, 2);
            assertCopyToThrowsIOOB(ds, -1, 20);
            assertCopyToThrowsIOOB(slice, -1, 20);
            assertCopyToThrowsIOOB(ds, 1, 20);
            assertCopyToThrowsIOOB(slice, 1, 20);
            assertCopyToThrowsIOOB(ds, ds.size() + 1, 0);
            assertCopyToThrowsIOOB(slice, slice.size() + 1, 0);
            assertCopyToThrowsIOOB(ds, ds.size(), 1);
            assertCopyToThrowsIOOB(slice, slice.size(), 1);
            assertCopyToThrowsIOOB(ds, ds.size() - 1, -1);
            assertCopyToThrowsIOOB(ds, slice.size() - 1, -1);

            // Destination buffer too small
            ByteBuffer buf = ByteBuffer.allocate(5);
            buf.position(2);
            buf.limit(3);
            assertCopyToThrowsBufferOverflow(ds, 0, 2, buf);
            buf.position(2);
            buf.limit(3);
            assertCopyToThrowsBufferOverflow(slice, 1, 2, buf);

            // Destination buffer larger than chunk copied using copyTo
            buf = ByteBuffer.allocate(10);
            buf.position(2);
            assertCopyToEquals("bcd", ds, 1, 3, buf);
            buf = ByteBuffer.allocate(10);
            buf.position(2);
            assertCopyToEquals("fg", slice, 3, 2, buf);
        }
    }

    protected static void assertSliceEquals(
            String expectedContents, DataSource ds, long offset, int size) throws IOException {
        DataSource slice = ds.slice(offset, size);
        assertEquals(size, slice.size());
        assertGetByteBufferEquals(expectedContents, slice, 0, size);
    }

    protected static void assertSliceThrowsIOOB(DataSource ds, long offset, int size) {
        try {
            ds.slice(offset, size);
            fail();
        } catch (IndexOutOfBoundsException expected) {}
    }

    protected static void assertGetByteBufferEquals(
            String expectedContents, DataSource ds, long offset, int size) throws IOException {
        ByteBuffer buf = ds.getByteBuffer(offset, size);
        assertEquals(0, buf.position());
        assertEquals(size, buf.limit());
        assertEquals(size, buf.capacity());
        assertEquals(expectedContents, toString(buf));
    }

    protected static void assertGetByteBufferThrowsIOOB(DataSource ds, long offset, int size)
            throws IOException {
        try {
            ds.getByteBuffer(offset, size);
            fail();
        } catch (IndexOutOfBoundsException expected) {}
    }

    protected static void assertFeedEquals(
            String expectedFedContents, DataSource ds, long offset, int size) throws IOException {
        ReadableDataSink out = DataSinks.newInMemoryDataSink(size);
        ds.feed(offset, size, out);
        assertEquals(size, out.size());
        assertEquals(expectedFedContents, toString(out.getByteBuffer(0, size)));
    }

    protected static void assertFeedThrowsIOOB(DataSource ds, long offset, long size)
            throws IOException {
        try {
            ds.feed(offset, size, NullDataSink.INSTANCE);
            fail();
        } catch (IndexOutOfBoundsException expected) {}
    }

    protected static void assertCopyToEquals(
            String expectedContents, DataSource ds, long offset, int size) throws IOException {
        // Create a ByteBuffer backed by a section of a byte array. The ByteBuffer is on purpose not
        // starting at offset 0 to catch issues to do with not checking ByteBuffer.arrayOffset().
        byte[] arr = new byte[size + 10];
        ByteBuffer buf = ByteBuffer.wrap(arr, 1, size + 5);
        // Use non-zero position to catch issues with not checking buf.position()
        buf.position(2);
        // Buffer contains sufficient space for the requested copyTo operation
        assertEquals(size + 4, buf.remaining());
        assertCopyToEquals(expectedContents, ds, offset, size, buf);
    }

    private static void assertCopyToEquals(
            String expectedContents, DataSource ds, long offset, int size, ByteBuffer buf)
                    throws IOException {
        int oldPosition = buf.position();
        int oldLimit = buf.limit();
        ds.copyTo(offset, size, buf);
        // Position should've advanced by size whereas limit should've remained unchanged
        assertEquals(oldPosition + size, buf.position());
        assertEquals(oldLimit, buf.limit());

        buf.limit(buf.position());
        buf.position(oldPosition);
        assertEquals(expectedContents, toString(buf));
    }

    protected static void assertCopyToThrowsIOOB(DataSource ds, long offset, int size)
            throws IOException {
        ByteBuffer buf = ByteBuffer.allocate((size < 0) ? 0 : size);
        try {
            ds.copyTo(offset, size, buf);
            fail();
        } catch (IndexOutOfBoundsException expected) {}
    }

    private static void assertCopyToThrowsBufferOverflow(
            DataSource ds, long offset, int size, ByteBuffer buf) throws IOException {
        try {
            ds.copyTo(offset, size, buf);
            fail();
        } catch (BufferOverflowException expected) {}
    }

    /**
     * Returns the contents of the provided buffer as a string. The buffer's position and limit
     * remain unchanged.
     */
    static String toString(ByteBuffer buf) {
        byte[] arr;
        int offset;
        int size = buf.remaining();
        if (buf.hasArray()) {
            arr = buf.array();
            offset = buf.arrayOffset() + buf.position();
        } else {
            arr = new byte[buf.remaining()];
            offset = 0;
            int oldPos = buf.position();
            buf.get(arr);
            buf.position(oldPos);
        }
        return new String(arr, offset, size, StandardCharsets.UTF_8);
    }

    public static class CloseableWithDataSource implements Closeable {
        private final DataSource mDataSource;
        private final Closeable mCloseable;

        private CloseableWithDataSource(DataSource dataSource, Closeable closeable) {
            mDataSource = dataSource;
            mCloseable = closeable;
        }

        public static CloseableWithDataSource of(DataSource dataSource) {
            return new CloseableWithDataSource(dataSource, null);
        }

        public static CloseableWithDataSource of(DataSource dataSource, Closeable closeable) {
            return new CloseableWithDataSource(dataSource, closeable);
        }

        public DataSource getDataSource() {
            return mDataSource;
        }

        public Closeable getCloseable() {
            return mCloseable;
        }

        @Override
        public void close() throws IOException {
            if (mCloseable != null) {
                mCloseable.close();
            }
        }
    }

    private static final class NullDataSink implements DataSink {
        private static final NullDataSink INSTANCE = new NullDataSink();

        @Override
        public void consume(byte[] buf, int offset, int length) {}

        @Override
        public void consume(ByteBuffer buf) {}
    }
}
