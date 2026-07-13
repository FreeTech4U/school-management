package com.schoolsaas.common.entity;


import com.schoolsaas.common.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertit l'enum Gender vers la colonne CHAR(1) students.gender ('M' / 'F').
 *
 * @Enumerated(EnumType.STRING) ne convient pas ici : il écrirait "MALE" et
 * "FEMALE", or la colonne ne fait qu'un caractère et la contrainte CHECK
 * n'accepte que 'M' ou 'F'.
 *
 * autoApply = true : la conversion s'applique automatiquement à tout champ de
 * type Gender, sans avoir à annoter chaque entité.
 */
@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return gender == null ? null : gender.getCode();
    }

    @Override
    public Gender convertToEntityAttribute(String code) {
        return Gender.fromCode(code);
    }
}
