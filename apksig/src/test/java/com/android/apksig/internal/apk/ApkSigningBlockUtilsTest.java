package com.android.apksig.internal.apk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.android.apksig.util.DataSource;
import com.android.apksig.util.DataSources;
import com.android.apksig.util.RunnablesExecutor;
import com.android.apksig.util.RunnablesProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ApkSigningBlockUtilsTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final int BASE = 255; // Intentionally not power of 2 to test properly

    DataSource[] dataSource;

    final Set<ContentDigestAlgorithm> algos = EnumSet.of(ContentDigestAlgorithm.CHUNKED_SHA512);

    @Before
    public void setUp() throws Exception {
        byte[] part1 = new byte[80 * 1024 * 1024 + 12345];
        for (int i = 0; i < part1.length; ++i) {
            part1[i] = (byte)(i % BASE);
        }

        File dataFile = temporaryFolder.newFile("fake.apk");

        try (FileOutputStream fos = new FileOutputStream(dataFile)) {
            fos.write(part1);
        }
        RandomAccessFile raf = new RandomAccessFile(dataFile, "r");

        byte[] part2 = new byte[1_500_000];
        for (int i = 0; i < part2.length; ++i) {
            part2[i] = (byte)(i % BASE);
        }
        byte[] part3 = new byte[30_000];
        for (int i = 0; i < part3.length; ++i) {
            part3[i] = (byte)(i % BASE);
        }
        dataSource = new DataSource[] {
                DataSources.asDataSource(raf),
                DataSources.asDataSource(ByteBuffer.wrap(part2)),
                DataSources.asDataSource(ByteBuffer.wrap(part3)),
        };
    }

    @Test
    public void testNewVersionMatchesOld() throws Exception {
        Map<ContentDigestAlgorithm, byte[]> outputContentDigestsOld =
                new EnumMap<>(ContentDigestAlgorithm.class);
        Map<ContentDigestAlgorithm, byte[]> outputContentDigestsNew =
                new EnumMap<>(ContentDigestAlgorithm.class);

        ApkSigningBlockUtils.computeOneMbChunkContentDigests(
                algos, dataSource, outputContentDigestsOld);

        ApkSigningBlockUtils.computeOneMbChunkContentDigests(
                RunnablesExecutor.SINGLE_THREADED,
                algos, dataSource, outputContentDigestsNew);

        assertEqualDigests(outputContentDigestsOld, outputContentDigestsNew);
    }

    @Test
    public void testMultithreadedVersionMatchesSinglethreaded() throws Exception {
        Map<ContentDigestAlgorithm, byte[]> outputContentDigests =
                new EnumMap<>(ContentDigestAlgorithm.class);
        Map<ContentDigestAlgorithm, byte[]> outputContentDigestsMultithreaded =
                new EnumMap<>(ContentDigestAlgorithm.class);

        ApkSigningBlockUtils.computeOneMbChunkContentDigests(
                RunnablesExecutor.SINGLE_THREADED,
                algos, dataSource, outputContentDigests);

        ApkSigningBlockUtils.computeOneMbChunkContentDigests(
                (RunnablesProvider provider) -> {
                    ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
                    int jobCount = forkJoinPool.getParallelism();
                    List<Future<?>> jobs = new ArrayList<>(jobCount);

                    for (int i = 0; i < jobCount; i++) {
                        jobs.add(forkJoinPool.submit(provider.createRunnable()));
                    }

                    try {
                        for (Future<?> future : jobs) {
                            future.get();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                },
                algos, dataSource, outputContentDigestsMultithreaded);

        assertEqualDigests(outputContentDigestsMultithreaded, outputContentDigests);
    }

    private void assertEqualDigests(
            Map<ContentDigestAlgorithm, byte[]> d1, Map<ContentDigestAlgorithm, byte[]> d2) {
        assertEquals(d1.keySet(), d2.keySet());
        for (ContentDigestAlgorithm algo : d1.keySet()) {
            byte[] digest1 = d1.get(algo);
            byte[] digest2 = d2.get(algo);
            assertArrayEquals(digest1, digest2);
        }
    }
}
