package com.pagoflex.auth.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutValidatorTest {

    @Test
    void aceptaRutValidoConPuntosYGuion() {
        assertTrue(RutValidator.isValid("15.834.207-3"));
    }

    @Test
    void aceptaRutValidoConK() {
        assertTrue(RutValidator.isValid("16.248.317-K"));
    }

    @Test
    void aceptaRutValidoConKMinuscula() {
        assertTrue(RutValidator.isValid("16248317-k"));
    }

    @Test
    void aceptaRutConDvCero() {
        assertTrue(RutValidator.isValid("19.287.430-0"));
    }

    @Test
    void rechazaRutConDvIncorrecto() {
        assertFalse(RutValidator.isValid("15.834.207-4"));
    }

    @Test
    void rechazaRutVacio() {
        assertFalse(RutValidator.isValid(""));
    }

    @Test
    void rechazaRutNulo() {
        assertFalse(RutValidator.isValid(null));
    }

    @Test
    void rechazaRutConLetrasEnCuerpo() {
        assertFalse(RutValidator.isValid("15A34207-3"));
    }

    @Test
    void normalizaFormatoConPuntosYGuion() {
        assertEquals("15.834.207-3", RutValidator.normalize("158342073"));
    }

    @Test
    void normalizaDvAMayuscula() {
        assertEquals("16.248.317-K", RutValidator.normalize("16248317k"));
    }
}
