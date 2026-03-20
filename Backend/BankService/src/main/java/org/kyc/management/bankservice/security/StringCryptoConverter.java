package org.kyc.management.bankservice.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that transparently encrypts String fields on write and decrypts on read
 * using AES-256-GCM via EncryptionUtil.
 */
@Converter
public class StringCryptoConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return EncryptionUtil.getInstance().encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return EncryptionUtil.getInstance().decrypt(dbData);
  }
}
