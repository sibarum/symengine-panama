package sibarum.symengine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExprTest {

    @Test
    void arithmeticAutoSimplifies() {
        assertEquals("5", Expr.parse("2 + 3").str());
        assertEquals("(1/2)*x", Expr.parse("1/3 + 1/6").mul(Expr.symbol("x")).str());
    }

    @Test
    void wheelPoints() {
        // The wheel: 1/0 is unsigned infinity (SymEngine's zoo), 0/0 is bottom (SymEngine's nan).
        assertEquals("zoo", Expr.parse("1").div(Expr.parse("0")).str());
        assertEquals("zoo", Expr.parse("1/0").str());
        assertEquals("nan", Expr.parse("0").div(Expr.parse("0")).str());
        // Wheel identities: zoo absorbs finite addition; 0*zoo is bottom; 1/zoo is 0.
        assertEquals("zoo", Expr.parse("zoo + 5").str());
        assertEquals("nan", Expr.parse("0 * zoo").str());
        assertEquals("0", Expr.parse("1/zoo").str());
    }

    @Test
    void constants() {
        assertEquals("pi", Expr.pi().str());
        assertEquals("E", Expr.e().str());
        assertEquals("I", Expr.i().str());
        assertEquals("zoo", Expr.complexInfinity().str());
        assertEquals("-1", Expr.i().mul(Expr.i()).str());
        assertEquals("1", Expr.e().log().str());
    }

    @Test
    void logBase() {
        // log-base is a quotient of natural logs; SymEngine leaves the exact quotient symbolic.
        assertEquals("log(8)/log(2)", Expr.parse("8").log(Expr.parse("2")).str());
        assertEquals("log(100)/log(10)", Expr.parse("log(100, 10)").str());
    }

    @Test
    void diff() {
        Expr x = Expr.symbol("x");
        assertEquals("2*x", Expr.parse("x**2").diff(x).str());
    }
}
