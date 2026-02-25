package com.nequi.model.exception;

import com.nequi.model.enums.GeneralMessage;
import lombok.Getter;

@Getter
public class TechnicalException extends ServiceException {

    public TechnicalException(GeneralMessage generalMessage) {
        super(generalMessage);
    }

    public TechnicalException(Throwable throwable, GeneralMessage generalMessage) {
        super(throwable, generalMessage);
    }
}
