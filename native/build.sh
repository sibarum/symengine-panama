#!/usr/bin/env bash
# Build SymEngine from source and stage it (plus its MinGW runtime deps) into native/dist/bin,
# which the Maven build bundles into the jar. Requires MSYS2 with the UCRT64 toolchain:
#   pacman -S mingw-w64-ucrt-x86_64-gcc mingw-w64-ucrt-x86_64-cmake mingw-w64-ucrt-x86_64-ninja mingw-w64-ucrt-x86_64-gmp
set -euo pipefail
cd "$(dirname "$0")"

MINGW=${MINGW:-/c/msys64/ucrt64}
export PATH="$MINGW/bin:$PATH"

if [ ! -d symengine ]; then
    git clone --depth 1 --branch v0.14.0 https://github.com/symengine/symengine.git symengine
fi

cmake -G Ninja -S symengine -B symengine/build \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DWITH_SYMENGINE_THREAD_SAFE=ON \
    -DBUILD_TESTS=OFF -DBUILD_BENCHMARKS=OFF \
    -DCMAKE_INSTALL_PREFIX="$(pwd)/dist"
cmake --build symengine/build
cmake --install symengine/build

# The MinGW runtime the DLL links against; bundled so consumers need no MSYS2 on PATH.
cp "$MINGW"/bin/{libwinpthread-1.dll,libgcc_s_seh-1.dll,libstdc++-6.dll,libgmp-10.dll} dist/bin/
echo "staged: $(ls dist/bin)"
