package com.agyaccess.app.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire cryptographique adossé au Hardware Android Keystore (AES-256-GCM).
 *
 * - Génère et stocke la clé secrète uniquement dans le module matériel (StrongBox
 *   si disponible sur l'appareil, sinon TEE Hardware Keystore).
 * - Utilise un IV aléatoire de 12 octets généré par le Keystore à chaque chiffrement
 *   (`setRandomizedEncryptionRequired(true)`) et un tag d'authentification GCM de 128 bits.
 * - La clé n'est jamais extractible en mémoire applicative.
 */
@Singleton
class KeystoreCryptoManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }
        return generateHardwareKey(useStrongBox = true)
    }

    private fun generateHardwareKey(useStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)

        if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        return try {
            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        } catch (_: StrongBoxUnavailableException) {
            if (useStrongBox) {
                // Repli transparent vers le TEE Hardware Keystore si StrongBox est absent
                generateHardwareKey(useStrongBox = false)
            } else {
                throw IllegalStateException("Impossible d'initialiser la clé Android Keystore.")
            }
        }
    }

    /**
     * Chiffre une chaîne UTF-8 et retourne Base64([ivLength:1B][iv:12B][ciphertext+GCMTag]).
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val payload = ByteBuffer.allocate(1 + iv.size + ciphertext.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()

        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * Déchiffre une chaîne produite par [encrypt].
     */
    fun decrypt(encodedPayload: String): String {
        if (encodedPayload.isEmpty()) return ""
        val raw = Base64.decode(encodedPayload, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(raw)

        val ivLength = buffer.get().toInt() and 0xFF
        require(ivLength in 12..16 && raw.size > 1 + ivLength) {
            "Payload chiffré corrompu ou IV invalide."
        }

        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), gcmSpec)

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** Supprime la clé du Keystore (utile lors d'une réinitialisation de sécurité). */
    @Synchronized
    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "agy_access_master_aes256_gcm_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
