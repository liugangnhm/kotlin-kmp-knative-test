cmake_minimum_required(VERSION 3.22.1)
project(demo_android C)

# Compile the same C source used by the Windows cinterop library.
set(DEMO_C_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../native-lib/src/nativeInterop/c")
add_library(demo_android SHARED
    src/main/cpp/demo_jni.c
    "${DEMO_C_DIR}/demo.c"
)
target_include_directories(demo_android PRIVATE "${DEMO_C_DIR}")
target_compile_features(demo_android PRIVATE c_std_11)
target_compile_options(demo_android PRIVATE -Wall -Wextra -Werror)
