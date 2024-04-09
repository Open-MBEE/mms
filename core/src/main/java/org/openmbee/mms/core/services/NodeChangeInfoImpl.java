package org.openmbee.mms.core.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.openmbee.mms.json.ElementJson;

public class NodeChangeInfoImpl extends NodeGetInfoImpl implements NodeChangeInfo {

    private Map<String, ElementJson> updatedMap;

    private Map<String, ElementJson> deletedMap;

    private Instant instant;

    private boolean overwrite;

    private boolean preserveTimestamps;

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Map<String, ElementJson> getUpdatedMap() {
        if(this.updatedMap == null){
            this.updatedMap = new HashMap<String, ElementJson>();
        }
        return updatedMap;
    }

    @Override
    public NodeChangeInfo setUpdatedMap(Map<String, ElementJson> updatedMap) {
        this.updatedMap = updatedMap;
        return this;
    }

    @Override
    public Map<String, ElementJson> getDeletedMap() {
        return deletedMap;
    }

    @Override
    public NodeChangeInfo setDeletedMap(Map<String, ElementJson> deletedMap) {
        this.deletedMap = deletedMap;
        return this;
    }

    @Override
    public boolean getOverwrite() {
        return overwrite;
    }

    @Override
    public NodeChangeInfo setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    @Override
    public boolean getPreserveTimestamps() {
        return preserveTimestamps;
    }

    @Override
    public NodeChangeInfo setPreserveTimestamps(boolean preserveTimestamps) {
        this.preserveTimestamps = preserveTimestamps;
        return this;
    }

}
