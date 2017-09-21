-optimizationpasses 5                   # 指定代码的压缩级别
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses        # 是否混淆第三方jar
-dontshrink                             # 不压缩输入的类文件
-dontoptimize                           # 不优化输入的类文件
-ignorewarning                          # 忽略警告，避免打包时某些警告出现
-verbose                                # 混淆时是否记录日志
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*    # 混淆时所采用的算法
-dontshrink                             # 不删除未引用的资源(类，方法等)

-libraryjars  <java.home>/lib/rt.jar	# Java运行时

-dontwarn org.**

#常规的代码混淆规则
-keep class android.** {*;}
-keep class com.android.** {*;}
-keep class org.** {*;}

-keep public class com.bihe0832.checksignature.CheckAndroidSignature {
	public static void main(java.lang.String[]);
}

-keep public class com.android.apksigner.ApkSignerTool {
	public static void main(java.lang.String[]);
}

-keep public class com.bihe0832.packageinfo.Main {
    public static void main(java.lang.String[]);
}
