package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties({"empty"})
public class BaseJson<T> extends HashMap<String, Object> {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DOCID = "_docId";
    public static final String PROJECTID = "_projectId";
    public static final String REFID = "_refId";
    public static final String MODIFIER = "_modifier";
    public static final String MODIFIED = "_modified";
    public static final String CREATOR = "_creator";
    public static final String CREATED = "_created";
    public static final String COMMITID = "_commitId";
    public static final String TYPE = "type";
    public static final String IS_ARCHIVED = "archived";

    public String getId() {
        return (String) this.get(ID);
    }

    @SuppressWarnings("unchecked")
    public T setId(String id) {
        this.put(ID, id);
        return (T) this;
    }

    public String getType() {
        return (String) this.get(TYPE);
    }

    @SuppressWarnings("unchecked")
    public T setType(String type) {
        this.put(TYPE, type);
        return (T) this;
    }

    public String getName() {
        return (String) this.get(NAME);
    }

    @SuppressWarnings("unchecked")
    public T setName(String name) {
        this.put(NAME, name);
        return (T) this;
    }

    @JsonProperty(DOCID)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getDocId() {
        return (String) this.get(DOCID);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(DOCID)
    public T setDocId(String docId) {
        this.put(DOCID, docId);
        return (T) this;
    }

    @JsonProperty(PROJECTID)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getProjectId() {
        return (String) this.get(PROJECTID);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(PROJECTID)
    public T setProjectId(String projectId) {
        this.put(PROJECTID, projectId);
        return (T) this;
    }

    @JsonProperty(REFID)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getRefId() {
        return (String) this.get(REFID);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(REFID)
    public T setRefId(String refId) {
        this.put(REFID, refId);
        return (T) this;
    }

    @JsonProperty(MODIFIER)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getModifier() {
        return (String) this.get(MODIFIER);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(MODIFIER)
    public T setModifier(String modifier) {
        this.put(MODIFIER, modifier);
        return (T) this;
    }

    @JsonProperty(MODIFIED)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getModified() {
        return (String) this.get(MODIFIED);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(MODIFIED)
    public T setModified(String modified) {
        this.put(MODIFIED, modified);
        return (T) this;
    }

    @JsonProperty(CREATOR)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getCreator() {
        return (String) this.get(CREATOR);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(CREATOR)
    public T setCreator(String creator) {
        this.put(CREATOR, creator);
        return (T) this;
    }

    @JsonProperty(CREATED)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getCreated() {
        return (String) this.get(CREATED);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(CREATED)
    public T setCreated(String created) {
        this.put(CREATED, created);
        return (T) this;
    }

    @JsonProperty(COMMITID)
    @Schema(accessMode = AccessMode.READ_ONLY)
    public String getCommitId() {
        return (String) this.get(COMMITID);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(COMMITID)
    public T setCommitId(String commitId) {
        this.put(COMMITID, commitId);
        return (T) this;
    }

    @JsonProperty(IS_ARCHIVED)
    public Boolean isArchived() {
        return this.get(IS_ARCHIVED) == null ? null : (Boolean) this.get(IS_ARCHIVED);
    }

    @SuppressWarnings("unchecked")
    @JsonProperty(IS_ARCHIVED)
    public T setIsArchived(Boolean archived) {
        this.put(IS_ARCHIVED, archived);
        return (T) this;
    }

    public boolean isPartialOf(Map<String, Object> o) {
        return isPartial(this, o);
    }

    public void merge(Map<String, Object> o) {
        for (Map.Entry<String, Object> entry : o.entrySet()) {
            String attr = entry.getKey();
            if (!this.containsKey(attr)) {
                this.put(attr, o.get(attr));
            }
        }
    }

    private static boolean isPartial(Map<String, Object> map1, Map<String, Object> map2) {
        if (map1 == map2) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : map1.entrySet()) {
            if (!map2.containsKey(entry.getKey())) {
                return false;
            }
            Object value1 = entry.getValue();
            Object value2 = map2.get(entry.getKey());
            if (value1 == null && value2 != null) {
                return false;
            }
            if (value1 == value2) {
                continue;
            }
            if (value1 instanceof Map) {
                if (!(value2 instanceof Map)) {
                    return false;
                } else {
                    if (!isPartial((Map<String, Object>) value1, (Map<String, Object>) value2)) {
                        return false;
                    }
                }
            } else if (value1 instanceof List) {
                if (!(value2 instanceof List)) {
                    return false;
                } else {
                    if (!isPartial((List<Object>) value1, (List<Object>) value2)) {
                        return false;
                    }
                }
            } else if (value1 != null && !value1.equals(value2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPartial(List<Object> list1, List<Object> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null || list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            Map<String, Object> toTestMap = new HashMap<>();
            Map<String, Object> testAgainstMap = new HashMap<>();

            toTestMap.put("fromList", list1.get(i));
            testAgainstMap.put("fromList", list2.get(i));

            if (!isPartial(toTestMap, testAgainstMap)) {
                return false;
            }
        }
        return true;
    }
}
