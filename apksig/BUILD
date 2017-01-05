# Bazel (https://bazel.io/) BUILD file for apksig library and apksigner tool.

licenses(["notice"])  # Apache License 2.0

java_library(
    name = "apksig",
    srcs = glob([
        "src/main/java/**/*.java",
    ]),
    visibility = ["//visibility:public"],
)

java_binary(
    name = "apksigner",
    srcs = glob([
        "src/apksigner/java/**/*.java",
    ]),
    main_class = "com.android.apksigner.ApkSignerTool",
    resources = glob([
        "src/apksigner/java/**/*.txt",
    ]),
    visibility = ["//visibility:public"],
    deps = [":apksig"],
)

java_test(
    name = "ApkUtilsTest",
    srcs = [
        "src/test/java/com/android/apksig/apk/ApkUtilsTest.java",
    ],
    deps = [":apksig"],
)
