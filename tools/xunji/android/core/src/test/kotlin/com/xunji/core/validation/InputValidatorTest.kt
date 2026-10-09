package com.xunji.core.validation

import org.junit.Assert.*
import org.junit.Test

class InputValidatorTest {
    @Test fun trimsDeckNamesAndRejectsEmpty() {
        assertEquals("英语",InputValidator.deckName("  英语  "))
        assertThrows(FieldValidationException::class.java) { InputValidator.deckName(" \n ") }
    }
    @Test fun deckNameUsesCodePoints() {
        assertEquals("😀".repeat(80),InputValidator.deckName("😀".repeat(80)))
        assertThrows(FieldValidationException::class.java) { InputValidator.deckName("a".repeat(81)) }
    }
    @Test fun frontAndBackMustNotBeBlank() {
        assertThrows(FieldValidationException::class.java) { InputValidator.cardContent(" ","answer","") }
        assertThrows(FieldValidationException::class.java) { InputValidator.cardContent("word","\n","") }
    }
    @Test fun permitsExactLimitsAndRejectsOneOver() {
        InputValidator.cardContent("😀".repeat(2000),"a".repeat(10000),"e".repeat(4000))
        assertThrows(FieldValidationException::class.java) { InputValidator.cardContent("a".repeat(2001),"a","") }
        assertThrows(FieldValidationException::class.java) { InputValidator.cardContent("a","a".repeat(10001),"") }
        assertThrows(FieldValidationException::class.java) { InputValidator.cardContent("a","a","e".repeat(4001)) }
    }
    @Test fun multilineAndCombiningCharactersAreValid() { InputValidator.cardContent("café\ne\u0301","第一行\n第二行","An example.") }
}
