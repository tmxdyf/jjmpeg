/*
 * Included before anything else
 */
#define  LOGI(...)  printf(__VA_ARGS__)

/* ********************************************************************** */
#include "dlopen-linux.h"

#define PLATFORM_BITS 32

/* ********************************************************************** */
#define HAVE_LOCK_CB 1

#define ENABLE_DL 1
#define HAVE_AVDEVICE 1
