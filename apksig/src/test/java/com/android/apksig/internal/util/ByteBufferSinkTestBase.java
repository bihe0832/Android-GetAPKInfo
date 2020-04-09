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

import com.android.apksig.util.DataSinkTestBase;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class ByteBufferSinkTestBase extends DataSinkTestBase<ByteBufferSink> {

    private static final int START_POS = 100;

    protected abstract ByteBuffer createBuffer(int size);

    @Override
    protected CloseableWithDataSink<ByteBufferSink> createDataSink() {
        ByteBuffer buf = createBuffer(1024);
        // Use non-zero position and limit which isn't set to capacity to catch the implementation
        // under test ignoring the initial position.
        buf.position(START_POS);
        buf.limit(buf.capacity() - 300);
        return CloseableWithDataSink.of(new ByteBufferSink(buf));
    }

    @Override
    protected ByteBuffer getContents(ByteBufferSink dataSink) throws IOException {
        ByteBuffer buf = dataSink.getBuffer();
        int oldPos = buf.position();
        int oldLimit = buf.limit();
        try {
            buf.position(START_POS);
            buf.limit(oldPos);
            return buf.slice();
        } finally {
            buf.limit(oldLimit);
            buf.position(oldPos);
        }
    }
}
