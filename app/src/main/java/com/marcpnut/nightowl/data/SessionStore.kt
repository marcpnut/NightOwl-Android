package com.marcpnut.nightowl.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only the revocable server session token, encrypted with a device Keystore key. */
class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("nightowl-secure-session", Context.MODE_PRIVATE)

    fun load(): NightOwlSession? = try {
        val encrypted = preferences.getString(SESSION_KEY, null) ?: return null
        val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
        if (bytes.size <= IV_LENGTH) return null
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val payload = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val json = JSONObject(String(cipher.doFinal(payload), Charsets.UTF_8))
        NightOwlSession(json.getString("serverUrl"), json.getString("accessToken"), json.getString("deviceId"))
    } catch (_: Exception) { null }

    fun save(session: NightOwlSession) {
        val json = JSONObject().put("serverUrl", session.serverUrl)
            .put("accessToken", session.accessToken).put("deviceId", session.deviceId).toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        preferences.edit().putString(SESSION_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    fun clear() { preferences.edit().remove(SESSION_KEY).apply() }

    fun deviceId(): String {
        val existing = preferences.getString(DEVICE_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        preferences.edit().putString(DEVICE_KEY, created).apply()
        return created
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    private companion object {
        const val SESSION_KEY = "session-v1"
        const val DEVICE_KEY = "device-id-v1"
        const val KEY_ALIAS = "nightowl.session.encryption.v1"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
