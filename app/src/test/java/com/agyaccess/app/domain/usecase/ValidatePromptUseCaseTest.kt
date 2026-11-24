package com.agyaccess.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatePromptUseCaseTest {

    private val useCase = ValidatePromptUseCase()

    @Test
    fun `rejects blank prompt`() {
        val result = useCase("   \n\t  ")
        assertTrue(result is ValidatePromptUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `preserves shell characters literally as UTF-8 text without escaping`() {
        val input = "Explique `rm -rf /tmp/demo && echo \$HOME` sans l'exécuter"
        val result = useCase(input)
        assertTrue(result is ValidatePromptUseCase.ValidationResult.Valid)
        val valid = (result as ValidatePromptUseCase.ValidationResult.Valid).prompt
        assertEquals(input, valid.text)
    }

    @Test
    fun `strips NUL bytes and flags sensitive keywords`() {
        val result = useCase("Peux-tu redémarrer le service sur le NAS ?\u0000")
        assertTrue(result is ValidatePromptUseCase.ValidationResult.Valid)
        val valid = (result as ValidatePromptUseCase.ValidationResult.Valid).prompt
        assertFalse(valid.text.contains('\u0000'))
        assertTrue(valid.requiresSensitiveConfirmation)
    }

    @Test
    fun `rejects invalid model override containing injection characters`() {
        val result = useCase(
            rawPrompt = "Bonjour agy",
            rawModelOverride = "gpt-4; cat /etc/passwd"
        )
        assertTrue(result is ValidatePromptUseCase.ValidationResult.Invalid)
    }
}
