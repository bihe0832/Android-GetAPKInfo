-injars       getPackageInfo-inner.jar
-outjars      getPackageInfo.jar
-libraryjars  <java.home>/lib/rt.jar

-dontwarn org.**
-keep class org.** { *;}

-keep public class com.bihe0832.packageinfo.Main {
public static void main(java.lang.String[]);
}