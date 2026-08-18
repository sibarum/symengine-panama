package sibarum.symengine;

/** A SymEngine C-API call returned a nonzero {@code CWRAPPER_OUTPUT_TYPE}, or the library failed to load. */
public class SymEngineException extends RuntimeException {

    /** Mirrors cwrapper.h's CWRAPPER_OUTPUT_TYPE. */
    public enum Code {
        NO_EXCEPTION, RUNTIME_ERROR, DIV_BY_ZERO, NOT_IMPLEMENTED, DOMAIN_ERROR, PARSE_ERROR, SERIALIZATION_ERROR;

        static Code of(int c) {
            Code[] all = values();
            return c >= 0 && c < all.length ? all[c] : RUNTIME_ERROR;
        }
    }

    private final Code code;

    public SymEngineException(String message) {
        super(message);
        this.code = Code.RUNTIME_ERROR;
    }

    public SymEngineException(String message, Throwable cause) {
        super(message, cause);
        this.code = Code.RUNTIME_ERROR;
    }

    SymEngineException(int code, String op) {
        super(op + " failed: " + Code.of(code));
        this.code = Code.of(code);
    }

    public Code code() {
        return code;
    }
}
