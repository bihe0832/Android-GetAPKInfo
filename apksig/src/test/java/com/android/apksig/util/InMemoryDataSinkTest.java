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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link DataSink} returned by {@link DataSinks#newInMemoryDataSink()}.
 */
@RunWith(JUnit4.class)
public class InMemoryDataSinkTest extends DataSinkTestBase<ReadableDataSink> {

    @Override
    protected CloseableWithDataSink<ReadableDataSink> createDataSink() {
        return CloseableWithDataSink.of(DataSinks.newInMemoryDataSink());
    }

    @Override
    protected ByteBuffer getContents(ReadableDataSink dataSink) throws IOException {
        if (dataSink.size() > Integer.MAX_VALUE) {
            throw new IOException("Too much data: " + dataSink.size());
        }
        return dataSink.getByteBuffer(0, (int) dataSink.size());
    }

}
