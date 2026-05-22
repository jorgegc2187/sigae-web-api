package com.sigae.api.service;

import com.sigae.api.config.SecurityProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MfaSecretCipher {

  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecureRandom secureRandom = new SecureRandom();
  private final SecretKeySpec keySpec;

  public MfaSecretCipher(SecurityProperties securityProperties) {
    byte[] key = Base64.getDecoder().decode(securityProperties.mfa().encryptionKey());
    if (key.length != 32) {
      throw new IllegalStateException("SIGAE_MFA_ENCRYPTION_KEY debe ser una clave Base64 de 32 bytes.");
    }
    this.keySpec = new SecretKeySpec(key, "AES");
  }

  public String encrypt(String value) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

      ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
      buffer.put(iv);
      buffer.put(cipherText);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("No se pudo cifrar el secreto 2FA.", exception);
    }
  }

  public String decrypt(String encryptedValue) {
    try {
      byte[] payload = Base64.getUrlDecoder().decode(encryptedValue);
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] iv = new byte[IV_LENGTH_BYTES];
      buffer.get(iv);
      byte[] cipherText = new byte[buffer.remaining()];
      buffer.get(cipherText);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalStateException("No se pudo descifrar el secreto 2FA.", exception);
    }
  }
}
