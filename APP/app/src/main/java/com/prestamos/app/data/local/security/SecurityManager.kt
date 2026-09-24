package com.prestamos.app.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class SecurityManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secret_prestamos_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Obtiene o genera la clave cifrada de base de datos SQLCipher.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val keyString = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        return if (keyString != null) {
            Base64.getDecoder().decode(keyString)
        } else {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            val encoded = Base64.getEncoder().encodeToString(randomBytes)
            encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
            randomBytes
        }
    }

    fun isPinCreated(): Boolean {
        return encryptedPrefs.getString(KEY_PIN_HASH, null) != null
    }

    fun savePin(pin: String) {
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        encryptedPrefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = encryptedPrefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        return savedHash == hash
    }

    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    private fun getOrCreateSalt(): String {
        var salt = encryptedPrefs.getString(KEY_PIN_SALT, null)
        if (salt == null) {
            val saltBytes = ByteArray(16)
            SecureRandom().nextBytes(saltBytes)
            salt = Base64.getEncoder().encodeToString(saltBytes)
            encryptedPrefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt!!
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val digest = md.digest(pin.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "db_passphrase_key"
        private const val KEY_PIN_HASH = "user_pin_hash"
        private const val KEY_PIN_SALT = "user_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
