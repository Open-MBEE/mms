package org.openmbee.mms.core.utils;

import org.openmbee.mms.json.ElementJson;

public interface ElementUtils {
    Enum<? extends Object> getNodeType(ElementJson e);
}