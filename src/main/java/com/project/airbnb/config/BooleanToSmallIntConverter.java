package com.project.airbnb.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps Java Boolean ↔ PostgreSQL SMALLINT (0/1).
 * Production database uses SMALLINT for booleans; this avoids the
 * "column is of type smallint but expression is of type boolean" error.
 */
@Converter(autoApply = true)
public class BooleanToSmallIntConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData != 0;
    }
}
