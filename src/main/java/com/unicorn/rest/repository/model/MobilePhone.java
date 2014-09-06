package com.unicorn.rest.repository.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import com.unicorn.rest.repository.exception.ValidationException;

import lombok.Getter;

@Getter
public class MobilePhone {

    @Nonnull private final PhoneNumber mobilePhone ;

    public MobilePhone(@Nullable String phone, @Nullable String region) throws ValidationException {
        try {
            this.mobilePhone = PhoneNumberUtil.getInstance().parse(phone, region);
        } catch (NumberParseException error) {
            throw new ValidationException(
                    String.format("Invalid mobile phone number %s with region %s: %s", phone, region, error.getMessage()));
        }
    }

    public MobilePhone(@Nullable PhoneNumber mobilePhone) throws ValidationException {
        if (mobilePhone == null) {
            throw new ValidationException("Invalid mobile phone number: null");
        }
        this.mobilePhone = mobilePhone;
    }

    public MobilePhone(@Nonnull Integer countryCode, @Nonnull Long phoneNumber) {
        this.mobilePhone = new PhoneNumber().setCountryCode(countryCode).setNationalNumber(phoneNumber);
    }

    public String toString(PhoneNumberFormat phoneNumberFormat) {
        return PhoneNumberUtil.getInstance().format(mobilePhone, phoneNumberFormat);
    }

    public String toString() {
        return toString(PhoneNumberFormat.INTERNATIONAL);
    }

    public Integer getCountryCode() {
        return this.mobilePhone.getCountryCode();
    }

    public Long getPhoneNumber() {
        return this.mobilePhone.getNationalNumber();
    }

    public CountryCodeSource getCountryCodeSource() {
        return this.mobilePhone.getCountryCodeSource();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MobilePhone other = (MobilePhone) obj;
        if (!getCountryCode().equals(other.getCountryCode()))
            return false;
        if (!getPhoneNumber().equals(other.getPhoneNumber()))
            return false;
        return true;
    }
}
