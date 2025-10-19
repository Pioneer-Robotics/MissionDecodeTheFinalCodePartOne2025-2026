package pioneer.helpers

// Note: coeffs are least to most significant
class Polynomial(coeffs: Array<Double>) {
    val coefficients = coeffs

    companion object {
        /**
         * Adds multiple polynomials together.
         * @param polynomials Array of polynomials to add
         * @return New polynomial representing the sum
         */
        fun add(vararg polynomials: Polynomial): Polynomial {
            if (polynomials.isEmpty()) return Polynomial(arrayOf(0.0))
            
            val maxLength = polynomials.maxOf { it.coefficients.size }
            val result = DoubleArray(maxLength) { 0.0 }
            
            polynomials.forEach { poly ->
                poly.coefficients.forEachIndexed { index, coeff ->
                    result[index] += coeff
                }
            }
            
            return Polynomial(result.toTypedArray())
        }
    }

    // --- Basic operations ---

    /**
     * Adds this polynomial with another polynomial.
     */
    operator fun plus(other: Polynomial): Polynomial {
        return add(this, other)
    }

    // Evaluates as a nested polynomial
    fun eval(x: Double, coeffs: Array<Double> = coefficients) : Double {
        var value = 0.0
        for (coeff in coeffs.reversed()) {
            value = coeff + (x*value)
        }
        return value
    }

    // --- Transformations ---

    fun vShift(a: Double): Polynomial {
        val newCoeffs = coefficients.copyOf()
        newCoeffs[0] += a
        return Polynomial(newCoeffs)
    }

    fun vRef(): Polynomial {
        val newCoeffs = Array(coefficients.size) {0.0}
        for (i in coefficients.indices) {
            newCoeffs[i] = -coefficients[i]
        }
        return Polynomial(newCoeffs)
    }

    fun vScale(a: Double) : Polynomial {
        val newCoeffs = Array(coefficients.size) {0.0}
        for (i in coefficients.indices) {
            newCoeffs[i] = coefficients[i] * a
        }
        return Polynomial(newCoeffs)
    }

//    fun hShift(a: Double): Polynomial {
//
//    }

    fun square() : Polynomial {
        val newCoeffs = Array(coefficients.size * 2 - 1) {0.0}
        for (i in coefficients.indices) {
            for (j in coefficients.indices) {
                val newIndex = i+j // Index is essentially equivalent to degree
                val newCoeff = coefficients[i] * coefficients[j]
                newCoeffs[newIndex] += newCoeff
            }
        }
        return Polynomial(newCoeffs)
    }

    // --- Calculus ---

    fun derivative() : Polynomial {
        val der = Array(coefficients.size - 1) {0.0}
        for (i in 1..<coefficients.size) {
            der[i - 1] = coefficients[i] * i
        }
        return Polynomial(der)
    }

    fun antiDerivative() : Polynomial {
        val antiDer = Array<Double>(coefficients.size + 1) {0.0}
        for (i in coefficients.indices) {
            antiDer[i + 1] = coefficients[i] / (i+1)
        }
        return Polynomial(antiDer)
    }

    fun derEval(x: Double) : Double {
        return derivative().eval(x)
    }

    fun finiteInt(x1: Double, x2: Double) : Double {
        val antiDer = antiDerivative()
        return antiDer.eval(x2) - antiDer.eval(x1)
    }

    // Evaluate nth derivative at x
    fun nDerEval(x: Double, n: Int) : Double {
        var der = this
        for (i in 1..n) {
            der = der.derivative()
        }
        return der.eval(x)
    }

}