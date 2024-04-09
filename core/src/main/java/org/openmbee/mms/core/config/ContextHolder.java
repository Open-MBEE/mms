package org.openmbee.mms.core.config;

public class ContextHolder {

    private static final ThreadLocal<ContextObject> contextHolder = new ThreadLocal<>();

    private ContextHolder() {
    }

    public static ContextObject getContext() {
        return contextHolder.get() != null ? contextHolder.get() : new ContextObject();
    }

    public static void setContext(String projectId) {
        setContext(projectId, getContext().getProjectId().equals(projectId) ? getContext().getBranchId() : Constants.MASTER_BRANCH);
    }

    public static void setContext(String projectId, String refId) {
        contextHolder.set(new ContextObject(projectId, refId));
    }

    public static void clearContext() {
        contextHolder.remove();
    }
}
