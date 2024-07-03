package org.openmbee.mms.search.objects;

import java.util.Map;

public class BasicSearchRequest {

    private Map<String, String> params;
    private Map<String, String> recurse;
    private Integer from;
    private Integer size;

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getRecurse() {
        return recurse;
    }

    public void setRecurse(Map<String, String> recurse) {
        this.recurse = recurse;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

}
