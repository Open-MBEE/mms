package org.openmbee.mms.federatedpersistence.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FederatedJsonUtils {

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper om) {
        this.objectMapper = om;
    }

    public Map<String, Object> convertToMap(Object obj) {
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }
}
