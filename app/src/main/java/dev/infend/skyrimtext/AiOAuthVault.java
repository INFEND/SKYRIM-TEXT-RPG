package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted OAuth tokens. Nothing sensitive goes to exports, logs, or the Git repo. */
final class AiOAuthVault {
    private static final String ALIAS = "skyrim_chatgpt_oauth_v1";
    private static final String PREFS = "skyrim_chatgpt_oauth";
    private AiOAuthVault(){}

    static synchronized String hostId(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = p.getString("host_id","");
        if (value.isEmpty()) {
            value = "urn:uuid:" + UUID.randomUUID();
            p.edit().putString("host_id",value).commit();
        }
        return value;
    }

    private static SecretKey key(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey)store.getKey(ALIAS,null);
        if (!create) return null;
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return gen.generateKey();
    }

    static synchronized JSONObject load(Context context) throws Exception {
        SharedPreferences p = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String saved = p.getString("encrypted","");
        if (saved.isEmpty()) return new JSONObject();
        SecretKey secret = key(false);
        if (secret == null) throw new IllegalStateException("Не найден защищённый ключ OAuth");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,secret,new GCMParameterSpec(128,
                Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
        byte[] bytes = cipher.doFinal(Base64.decode(saved,Base64.NO_WRAP));
        return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
    }

    static synchronized void save(Context context,JSONObject record) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,key(true));
        byte[] encrypted = cipher.doFinal(record.toString().getBytes(StandardCharsets.UTF_8));
        boolean ok = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString("encrypted",Base64.encodeToString(encrypted,Base64.NO_WRAP))
                .putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                .commit();
        if (!ok) throw new IllegalStateException("Не удалось сохранить OAuth-сессию");
    }

    static synchronized void clear(Context context) {
        // Keep the stable host ID when signing out on this device.
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .remove("encrypted").remove("iv").commit();
    }
}
