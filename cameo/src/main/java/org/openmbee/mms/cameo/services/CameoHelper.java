package org.openmbee.mms.cameo.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openmbee.mms.cameo.CameoConstants;
import org.openmbee.mms.cameo.CameoNodeType;
import org.openmbee.mms.core.utils.ElementUtils;
import org.openmbee.mms.json.ElementJson;
import org.springframework.stereotype.Component;

@Component
public class CameoHelper implements ElementUtils {

    @Override
    public CameoNodeType getNodeType(ElementJson e) {
        if (isDocument(e)) {
            return CameoNodeType.DOCUMENT;
        }
        if (isView(e)) {
            return CameoNodeType.VIEW;
        }
        if (isGroup(e)) {
            return CameoNodeType.GROUP;
        }
        if (e.getId().contains("holding_bin") || e.getId().contains("view_instances_bin")) {
            return CameoNodeType.HOLDINGBIN;
        }
        String type = e.getType();
        if (type == null) {
            return CameoNodeType.ELEMENT;
        }
        switch (type) {
            case "Mount":
                return CameoNodeType.PROJECTUSAGE;
            case "InstanceSpecification":
                return CameoNodeType.INSTANCESPECIFICATION;
            case "Constraint":
                return CameoNodeType.CONSTRAINT;
            case "Package":
                return CameoNodeType.PACKAGE;
            case "Property":
                return CameoNodeType.PROPERTY;
            case "Parameter":
                return CameoNodeType.PARAMETER;
            default:
        }
        return CameoNodeType.ELEMENT;
    }

    public boolean isView(ElementJson e) {
        List<String> sids = (List)e.get(CameoConstants.APPLIEDSTEREOTYPEIDS);
        if (sids == null) {
            sids = (List)e.get(CameoConstants.OLDAPPLIEDSTEREOTYPEIDS);
        }
        if (sids != null && !sids.isEmpty()) {
            Set<String> ids = new HashSet<>(sids);
            ids.retainAll(CameoConstants.VIEWSIDS);
            return !ids.isEmpty();
        }
        return false;
    }

    public boolean isDocument(ElementJson e) {
        List<String> sids = (List)e.get(CameoConstants.APPLIEDSTEREOTYPEIDS);
        if (sids == null) {
            sids = (List)e.get(CameoConstants.OLDAPPLIEDSTEREOTYPEIDS);
        }
        if (sids != null && sids.contains(CameoConstants.DOCUMENTSID)) {
            return true;
        }
        return false;
    }

    public boolean isGroup(ElementJson e) {
        Boolean isGroup = (Boolean)e.get(CameoConstants.ISGROUP);
        if (isGroup != null) {
            return isGroup;
        }
        return false;
    }

    public ElementJson createProperty(String id, String name, String ownerId, String aggregation, String typeId, String assocId) {
        ElementJson res = new ElementJson();
        res.put(ElementJson.ID, id);
        res.put(CameoConstants.NAME, name);
        res.put(CameoConstants.NAMEEXPRESSION, null);
        res.put(CameoConstants.TYPE, "Property");
        res.put(CameoConstants.OWNERID, ownerId);
        res.put(CameoConstants.TYPEID, typeId);
        res.put(CameoConstants.AGGREGATION, aggregation);
        res.put(CameoConstants.ASSOCIATIONID, assocId);
        List<String> asIds = new ArrayList<>();
        res.put(CameoConstants.APPLIEDSTEREOTYPEIDS, asIds);
        res.put(CameoConstants.DOCUMENTATION, "");
        res.put(CameoConstants.MDEXTENSIONSIDS, new ArrayList());
        res.put(CameoConstants.SYNCELEMENTID, null);
        res.put(CameoConstants.CLIENTDEPENDENCYIDS, new ArrayList());
        res.put(CameoConstants.SUPPLIERDEPENDENCYIDS, new ArrayList());
        res.put(CameoConstants.VISIBILITY, "private");
        res.put(CameoConstants.ISLEAF, false);
        res.put(CameoConstants.ISSTATIC, false);
        res.put(CameoConstants.ISORDERED, false);
        res.put(CameoConstants.ISUNIQUE, true);
        res.put(CameoConstants.LOWERVALUE, null);
        res.put(CameoConstants.UPPERVALUE, null);
        res.put(CameoConstants.ISREADONLY, false);
        res.put(CameoConstants.TEMPLATEPARAMETERID, null);
        res.put(CameoConstants.ENDIDS, new ArrayList());
        res.put(CameoConstants.DEPLOYMENTIDS, new ArrayList());
        res.put(CameoConstants.ASSOCIATIONENDID, null);
        res.put(CameoConstants.QUALIFIERIDS, new ArrayList());
        res.put(CameoConstants.DATATYPEID, null);
        res.put(CameoConstants.DEFAULTVALUE, null);
        res.put(CameoConstants.INTERFACEID, null);
        res.put(CameoConstants.ISDERIVED, false);
        res.put(CameoConstants.ISDERIVEDUNION, false);
        res.put(CameoConstants.ISID, false);
        res.put(CameoConstants.REDEFINEDPROPERTYIDS, new ArrayList());
        res.put(CameoConstants.SUBSETTEDPROPERTYIDS, new ArrayList());
        return res;
    }

    public ElementJson createAssociation(String id, String ownerId, String ownedEnd, String memberEnd2) {
        ElementJson association = new ElementJson();
        List<String> memberEndIds = new ArrayList<>();
        memberEndIds.add(ownedEnd);
        memberEndIds.add(memberEnd2);
        List<String> ownedEndIds = new ArrayList<>();
        ownedEndIds.add(ownedEnd);

        association.put(ElementJson.ID, id);
        association.put(CameoConstants.NAME, "");
        association.put(CameoConstants.NAMEEXPRESSION, null);
        association.put(CameoConstants.TYPE, "Association");
        association.put(CameoConstants.OWNERID, ownerId);
        association.put(CameoConstants.MEMBERENDIDS, memberEndIds);
        association.put(CameoConstants.OWNEDENDIDS, ownedEndIds);
        // Default Fields
        association.put(CameoConstants.DOCUMENTATION, "");
        association.put(CameoConstants.MDEXTENSIONSIDS, new ArrayList());
        association.put(CameoConstants.SYNCELEMENTID, null);
        association.put(CameoConstants.APPLIEDSTEREOTYPEIDS, new ArrayList());
        association.put(CameoConstants.CLIENTDEPENDENCYIDS, new ArrayList());
        association.put(CameoConstants.SUPPLIERDEPENDENCYIDS, new ArrayList());
        association.put(CameoConstants.NAMEEXPRESSION, null);
        association.put(CameoConstants.VISIBILITY, "public");
        association.put(CameoConstants.TEMPLATEPARAMETERID, null);
        association.put(CameoConstants.ELEMENTIMPORTIDS, new ArrayList());
        association.put(CameoConstants.PACKAGEIMPORTIDS, new ArrayList());
        association.put(CameoConstants.ISLEAF, false);
        association.put(CameoConstants.TEMPLATEBINDINGIDS, new ArrayList());
        association.put(CameoConstants.USECASEIDS, new ArrayList());
        association.put(CameoConstants.REPRESENTATIONID, null);
        association.put(CameoConstants.COLLABORATIONUSEIDS, new ArrayList());
        association.put(CameoConstants.GENERALIZATIONIDS, new ArrayList());
        association.put(CameoConstants.POWERTYPEEXTENTIDS, new ArrayList());
        association.put(CameoConstants.ISABSTRACT, false);
        association.put(CameoConstants.ISFINALSPECIALIZATION, false);
        association.put(CameoConstants.REDEFINEDCLASSIFIERIDS, new ArrayList());
        association.put(CameoConstants.SUBSTITUTIONIDS, new ArrayList());
        association.put(CameoConstants.ISDERIVED, false);
        association.put(CameoConstants.NAVIGABLEOWNEDENDIDS, new ArrayList());
        return association;
    }
}
