package org.openmbee.mms.view.controllers;

import java.util.ArrayList;
import java.util.List;
import org.openmbee.mms.core.objects.BaseResponse;
import org.openmbee.mms.json.MountJson;

public class MountsResponse extends BaseResponse<MountsResponse> {

    private List<MountJson> projects;

    public MountsResponse() {
        this.projects = new ArrayList<>();
    }

    public List<MountJson> getProjects() {
        return projects;
    }

    public MountsResponse setProjects(List<MountJson> projects) {
        this.projects = projects;
        return this;
    }
}
