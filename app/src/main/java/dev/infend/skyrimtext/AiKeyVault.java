package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts the player's own API key with a non-exportable Android Keystore key. */
final class AiKeyVault {
    private static final String ALIAS = "skyrim_ai_api_v1";
    private static final String PREF = "skyrim_ai_vault";
    private AiKeyVault() {}

    static boolean configured(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains("data");
    }

    static void save(Context context, String token) throws Exception {
        if (token == null || token.trim().length() < 12 || token.trim().length() > 512)
            throw new IllegalArgumentException("Неверный формат ключа API");
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key;
        if (store.containsAlias(ALIAS)) {
            key = (SecretKey) store.getKey(ALIAS, null);
        } else {
            KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true).build());
            key = gen.generateKey();
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply();
    }

    static String read(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!prefs.contains("data")) return "";
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey(ALIAS, null);
        if (key == null) throw new IllegalStateException("Ключ хранилища не найден");
        byte[] iv = Base64.decode(prefs.getString("iv", ""), Base64.DEFAULT);
        byte[] ciphertext = Base64.decode(prefs.getString("data", ""), Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
