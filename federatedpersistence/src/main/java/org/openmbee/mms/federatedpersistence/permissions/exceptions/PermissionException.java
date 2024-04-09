package org.openmbee.mms.federatedpersistence.permissions.exceptions;

import org.openmbee.mms.core.exceptions.MMSException;
import org.springframework.http.HttpStatus;

public class PermissionException extends MMSException {
    public PermissionException(HttpStatus status, Object body) {
        super(status, body);
    }
}