#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

# apksig library, for signing APKs and verifying signatures of APKs
# ============================================================
include $(CLEAR_VARS)
LOCAL_MODULE := apksig
LOCAL_SRC_FILES := $(call all-java-files-under, src/main/java)

# Disable warnnings about our use of internal proprietary OpenJDK API.
# TODO: Remove this workaround by moving to our own implementation of PKCS #7
# SignedData block generation, parsing, and verification.
LOCAL_JAVACFLAGS := -XDignore.symbol.file

include $(BUILD_HOST_JAVA_LIBRARY)


# apksigner command-line tool for signing APKs and verifying their signatures
# ============================================================
include $(CLEAR_VARS)
LOCAL_MODULE := apksigner
LOCAL_SRC_FILES := $(call all-java-files-under, src/apksigner/java)
LOCAL_JAVA_RESOURCE_DIRS = src/apksigner/java
LOCAL_JAR_MANIFEST := src/apksigner/apksigner.mf
LOCAL_STATIC_JAVA_LIBRARIES := apksig
# Output the apksigner.jar library
include $(BUILD_HOST_JAVA_LIBRARY)

# Output the shell script wrapper around the library
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := apksigner

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(HOST_OUT_JAVA_LIBRARIES)/apksigner$(COMMON_JAVA_PACKAGE_SUFFIX)
$(LOCAL_BUILT_MODULE): $(LOCAL_PATH)/etc/apksigner | $(ACP)
	@echo "Copy: $(PRIVATE_MODULE) ($@)"
	$(copy-file-to-new-target)
	$(hide) chmod 755 $@
