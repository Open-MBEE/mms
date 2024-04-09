package org.openmbee.mms.view.services;

import org.openmbee.mms.json.ElementJson;

public class PropertyData {


    private ElementJson typeJson;

    private ElementJson propertyJson;

    private ElementJson assocJson;

    private ElementJson assocPropertyJson;

    private boolean isView;


    public ElementJson getTypeJson() {
        return typeJson;
    }

    public void setTypeJson(ElementJson typeJson) {
        this.typeJson = typeJson;
    }

    public ElementJson getPropertyJson() {
        return propertyJson;
    }

    public void setPropertyJson(ElementJson propertyJson) {
        this.propertyJson = propertyJson;
    }

    public boolean isView() {
        return isView;
    }

    public void setView(boolean view) {
        isView = view;
    }

    public ElementJson getAssocJson() {
        return assocJson;
    }

    public void setAssocJson(ElementJson assocJson) {
        this.assocJson = assocJson;
    }


    public ElementJson getAssocPropertyJson() {
        return assocPropertyJson;
    }

    public void setAssocPropertyJson(ElementJson assocPropertyJson) {
        this.assocPropertyJson = assocPropertyJson;
    }
}
