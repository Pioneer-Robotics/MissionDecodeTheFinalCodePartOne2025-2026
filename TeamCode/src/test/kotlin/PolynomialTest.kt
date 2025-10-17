import pioneer.helpers.Polynomial
import org.junit.Assert.*
import org.junit.Test

class PolynomialTest {
    private val epsilon = 1e-6

    @Test
    fun testPolynomialConstruction() {
        val poly = Polynomial(arrayOf(1.0, 2.0, 3.0))
        assertNotNull(poly)
    }

    @Test
    fun testPolynomialEval() {
        // p(x) = 1 + 2x + 3x^2
        val poly = Polynomial(arrayOf(1.0, 2.0, 3.0))
        
        // p(0) = 1
        assertEquals(1.0, poly.eval(0.0), epsilon)
        
        // p(1) = 1 + 2 + 3 = 6
        assertEquals(6.0, poly.eval(1.0), epsilon)
        
        // p(2) = 1 + 4 + 12 = 17
        assertEquals(17.0, poly.eval(2.0), epsilon)
    }

    @Test
    fun testPolynomialDerEval() {
        // p(x) = 1 + 2x + 3x^2
        // p'(x) = 2 + 6x
        val poly = Polynomial(arrayOf(1.0, 2.0, 3.0))
        
        // p'(0) = 2
        assertEquals(2.0, poly.derEval(0.0), epsilon)
        
        // p'(1) = 2 + 6 = 8
        assertEquals(8.0, poly.derEval(1.0), epsilon)
    }

    @Test
    fun testPolynomialNDerEval() {
        // p(x) = 1 + 2x + 3x^2 + 4x^3
        val poly = Polynomial(arrayOf(1.0, 2.0, 3.0, 4.0))
        
        // 0th derivative is just the function
        assertEquals(poly.eval(1.0), poly.nDerEval(1.0, 0), epsilon)
        
        // 1st derivative: 2 + 6x + 12x^2
        // at x=1: 2 + 6 + 12 = 20
        assertEquals(20.0, poly.nDerEval(1.0, 1), epsilon)
        
        // 2nd derivative: 6 + 24x
        // at x=1: 6 + 24 = 30
        assertEquals(30.0, poly.nDerEval(1.0, 2), epsilon)
        
        // 3rd derivative: 24
        assertEquals(24.0, poly.nDerEval(1.0, 3), epsilon)
    }

    @Test
    fun testPolynomialVScale() {
        // p(x) = 1 + 2x + 3x^2
        val poly = Polynomial(arrayOf(1.0, 2.0, 3.0))
        val scaled = poly.vScale(2.0)
        
        // scaled(x) = 2 + 4x + 6x^2
        assertEquals(2.0, scaled.eval(0.0), epsilon)
        assertEquals(12.0, scaled.eval(1.0), epsilon)
    }

    @Test
    fun testPolynomialAdd() {
        val poly1 = Polynomial(arrayOf(1.0, 2.0, 3.0))
        val poly2 = Polynomial(arrayOf(4.0, 5.0, 6.0))
        
        val sum = Polynomial.add(poly1, poly2)
        
        // sum(x) = 5 + 7x + 9x^2
        assertEquals(5.0, sum.eval(0.0), epsilon)
        assertEquals(21.0, sum.eval(1.0), epsilon)
    }

    @Test
    fun testPolynomialAddMultiple() {
        val poly1 = Polynomial(arrayOf(1.0, 0.0))
        val poly2 = Polynomial(arrayOf(0.0, 2.0))
        val poly3 = Polynomial(arrayOf(0.0, 0.0, 3.0))
        
        val sum = Polynomial.add(poly1, poly2, poly3)
        
        // sum(x) = 1 + 2x + 3x^2
        assertEquals(1.0, sum.eval(0.0), epsilon)
        assertEquals(6.0, sum.eval(1.0), epsilon)
    }

    @Test
    fun testPolynomialZero() {
        val poly = Polynomial(arrayOf(0.0, 0.0, 0.0))
        
        assertEquals(0.0, poly.eval(0.0), epsilon)
        assertEquals(0.0, poly.eval(1.0), epsilon)
        assertEquals(0.0, poly.eval(100.0), epsilon)
    }

    @Test
    fun testPolynomialConstant() {
        val poly = Polynomial(arrayOf(5.0))
        
        assertEquals(5.0, poly.eval(0.0), epsilon)
        assertEquals(5.0, poly.eval(1.0), epsilon)
        assertEquals(5.0, poly.eval(100.0), epsilon)
        
        // Derivative of constant is zero
        assertEquals(0.0, poly.derEval(1.0), epsilon)
    }

    @Test
    fun testPolynomialLinear() {
        // p(x) = 2 + 3x
        val poly = Polynomial(arrayOf(2.0, 3.0))
        
        assertEquals(2.0, poly.eval(0.0), epsilon)
        assertEquals(5.0, poly.eval(1.0), epsilon)
        assertEquals(8.0, poly.eval(2.0), epsilon)
        
        // Derivative is constant: 3
        assertEquals(3.0, poly.derEval(0.0), epsilon)
        assertEquals(3.0, poly.derEval(100.0), epsilon)
    }
}
