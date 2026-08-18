package sibarum.symengine;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Raw FFM downcalls onto symengine's C API (cwrapper.h). Every {@code basic} parameter is an opaque
 * {@code basic_struct*}; every function returning {@code CWRAPPER_OUTPUT_TYPE} returns a Java int
 * (0 = no exception). Lifetimes are managed by {@link Expr}, not here.
 */
final class CApi {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    static {
        NativeLib.ensureLoaded();
        LOOKUP = SymbolLookup.loaderLookup();
    }

    // lifecycle
    static final MethodHandle basic_new_heap = h("basic_new_heap", FunctionDescriptor.of(ADDRESS));
    static final MethodHandle basic_free_heap = h("basic_free_heap", FunctionDescriptor.ofVoid(ADDRESS));

    // construction
    static final MethodHandle basic_parse = h("basic_parse", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    static final MethodHandle symbol_set = h("symbol_set", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    static final MethodHandle basic_const_pi = h("basic_const_pi", FunctionDescriptor.ofVoid(ADDRESS));
    static final MethodHandle basic_const_E = h("basic_const_E", FunctionDescriptor.ofVoid(ADDRESS));
    static final MethodHandle basic_const_I = h("basic_const_I", FunctionDescriptor.ofVoid(ADDRESS));
    static final MethodHandle basic_const_complex_infinity =
            h("basic_const_complex_infinity", FunctionDescriptor.ofVoid(ADDRESS));
    static final MethodHandle basic_const_nan = h("basic_const_nan", FunctionDescriptor.ofVoid(ADDRESS));

    // arithmetic: (result, a, b)
    static final MethodHandle basic_add = h("basic_add", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    static final MethodHandle basic_sub = h("basic_sub", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    static final MethodHandle basic_mul = h("basic_mul", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    static final MethodHandle basic_div = h("basic_div", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
    static final MethodHandle basic_pow = h("basic_pow", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    // unary: (result, a)
    static final MethodHandle basic_neg = h("basic_neg", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    static final MethodHandle basic_log = h("basic_log", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    static final MethodHandle basic_expand = h("basic_expand", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    static final MethodHandle basic_diff = h("basic_diff", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    // printing
    static final MethodHandle basic_str = h("basic_str", FunctionDescriptor.of(ADDRESS, ADDRESS));
    static final MethodHandle basic_str_free = h("basic_str_free", FunctionDescriptor.ofVoid(ADDRESS));

    private static MethodHandle h(String name, FunctionDescriptor desc) {
        return LINKER.downcallHandle(
                LOOKUP.find(name).orElseThrow(() -> new SymEngineException("symbol not found: " + name)),
                desc);
    }

    private CApi() {
    }
}
