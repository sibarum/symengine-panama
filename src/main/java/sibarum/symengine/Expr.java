package sibarum.symengine;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;

/**
 * An immutable symbolic expression: a GC-managed handle to a SymEngine {@code basic}. Construction
 * goes through {@link #parse}, {@link #symbol}, or the named constants; everything else is derived by
 * the operator methods. All operations are thread-safe (the native library is built thread-safe, and
 * every operation writes to a fresh result handle).
 *
 * <pre>{@code
 * Expr x = Expr.symbol("x");
 * Expr e = Expr.parse("(1/3 + 1/6)").mul(x);   // -> x/2
 * String s = e.str();
 * }</pre>
 */
public final class Expr {

    private static final Cleaner CLEANER = Cleaner.create();

    final MemorySegment ptr;

    private Expr(MemorySegment ptr) {
        this.ptr = ptr;
        CLEANER.register(this, () -> free(ptr));
    }

    private static void free(MemorySegment ptr) {
        try {
            CApi.basic_free_heap.invokeExact(ptr);
        } catch (Throwable t) {
            throw new SymEngineException("basic_free_heap failed", t);
        }
    }

    private static Expr fresh() {
        try {
            return new Expr((MemorySegment) CApi.basic_new_heap.invokeExact());
        } catch (Throwable t) {
            throw new SymEngineException("basic_new_heap failed", t);
        }
    }

    private static void check(int code, String op) {
        if (code != 0) {
            throw new SymEngineException(code, op);
        }
    }

    /** Parse a SymEngine expression string, e.g. {@code "2*x + sin(pi/4)"}. Auto-simplifies. */
    public static Expr parse(String s) {
        Expr r = fresh();
        try (Arena a = Arena.ofConfined()) {
            check((int) CApi.basic_parse.invokeExact(r.ptr, a.allocateFrom(s)), "parse \"" + s + "\"");
        } catch (SymEngineException e) {
            throw e;
        } catch (Throwable t) {
            throw new SymEngineException("parse failed", t);
        }
        return r;
    }

    /** A free symbol, e.g. {@code symbol("x")}. */
    public static Expr symbol(String name) {
        Expr r = fresh();
        try (Arena a = Arena.ofConfined()) {
            check((int) CApi.symbol_set.invokeExact(r.ptr, a.allocateFrom(name)), "symbol " + name);
        } catch (SymEngineException e) {
            throw e;
        } catch (Throwable t) {
            throw new SymEngineException("symbol failed", t);
        }
        return r;
    }

    public static Expr pi() {
        return constant(CApi.basic_const_pi, "pi");
    }

    public static Expr e() {
        return constant(CApi.basic_const_E, "E");
    }

    public static Expr i() {
        return constant(CApi.basic_const_I, "I");
    }

    /** Complex (unsigned) infinity — the wheel's {@code 1/0}, printed as {@code zoo} by SymEngine. */
    public static Expr complexInfinity() {
        return constant(CApi.basic_const_complex_infinity, "complex_infinity");
    }

    /** Not-a-number — the wheel's bottom {@code 0/0}, printed as {@code nan} by SymEngine. */
    public static Expr nan() {
        return constant(CApi.basic_const_nan, "nan");
    }

    private static Expr constant(MethodHandle setter, String name) {
        Expr r = fresh();
        try {
            setter.invokeExact(r.ptr);
        } catch (Throwable t) {
            throw new SymEngineException("const " + name + " failed", t);
        }
        return r;
    }

    public Expr add(Expr o) {
        return binary(CApi.basic_add, "add", this, o);
    }

    public Expr sub(Expr o) {
        return binary(CApi.basic_sub, "sub", this, o);
    }

    public Expr mul(Expr o) {
        return binary(CApi.basic_mul, "mul", this, o);
    }

    public Expr div(Expr o) {
        return binary(CApi.basic_div, "div", this, o);
    }

    public Expr pow(Expr o) {
        return binary(CApi.basic_pow, "pow", this, o);
    }

    /** Derivative with respect to {@code sym} (a symbol). */
    public Expr diff(Expr sym) {
        return binary(CApi.basic_diff, "diff", this, sym);
    }

    public Expr neg() {
        return unary(CApi.basic_neg, "neg");
    }

    /** Natural logarithm. */
    public Expr log() {
        return unary(CApi.basic_log, "log");
    }

    /** Logarithm base {@code b}: {@code log(this) / log(b)}. */
    public Expr log(Expr b) {
        return log().div(b.log());
    }

    public Expr expand() {
        return unary(CApi.basic_expand, "expand");
    }

    private static Expr binary(MethodHandle op, String name, Expr a, Expr b) {
        Expr r = fresh();
        try {
            check((int) op.invokeExact(r.ptr, a.ptr, b.ptr), name);
        } catch (SymEngineException e) {
            throw e;
        } catch (Throwable t) {
            throw new SymEngineException(name + " failed", t);
        }
        return r;
    }

    private Expr unary(MethodHandle op, String name) {
        Expr r = fresh();
        try {
            check((int) op.invokeExact(r.ptr, this.ptr), name);
        } catch (SymEngineException e) {
            throw e;
        } catch (Throwable t) {
            throw new SymEngineException(name + " failed", t);
        }
        return r;
    }

    /** Canonical SymEngine string form, e.g. {@code "x/2"}, {@code "zoo"}, {@code "E**x"}. */
    public String str() {
        try {
            MemorySegment cstr = (MemorySegment) CApi.basic_str.invokeExact(ptr);
            try {
                return cstr.reinterpret(Long.MAX_VALUE).getString(0);
            } finally {
                CApi.basic_str_free.invokeExact(cstr);
            }
        } catch (Throwable t) {
            throw new SymEngineException("str failed", t);
        }
    }

    @Override
    public String toString() {
        return str();
    }
}
