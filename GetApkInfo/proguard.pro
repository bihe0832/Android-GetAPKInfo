-injars       getPackageInfo-inner.jar  # 混淆前的文件
-outjars      getPackageInfo.jar		# 混淆后的文件

-libraryjars  <java.home>/lib/rt.jar	# Java运行时

#常规的代码混淆规则

-dontwarn org.**
-keep class org.** { *;}

-keep public class com.bihe0832.packageinfo.Main {
	public static void main(java.lang.String[]);
}
