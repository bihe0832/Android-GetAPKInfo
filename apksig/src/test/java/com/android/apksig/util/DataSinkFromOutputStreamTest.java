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

import com.android.apksig.internal.util.OutputStreamDataSink;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link DataSink} returned by {@link DataSinks#asDataSink(java.io.OutputStream)}.
 */
@RunWith(JUnit4.class)
public class DataSinkFromOutputStreamTest extends DataSinkTestBase<OutputStreamDataSink> {

    @Override
    protected CloseableWithDataSink<OutputStreamDataSink> createDataSink() {
        return CloseableWithDataSink.of(
                (OutputStreamDataSink) DataSinks.asDataSink(new ByteArrayOutputStream()));
    }

    @Override
    protected ByteBuffer getContents(OutputStreamDataSink dataSink) throws IOException {
        return ByteBuffer.wrap(((ByteArrayOutputStream) dataSink.getOutputStream()).toByteArray());
    }
}
