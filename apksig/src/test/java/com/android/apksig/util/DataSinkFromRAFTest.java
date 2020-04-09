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

package com.android.apksig.util;

import com.android.apksig.internal.util.RandomAccessFileDataSink;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link DataSink} returned by
 * {@link DataSinks#asDataSink(java.io.RandomAccessFile)}.
 */
@RunWith(JUnit4.class)
public class DataSinkFromRAFTest extends DataSinkTestBase<RandomAccessFileDataSink> {

    @Override
    protected CloseableWithDataSink<RandomAccessFileDataSink> createDataSink() throws IOException {
        File tmp = File.createTempFile(DataSourceFromRAFTest.class.getSimpleName(), ".bin");
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(tmp, "rw");
        } finally {
            if (f == null) {
                tmp.delete();
            }
        }
        return CloseableWithDataSink.of(
                (RandomAccessFileDataSink) DataSinks.asDataSink(f),
                new DataSourceFromRAFTest.TmpFileCloseable(tmp, f));
    }

    @Override
    protected ByteBuffer getContents(RandomAccessFileDataSink dataSink) throws IOException {
        RandomAccessFile f = dataSink.getFile();
        if (f.length() > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + f.length());
        }
        byte[] contents = new byte[(int) f.length()];
        f.seek(0);
        f.readFully(contents);
        return ByteBuffer.wrap(contents);
    }
}
