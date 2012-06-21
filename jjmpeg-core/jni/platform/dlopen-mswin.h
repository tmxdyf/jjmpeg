// Windows dynamic linkage stuff
#include <windows.h>

#define _TOSTR(x) #x
#define TOSTR(x) _TOSTR(x)

#define DLOPEN(x, lib, ver) do { x = LoadLibrary(lib "-"  _TOSTR(ver) ".dll"); if (x == NULL) { fprintf(stderr, "cannot open %s\n",  lib "-" TOSTR(ver) ".dll"); fflush(stderr); return -1; } } while(0)
#define CALLDL(x) (*d ## x)
#define MAPDL(x, lib) do { if ((d ## x = (void *)GetProcAddress(lib, #x)) == NULL) { fprintf(stderr, "cannot resolve %s\n", #x); fflush(stderr); return -1; } } while(0)
