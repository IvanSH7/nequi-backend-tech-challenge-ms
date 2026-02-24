package com.nequi.dynamodb.util;

import com.nequi.model.enums.GeneralMessage;
import com.nequi.model.exception.TechnicalException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

    public static Throwable handleTechnicalError(Throwable error) {
        return new TechnicalException(error, GeneralMessage.INTERNAL_SERVER_ERROR);
    }

}
