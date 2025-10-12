package pioneer.helpers

class PolynomialUtils {
    companion object {

        fun addPolynomials(polynomials: Array<Polynomial>) : Polynomial {
            var longestCoeffs = 0
            for (poly in polynomials) { // Find longest array
                if (poly.coefficients.size > longestCoeffs) {
                    longestCoeffs = poly.coefficients.size
                }
            }

            var finalCoeffs = Array(longestCoeffs) {0.0}
            // Pad and sum coeffs
            for (index in polynomials.indices) {
                var coeffs = polynomials[index].coefficients
                while (coeffs.size < longestCoeffs) { // Pad coeffs
                    coeffs += 0.0
                }
                // Add coeffs into finalCoeffs
                finalCoeffs = finalCoeffs.zip(coeffs) { a, b -> a + b }.toTypedArray()
            }
            return Polynomial(finalCoeffs)
        }

    }
}