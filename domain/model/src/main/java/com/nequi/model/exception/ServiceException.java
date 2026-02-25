package com.nequi.model.exception;

import com.nequi.model.enums.GeneralMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ServiceException extends RuntimeException {

    private final GeneralMessage generalMessage;

    public ServiceException(String message, GeneralMessage generalMessage){
        super(message);
        this.generalMessage = generalMessage;
    }

    public ServiceException(Throwable cause, GeneralMessage generalMessage) {
        super(cause);
        this.generalMessage = generalMessage;
    }
}
