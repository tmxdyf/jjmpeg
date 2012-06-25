// Use dynamic linkage on linux
#include <dlfcn.h>

#define DLOPEN(x, lib, ver) x = dlopen("lib" lib ".so", RTLD_LAZY|RTLD_GLOBAL); do { if (x == NULL) { fprintf(stderr, "cannot open %s\n", lib ".so"); fflush(stderr); return -1; } } while (0)
#define CALLDL(x) (*d ## x)
#define MAPDL(x, lib) do { if ((d ## x = dlsym(lib, #x)) == NULL) { fprintf(stderr, "cannot resolve %s\n", #x); fflush(stderr); return -1; } } while (0)