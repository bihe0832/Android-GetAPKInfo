## 背景

### APK Signature Scheme v2官方介绍

Android 7.0 引入一项新的应用签名方案 APK Signature Scheme v2，它能提供更快的应用安装时间和更多针对未授权 APK 文件更改的保护。在默认情况下，Android Studio 2.2 和 Android Plugin for Gradle 2.2 会使用 APK Signature Scheme v2 和传统签名方案来签署您的应用。

如果您使用 APK Signature Scheme v2 签署您的应用，并对应用进行了进一步更改，则应用的签名将无效。出于这个原因，请在使用 APK Signature Scheme v2 签署您的应用之前、而非之后使用 zipalign 等工具。

**关于Android的APK Signature Scheme v2签名相关的资料汇总**:
[http://blog.bihe0832.com/android-v2.html](http://blog.bihe0832.com/android-v2.html)

**官方关于v2的详细介绍：[https://source.android.com/security/apksigning/v2.html](https://source.android.com/security/apksigning/v2.html)**

**个人关于V2签名以及V2签名引起的渠道打包失败分析的介绍：[http://blog.bihe0832.com/android-v2-signature.html](http://blog.bihe0832.com/android-v2-signature.html)**

### 特别说明

**这几天得到官方jarsiger的开发者Alex指点，发现其实官方已经提供了相关的打包工具以及检测方法，在此一并附上，后续再补充比较详细的介绍**。

#### 官方打包或者检查工具

- 工具位置：Android SDK包中`build-tools/<version>/apksigner`
- 支持版本：Android SDK Build Tools 24.0.3及以上
- 对应源码：
	
	- 官方地址：[https://android.googlesource.com/platform/tools/apksig](https://android.googlesource.com/platform/tools/apksig)
	- 个人github：项目根目录的apksig目录
- 使用方法：

		➜ $ANDROID_HOME/build-tools/24.0.3/apksigner
		USAGE: apksigner <command> [options]
		       apksigner --version
		       apksigner --help
		
		EXAMPLE:
		       apksigner sign --ks release.jks app.apk
		       apksigner verify --verbose app.apk
		
		apksigner is a tool for signing Android APK files and for checking whether
		signatures of APK files will verify on Android devices.
		
		
		        COMMANDS
		
		sign                  Sign the provided APK
		
		verify                Check whether the provided APK is expected to verify on
		                      Android
		
		version               Show this tool's version number and exit
		
		help                  Show this usage page and exit


## 代码调整

总体上是对Android的源码的移植，没有太多调整。主要调整的部分就是在`feedIntoMessageDigests `函数中计算md5的时候，为了提升效率，源码使用内存映射的方式，源码中是直接内存映射，代码迁移的时候调整为调用Java系统函数来完成内存映射。对应代码如下：


- AOSP:

		@Override
        public void feedIntoMessageDigests(
                MessageDigest[] mds, long offset, int size) throws IOException {
            // IMPLEMENTATION NOTE: After a lot of experimentation, the implementation of this
            // method was settled on a straightforward mmap with prefaulting.
            //
            // This method is not using FileChannel.map API because that API does not offset a way
            // to "prefault" the resulting memory pages. Without prefaulting, performance is about
            // 10% slower on small to medium APKs, but is significantly worse for APKs in 500+ MB
            // range. FileChannel.load (which currently uses madvise) doesn't help. Finally,
            // invoking madvise (MADV_SEQUENTIAL) after mmap with prefaulting wastes quite a bit of
            // time, which is not compensated for by faster reads.

            // We mmap the smallest region of the file containing the requested data. mmap requires
            // that the start offset in the file must be a multiple of memory page size. We thus may
            // need to mmap from an offset less than the requested offset.
            long filePosition = mFilePosition + offset;
            long mmapFilePosition =
                    (filePosition / MEMORY_PAGE_SIZE_BYTES) * MEMORY_PAGE_SIZE_BYTES;
            int dataStartOffsetInMmapRegion = (int) (filePosition - mmapFilePosition);
            long mmapRegionSize = size + dataStartOffsetInMmapRegion;
            long mmapPtr = 0;
            try {
                mmapPtr = OS.mmap(
                        0, // let the OS choose the start address of the region in memory
                        mmapRegionSize,
                        OsConstants.PROT_READ,
                        OsConstants.MAP_SHARED | OsConstants.MAP_POPULATE, // "prefault" all pages
                        mFd,
                        mmapFilePosition);
                // Feeding a memory region into MessageDigest requires the region to be represented
                // as a direct ByteBuffer.
                ByteBuffer buf = new DirectByteBuffer(
                        size,
                        mmapPtr + dataStartOffsetInMmapRegion,
                        mFd,  // not really needed, but just in case
                        null, // no need to clean up -- it's taken care of by the finally block
                        true  // read only buffer
                        );
                for (MessageDigest md : mds) {
                    buf.position(0);
                    md.update(buf);
                }
            } catch (ErrnoException e) {
                throw new IOException("Failed to mmap " + mmapRegionSize + " bytes", e);
            } finally {
                if (mmapPtr != 0) {
                    try {
                        OS.munmap(mmapPtr, mmapRegionSize);
                    } catch (ErrnoException ignored) {}
                }
            }
        }

- 修改后


 		@Override
        public void feedIntoMessageDigests(FileChannel channel,
                MessageDigest[] mds, long offset, int size) throws IOException {
            long filePosition = mFilePosition + offset;
            MappedByteBuffer inputBuffer = channel.map(FileChannel.MapMode.READ_ONLY, filePosition, size);// 读取大文件    
            for (MessageDigest md : mds) {
            	inputBuffer.position(0);
                md.update(inputBuffer);
            }
        }
        
## 使用事例

#### 查看帮助

	➜  java -jar CheckAndroidV2Signature.jar

	usage: java -jar ./CheckAndroidV2Signature.jar [--version] [--help] [filePath]
	
	such as:
	
		 java -jar ./CheckAndroidV2Signature.jar --version
		 java -jar ./CheckAndroidV2Signature.jar --help
		 java -jar ./CheckAndroidV2Signature.jar ./test.apk
	
	after check,the result will be a string json such as:
	
		 {"ret":0,"msg":"ok","isV2":true,"isV2OK":true}
	
		 ret: result code for check
	
			 0 : command exec succ
			 -1 : file not found
			 -2 : file not an Android APK file
			 -3 : check File signature error ,retry again
	
		 msg: result msg for check
		 isV2: whether the file is use Android-V2 signature or not
		 isV2OK: whether the file's Android-V2 signature is ok or not
			
			
#### 查看版本

	➜  java -jar ./CheckAndroidV2Signature.jar --version
		com.tencent.ysdk.CheckAndroidV2Signature version 1.0.1 (CheckAndroidV2Signature - 2)
		homepage : https://github.com/bihe0832/AndroidGetAPKInfo
		blog : http://blog.bihe0832.com
		github : https://github.com/bihe0832
		
#### 查看应用信息

	➜  java -jar ./CheckAndroidV2Signature.jar ./YSDK_Android_1.3.1_629-debug-ysdktest-inner.apk
	{"ret":0,"msg":"ok","isV2":false,"isV2OK":false}
		

