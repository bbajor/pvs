package de.bbajor.pvs.base.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SideOfEyeConverter implements AttributeConverter<SideOfEye, String> {

    @Override
    public String convertToDatabaseColumn(SideOfEye attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toDbString();
    }

    @Override
    public SideOfEye convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return SideOfEye.byDbString(dbData);
    }
}
