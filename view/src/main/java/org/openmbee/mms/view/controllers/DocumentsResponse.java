package org.openmbee.mms.view.controllers;

import java.util.ArrayList;
import java.util.List;
import org.openmbee.mms.core.objects.BaseResponse;
import org.openmbee.mms.json.ElementJson;

public class DocumentsResponse extends BaseResponse<DocumentsResponse> {

    private List<ElementJson> documents;

    public DocumentsResponse() {
        this.documents = new ArrayList<>();
    }

    public List<ElementJson> getDocuments() {
        return documents;
    }

    public DocumentsResponse setDocuments(List<ElementJson> documents) {
        this.documents = documents;
        return this;
    }
}
