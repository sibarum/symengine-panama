# symengine-panama

Panama (FFM) bindings for [SymEngine](https://github.com/symengine/symengine), the fast C++
symbolic algebra library. Hand-written downcalls over SymEngine's C API (`cwrapper.h`) — no
jextract, no JNI, no code generation.

```java
Expr x = Expr.symbol("x");
Expr.parse("1/3 + 1/6").mul(x).str();     // "(1/2)*x"
Expr.parse("1/0").str();                  // "zoo"  (complex infinity — the wheel's omega)
Expr.parse("x**2").diff(x).str();         // "2*x"
Expr.parse("8").log(Expr.parse("2"));     // log(8)/log(2)
```

`Expr` is an immutable, GC-managed (Cleaner) handle to a SymEngine `basic`; all operations are
thread-safe (the native library is built with `WITH_SYMENGINE_THREAD_SAFE`). Errors surface as
`SymEngineException` with the C API's error code.

## Building

The native library is built from source once, staged into `native/dist/bin`, and bundled into the
jar at package time (extracted to a temp dir and loaded at first use; override the location with
`-Dsymengine.native.dir=<dir>`):

```bash
./native/build.sh   # needs MSYS2 UCRT64: gcc, cmake, ninja, gmp
mvn install         # tests need --enable-native-access (surefire is configured)
```

Currently Windows x86-64 only; other platforms just need their `LOAD_ORDER` + a `build.sh` branch.
