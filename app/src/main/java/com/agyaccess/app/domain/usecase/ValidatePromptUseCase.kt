package com.agyaccess.app.domain.usecase

import javax.inject.Inject

/**
 * Validation stricte des prompts utilisateur (principe Zero Raw Command).
 *
 * L'application n'assemble ni n'échappe jamais de commandes shell/bash :
 * le prompt est traité uniquement comme une chaîne UTF-8 pure sérialisée
 * dans un DTO JSON typé. Ce cas d'usage garantit :
 *  1. L'intégrité UTF-8 (pas de surrogates orphelins, pas de NUL bytes ou contrôles non imprimables).
 *  2. Une borne stricte de taille pour éviter les dénis de service mémoire/réseau.
 *  3. Une détection heuristique d'instructions sensibles pour déclencher une
 *     confirmation biométrique optionnelle avant l'envoi.
 */
class ValidatePromptUseCase @Inject constructor() {

    data class ValidatedPrompt(
        val text: String,
        val modelOverride: String?,
        val requiresSensitiveConfirmation: Boolean
    )

    sealed interface ValidationResult {
        data class Valid(val prompt: ValidatedPrompt) : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    operator fun invoke(
        rawPrompt: String,
        rawModelOverride: String? = null
    ): ValidationResult {
        // Retire les caractères de contrôle dangereux (NUL, etc.) en préservant \n, \r, \t
        val sanitized = rawPrompt
            .replace("\u0000", "")
            .filter { ch -> ch == '\n' || ch == '\r' || ch == '\t' || !ch.isISOControl() }
            .trim()

        if (sanitized.isEmpty()) {
            return ValidationResult.Invalid("Le message ne peut pas être vide.")
        }

        if (sanitized.length > MAX_PROMPT_LENGTH) {
            return ValidationResult.Invalid(
                "Le message dépasse la limite autorisée ($MAX_PROMPT_LENGTH caractères)."
            )
        }

        if (!isWellFormedUtf16(sanitized)) {
            return ValidationResult.Invalid("Encodage UTF-8 invalide détecté dans le message.")
        }

        val validatedModel = rawModelOverride?.trim()?.takeIf { it.isNotEmpty() }
        if (validatedModel != null && !isValidModeOrModel(validatedModel)) {
            return ValidationResult.Invalid(
                "Mode ou modèle invalide (modes autorisés : auto, accept-edits, plan ; caractères autorisés : a-z, A-Z, 0-9, ., _, -, :, /)."
            )
        }

        val isSensitive = SENSITIVE_INTENT_PATTERNS.any { it.containsMatchIn(sanitized) }

        return ValidationResult.Valid(
            ValidatedPrompt(
                text = sanitized,
                modelOverride = validatedModel,
                requiresSensitiveConfirmation = isSensitive
            )
        )
    }

    private fun isValidModeOrModel(value: String): Boolean {
        if (ALLOWED_MODES.contains(value.lowercase())) return true
        return MODEL_NAME_REGEX.matches(value)
    }

    private fun isWellFormedUtf16(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= text.length || !Character.isLowSurrogate(text[i + 1])) {
                    return false
                }
                i += 2
            } else if (Character.isLowSurrogate(c)) {
                return false
            } else {
                i++
            }
        }
        return true
    }

    companion object {
        const val MAX_PROMPT_LENGTH = 100_000

        private val ALLOWED_MODES = setOf("auto", "accept-edits", "plan")
        private val MODEL_NAME_REGEX = Regex("^[A-Za-z0-9._\\-:/]{1,64}$")

        private val SENSITIVE_INTENT_PATTERNS = listOf(
            Regex("\\b(rm\\s+-rf|dd\\s+if=|mkfs|format|drop\\s+database|truncate\\s+table)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(shutdown|reboot|poweroff|init\\s+0|halt)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(iptables\\s+-F|ufw\\s+disable|setenforce\\s+0)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(chmod\\s+-R\\s+777|chown\\s+-R)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(supprime|efface|reinitialise|formate|arrete|eteins|redemarre)\\b", RegexOption.IGNORE_CASE)
        )
    }
}
