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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link DataSource} returned by {@link DataSources#asDataSource(ByteBuffer)}.
 */
@RunWith(JUnit4.class)
public class DataSourceFromByteBufferTest extends DataSourceTestBase {

    @Test
    public void testChangesToBufferPosAndLimitNotVisible() throws Exception {
        ByteBuffer buf = ByteBuffer.wrap("abcdefgh".getBytes(StandardCharsets.UTF_8));
        buf.position(1);
        buf.limit(4);
        DataSource ds = DataSources.asDataSource(buf);
        buf.position(2);
        buf.limit(buf.capacity());
        assertGetByteBufferEquals("bcd", ds, 0, (int) ds.size());
        assertFeedEquals("bcd", ds, 0, (int) ds.size());
        assertSliceEquals("bcd", ds, 0, (int) ds.size());
        assertCopyToEquals("bcd", ds, 0, (int) ds.size());
    }

    @Override
    protected CloseableWithDataSource createDataSource(byte[] contents) throws IOException {
        return CloseableWithDataSource.of(DataSources.asDataSource(ByteBuffer.wrap(contents)));
    }
}
