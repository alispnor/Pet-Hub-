package com.alispnor.pethub.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String>, ApplicationContextAware {

    private static AesGcmCipher cipher;

    @Autowired
    public void setCipher(AesGcmCipher cipher) {
        EncryptedStringConverter.cipher = cipher;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (cipher == null) {
            cipher = applicationContext.getBean(AesGcmCipher.class);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher == null ? attribute : cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher == null ? dbData : cipher.decrypt(dbData);
    }
}
