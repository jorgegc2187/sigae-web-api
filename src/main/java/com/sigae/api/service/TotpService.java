package com.sigae.api.service;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TotpService {

  private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private static final int SECRET_LENGTH_BYTES = 20;
  private static final int TIME_STEP_SECONDS = 30;
  private static final int DIGITS = 6;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock;

  public TotpService(Clock clock) {
    this.clock = clock;
  }

  public String generateSecret() {
    byte[] bytes = new byte[SECRET_LENGTH_BYTES];
    secureRandom.nextBytes(bytes);
    return encodeBase32(bytes);
  }

  public String createOtpAuthUri(String issuer, String accountName, String secret) {
    String label = urlEncode(issuer + ":" + accountName);
    String encodedIssuer = urlEncode(issuer);
    return "otpauth://totp/" + label
        + "?secret=" + secret
        + "&issuer=" + encodedIssuer
        + "&algorithm=SHA1"
        + "&digits=" + DIGITS
        + "&period=" + TIME_STEP_SECONDS;
  }

  public boolean verify(String secret, String code) {
    if (code == null || !code.matches("\\d{6}")) {
      return false;
    }

    long currentCounter = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
    for (long counter = currentCounter - 1; counter <= currentCounter + 1; counter++) {
      if (code.equals(generateCode(secret, counter))) {
        return true;
      }
    }
    return false;
  }

  public String currentCode(String secret) {
    return generateCode(secret, clock.instant().getEpochSecond() / TIME_STEP_SECONDS);
  }

  private String generateCode(String secret, long counter) {
    try {
      byte[] key = decodeBase32(secret);
      byte[] counterBytes = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();

      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(counterBytes);

      int offset = hash[hash.length - 1] & 0x0f;
      int binary = ((hash[offset] & 0x7f) << 24)
          | ((hash[offset + 1] & 0xff) << 16)
          | ((hash[offset + 2] & 0xff) << 8)
          | (hash[offset + 3] & 0xff);

      int otp = binary % 1_000_000;
      return String.format("%06d", otp);
    } catch (InvalidKeyException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("No se pudo generar el código TOTP.", exception);
    }
  }

  private String encodeBase32(byte[] data) {
    StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
    int buffer = 0;
    int bitsLeft = 0;
    for (byte value : data) {
      buffer = (buffer << 8) | (value & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        result.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 31]);
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      result.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 31]);
    }
    return result.toString();
  }

  private byte[] decodeBase32(String secret) {
    String normalized = secret.replace("=", "").replace(" ", "").toUpperCase();
    int buffer = 0;
    int bitsLeft = 0;
    ByteBuffer output = ByteBuffer.allocate(normalized.length() * 5 / 8);

    for (char character : normalized.toCharArray()) {
      int value = base32Value(character);
      buffer = (buffer << 5) | value;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        output.put((byte) ((buffer >> (bitsLeft - 8)) & 0xff));
        bitsLeft -= 8;
      }
    }

    byte[] decoded = new byte[output.position()];
    output.flip();
    output.get(decoded);
    return decoded;
  }

  private int base32Value(char character) {
    if (character >= 'A' && character <= 'Z') {
      return character - 'A';
    }
    if (character >= '2' && character <= '7') {
      return character - '2' + 26;
    }
    throw new IllegalArgumentException("Secreto TOTP inválido.");
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
