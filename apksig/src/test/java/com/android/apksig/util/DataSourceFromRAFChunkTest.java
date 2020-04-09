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

import com.android.apksig.util.DataSourceFromRAFTest.TmpFileCloseable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for the {@link DataSource} returned by
 * {@link DataSources#asDataSource(RandomAccessFile, long, long)}.
 */
@RunWith(Parameterized.class)
public class DataSourceFromRAFChunkTest extends DataSourceTestBase {

    @Parameterized.Parameters(name = "{0}")
    public static DataSourceFromRAFFactory[] data() {
        return DataSourceFromRAFFactory.values();
    }

    @Parameterized.Parameter
    public DataSourceFromRAFFactory factory;

    @Test
    public void testFileSizeChangesNotVisible() throws Exception {
        try (CloseableWithDataSource c = createDataSource("abcdefg")) {
            DataSource ds = c.getDataSource();
            DataSource slice = ds.slice(3, 2);
            File f = ((TmpFileCloseable) c.getCloseable()).getFile();
            assertGetByteBufferEquals("abcdefg", ds, 0, (int) ds.size());
            assertGetByteBufferEquals("de", slice, 0, (int) slice.size());
            assertFeedEquals("cdefg", ds, 2, 5);
            assertFeedEquals("e", slice, 1, 1);
            assertCopyToEquals("cdefg", ds, 2, 5);
            assertCopyToEquals("e", slice, 1, 1);
            assertSliceEquals("cdefg", ds, 2, 5);
            assertSliceEquals("e", slice, 1, 1);
            try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                raf.seek(raf.length());
                raf.write("hijkl".getBytes(StandardCharsets.UTF_8));
            }

            assertGetByteBufferEquals("abcdefg", ds, 0, (int) ds.size());
            assertGetByteBufferEquals("de", slice, 0, (int) slice.size());
            assertGetByteBufferThrowsIOOB(ds, 0, (int) ds.size() + 3);
            assertFeedThrowsIOOB(ds, 0, (int) ds.size() + 3);
            assertSliceThrowsIOOB(ds, 0, (int) ds.size() + 3);
            assertCopyToThrowsIOOB(ds, 0, (int) ds.size() + 3);
        }
    }

    @Override
    protected CloseableWithDataSource createDataSource(byte[] contents) throws IOException {
        // "01" | contents | "9"
        byte[] fullContents = new byte[2 + contents.length + 1];
        fullContents[0] = '0';
        fullContents[1] = '1';
        System.arraycopy(contents, 0, fullContents, 2, contents.length);
        fullContents[fullContents.length - 1] = '9';

        File tmp = File.createTempFile(DataSourceFromRAFChunkTest.class.getSimpleName(), ".bin");
        RandomAccessFile f = null;
        try {
            Files.write(tmp.toPath(), fullContents);
            f = new RandomAccessFile(tmp, "r");
        } finally {
            if (f == null) {
                tmp.delete();
            }
        }

        return CloseableWithDataSource.of(
                factory.create(f, 2, contents.length),
                new DataSourceFromRAFTest.TmpFileCloseable(tmp, f));
    }
}
