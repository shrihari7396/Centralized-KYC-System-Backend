package org.kyc.management.bankservice.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts String fields on write and decrypts on read
 * using AES-256-GCM via EncryptionUtil.
 */
@Converter
@Component
public class StringCryptoConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    public StringCryptoConverter(EncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionUtil.decrypt(dbData);
    }
}
