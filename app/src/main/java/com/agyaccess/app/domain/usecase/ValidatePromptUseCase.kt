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
        if (validatedModel != null && !MODEL_NAME_REGEX.matches(validatedModel)) {
            return ValidationResult.Invalid(
                "Nom de modèle invalide (caractères autorisés : a-z, A-Z, 0-9, ., _, -, :, /)."
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
                i += 1
            }
        }
        return true
    }

    companion object {
        const val MAX_PROMPT_LENGTH = 32_768
        private val MODEL_NAME_REGEX = Regex("^[a-zA-Z0-9._:/-]{1,128}$")

        // Heuristique déclenchant la confirmation BiometricPrompt lorsque l'option est active
        private val SENSITIVE_INTENT_PATTERNS = listOf(
            Regex("\\b(supprime|supprimer|efface|effacer|delete|remove|drop|truncate|purge|wipe)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(red[ée]marre|reboot|shutdown|restart|stop|kill|deploy|d[ée]ploie)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(mot de passe|password|token|secret|cl[ée] api|api key|credential|ssh|firewall|iptables|chmod|chown|sudo)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(production|prod|backup|restore|restaure|format)\\b", RegexOption.IGNORE_CASE)
        )
    }
}
