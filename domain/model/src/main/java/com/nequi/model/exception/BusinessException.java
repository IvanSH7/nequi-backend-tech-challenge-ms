package com.nequi.model.exception;

import com.nequi.model.enums.GeneralMessage;
import lombok.Getter;

@Getter
public class BusinessException extends ServiceException {

    public BusinessException(GeneralMessage generalMessage) {
        super(generalMessage);
    }

    public BusinessException(String message, GeneralMessage generalMessage) {
        super(message, generalMessage);
    }
}
