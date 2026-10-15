#include "demo.h"

int64_t demo_sum(const int32_t *values, int32_t count) {
    int64_t total = 0;
    for (int32_t i = 0; i < count; ++i) {
        total += values[i];
    }
    return total;
}

const char *demo_backend(void) {
#if defined(__ANDROID__)
    return "C shared library (Android NDK / JNI)";
#else
    return "C static library (MinGW x64)";
#endif
}
