package org.openmbee.mms.core.services;

import org.openmbee.mms.json.ElementJson;

import java.time.Instant;
import java.util.Map;

public interface NodeChangeInfo extends NodeGetInfo {

    Instant getInstant();

    void setInstant(Instant instant);

    Map<String, ElementJson> getUpdatedMap();

    NodeChangeInfo setUpdatedMap(Map<String, ElementJson> updatedMap);

    Map<String, ElementJson> getDeletedMap();

    NodeChangeInfo setDeletedMap(Map<String, ElementJson> deletedMap);

    boolean getPreserveTimestamps();

    NodeChangeInfo setPreserveTimestamps(boolean preserveTimestamps);

    boolean getOverwrite();

    NodeChangeInfo setOverwrite(boolean overwrite);
}
