/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.junit.Assert.assertArrayEquals;

import com.android.apksig.util.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FileChannelDataSourceTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testFeedsCorrectData_whenFilePartiallyReadFromBeginning() throws Exception {
        byte[] fullFileContent = createFileContent(1024 * 1024 + 987654);
        RandomAccessFile raf = createRaf(fullFileContent);
        DataSource rafDataSource = new FileChannelDataSource(raf.getChannel());

        ByteArrayDataSink dataSink = new ByteArrayDataSink();

        int bytesToFeed = 1024 * 1024 + 12345;
        rafDataSource.feed(0, bytesToFeed, dataSink);

        byte[] expectedBytes = Arrays.copyOf(fullFileContent, bytesToFeed);

        byte[] resultBytes = getDataSinkBytes(dataSink);

        assertArrayEquals(expectedBytes, resultBytes);
    }

    @Test
    public void testFeedsCorrectData_whenFilePartiallyReadWithOffset() throws Exception {
        byte[] fullFileContent = createFileContent(1024 * 1024 + 987654);
        RandomAccessFile raf = createRaf(fullFileContent);
        DataSource rafDataSource = new FileChannelDataSource(raf.getChannel());

        ByteArrayDataSink dataSink = new ByteArrayDataSink();

        int offset = 23456;
        int bytesToFeed = 1024 * 1024 + 12345;
        rafDataSource.feed(offset, bytesToFeed, dataSink);

        byte[] expectedBytes = Arrays.copyOfRange(fullFileContent, offset, offset + bytesToFeed);

        byte[] resultBytes = getDataSinkBytes(dataSink);

        assertArrayEquals(expectedBytes, resultBytes);
    }

    @Test
    public void testFeedsCorrectData_whenSeveralMbRead() throws Exception {
        byte[] fullFileContent = createFileContent(3 * 1024 * 1024 + 987654);
        RandomAccessFile raf = createRaf(fullFileContent);
        DataSource rafDataSource = new FileChannelDataSource(raf.getChannel());

        ByteArrayDataSink dataSink = new ByteArrayDataSink();

        int offset = 23456;
        int bytesToFeed = 2 * 1024 * 1024 + 12345;
        rafDataSource.feed(offset, bytesToFeed, dataSink);

        byte[] expectedBytes = Arrays.copyOfRange(fullFileContent, offset, offset + bytesToFeed);

        byte[] resultBytes = getDataSinkBytes(dataSink);

        assertArrayEquals(expectedBytes, resultBytes);
    }

    private byte[] getDataSinkBytes(ByteArrayDataSink dataSink) {
        ByteBuffer result = dataSink.getByteBuffer(0, (int)dataSink.size());
        byte[] resultBytes = new byte[result.limit()];
        result.get(resultBytes);
        return resultBytes;
    }

    private byte[] createFileContent(int fileSize) {
        byte[] fullFileContent = new byte[fileSize];
        for (int i = 0; i < fileSize; ++i) {
            fullFileContent[i] = (byte) (i % 255);
        }
        return fullFileContent;
    }

    private RandomAccessFile createRaf(byte[] content) throws Exception {
        File dataFile = temporaryFolder.newFile();

        try (FileOutputStream fos = new FileOutputStream(dataFile)) {
            fos.write(content);
        }
        return new RandomAccessFile(dataFile, "r");
    }
}
