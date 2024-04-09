package org.openmbee.mms.crud.domain;

import org.openmbee.mms.json.ElementJson;

import java.util.*;

public class JsonDomain {

    public static List<ElementJson> filter(List<String> ids, List<ElementJson> orig) {
        Map<String, ElementJson> map = convertJsonToMap(orig);
        List<ElementJson> ret = new ArrayList<>();
        for (String id: ids) {
            if (map.containsKey(id)) {
                ret.add(map.get(id));
            }
        }
        return ret;
    }
    public static Map<String, ElementJson> convertJsonToMap(Collection<ElementJson> elements) {
        if (elements == null || elements.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ElementJson> result = new HashMap<>();

        for (ElementJson elem : elements) {
            if (elem == null) {
                continue;
            }
            if (elem.getId() != null && !elem.getId().isEmpty()) {
                result.put(elem.getId(), elem);
            }
        }
        return result;
    }

}
