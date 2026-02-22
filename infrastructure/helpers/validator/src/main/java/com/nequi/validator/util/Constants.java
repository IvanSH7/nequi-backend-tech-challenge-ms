package com.nequi.validator.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String REGULAR_PHRASE_NUMERIC = "^[0-9]+$";
    public static final String REGULAR_PHRASE_DATE = "^\\d{2}/\\d{2}/\\d{4}$";
    public static final String REGULAR_PHRASE_DEFAULT = "^[a-zA-Z0-9 .:()-]+$";

}
