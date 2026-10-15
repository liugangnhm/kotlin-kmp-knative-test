#ifndef DEMO_H
#define DEMO_H

/* Fixed-width integers keep the Kotlin/C boundary explicit. */
#include <stdint.h>

int64_t demo_sum(const int32_t *values, int32_t count);
const char *demo_backend(void);

#endif
