package org.openmbee.mms.msosa.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openmbee.mms.msosa.MsosaConstants;
import org.openmbee.mms.msosa.MsosaNodeType;
import org.openmbee.mms.core.utils.ElementUtils;
import org.openmbee.mms.json.ElementJson;
import org.springframework.stereotype.Component;

@Component
public class MsosaHelper implements ElementUtils{

    @Override
    public MsosaNodeType getNodeType(ElementJson e) {
        if (isDocument(e)) {
            return MsosaNodeType.DOCUMENT;
        }
        if (isView(e)) {
            return MsosaNodeType.VIEW;
        }
        if (isGroup(e)) {
            return MsosaNodeType.GROUP;
        }
        if (e.getId().contains("holding_bin") || e.getId().contains("view_instances_bin")) {
            return MsosaNodeType.HOLDINGBIN;
        }
        String type = e.getType();
        if (type == null) {
            return MsosaNodeType.ELEMENT;
        }
        switch (type) {
            case "Mount":
                return MsosaNodeType.PROJECTUSAGE;
            case "InstanceSpecification":
                return MsosaNodeType.INSTANCESPECIFICATION;
            case "Constraint":
                return MsosaNodeType.CONSTRAINT;
            case "Package":
                return MsosaNodeType.PACKAGE;
            case "Property":
                return MsosaNodeType.PROPERTY;
            case "Parameter":
                return MsosaNodeType.PARAMETER;
            default:
        }
        return MsosaNodeType.ELEMENT;
    }

    public boolean isView(ElementJson e) {
        List<String> sids = (List)e.get(MsosaConstants.APPLIEDSTEREOTYPEIDS);
        if (sids != null && !sids.isEmpty()) {
            Set<String> ids = new HashSet<>(sids);
            ids.retainAll(MsosaConstants.VIEWSIDS);
            return !ids.isEmpty();
        }
        return false;
    }

    public boolean isDocument(ElementJson e) {
        List<String> sids = (List)e.get(MsosaConstants.APPLIEDSTEREOTYPEIDS);
        if (sids != null && sids.contains(MsosaConstants.DOCUMENTSID)) {
            return true;
        }
        return false;
    }

    public boolean isGroup(ElementJson e) {
        Boolean isGroup = (Boolean)e.get(MsosaConstants.ISGROUP);
        if (isGroup != null) {
            return isGroup;
        }
        return false;
    }

    public ElementJson createProperty(String id, String name, String ownerId, String aggregation, String typeId, String assocId) {
        ElementJson res = new ElementJson();
        res.put(ElementJson.ID, id);
        res.put(MsosaConstants.NAME, name);
        res.put(MsosaConstants.NAMEEXPRESSION, null);
        res.put(MsosaConstants.TYPE, "Property");
        res.put(MsosaConstants.OWNERID, ownerId);
        res.put(MsosaConstants.TYPEID, typeId);
        res.put(MsosaConstants.AGGREGATION, aggregation);
        res.put(MsosaConstants.ASSOCIATIONID, assocId);
        List<String> asIds = new ArrayList<>();
        res.put(MsosaConstants.APPLIEDSTEREOTYPEIDS, asIds);
        res.put(MsosaConstants.DOCUMENTATION, "");
        res.put(MsosaConstants.MDEXTENSIONSIDS, new ArrayList());
        res.put(MsosaConstants.SYNCELEMENTID, null);
        res.put(MsosaConstants.CLIENTDEPENDENCYIDS, new ArrayList());
        res.put(MsosaConstants.SUPPLIERDEPENDENCYIDS, new ArrayList());
        res.put(MsosaConstants.VISIBILITY, "private");
        res.put(MsosaConstants.ISLEAF, false);
        res.put(MsosaConstants.ISSTATIC, false);
        res.put(MsosaConstants.ISORDERED, false);
        res.put(MsosaConstants.ISUNIQUE, true);
        res.put(MsosaConstants.LOWERVALUE, null);
        res.put(MsosaConstants.UPPERVALUE, null);
        res.put(MsosaConstants.ISREADONLY, false);
        res.put(MsosaConstants.TEMPLATEPARAMETERID, null);
        res.put(MsosaConstants.ENDIDS, new ArrayList());
        res.put(MsosaConstants.DEPLOYMENTIDS, new ArrayList());
        res.put(MsosaConstants.ASSOCIATIONENDID, null);
        res.put(MsosaConstants.QUALIFIERIDS, new ArrayList());
        res.put(MsosaConstants.DATATYPEID, null);
        res.put(MsosaConstants.DEFAULTVALUE, null);
        res.put(MsosaConstants.INTERFACEID, null);
        res.put(MsosaConstants.ISDERIVED, false);
        res.put(MsosaConstants.ISDERIVEDUNION, false);
        res.put(MsosaConstants.ISID, false);
        res.put(MsosaConstants.REDEFINEDPROPERTYIDS, new ArrayList());
        res.put(MsosaConstants.SUBSETTEDPROPERTYIDS, new ArrayList());
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
        association.put(MsosaConstants.NAME, "");
        association.put(MsosaConstants.NAMEEXPRESSION, null);
        association.put(MsosaConstants.TYPE, "Association");
        association.put(MsosaConstants.OWNERID, ownerId);
        association.put(MsosaConstants.MEMBERENDIDS, memberEndIds);
        association.put(MsosaConstants.OWNEDENDIDS, ownedEndIds);
        // Default Fields
        association.put(MsosaConstants.DOCUMENTATION, "");
        association.put(MsosaConstants.MDEXTENSIONSIDS, new ArrayList());
        association.put(MsosaConstants.SYNCELEMENTID, null);
        association.put(MsosaConstants.APPLIEDSTEREOTYPEIDS, new ArrayList());
        association.put(MsosaConstants.CLIENTDEPENDENCYIDS, new ArrayList());
        association.put(MsosaConstants.SUPPLIERDEPENDENCYIDS, new ArrayList());
        association.put(MsosaConstants.NAMEEXPRESSION, null);
        association.put(MsosaConstants.VISIBILITY, "public");
        association.put(MsosaConstants.TEMPLATEPARAMETERID, null);
        association.put(MsosaConstants.ELEMENTIMPORTIDS, new ArrayList());
        association.put(MsosaConstants.PACKAGEIMPORTIDS, new ArrayList());
        association.put(MsosaConstants.ISLEAF, false);
        association.put(MsosaConstants.TEMPLATEBINDINGIDS, new ArrayList());
        association.put(MsosaConstants.USECASEIDS, new ArrayList());
        association.put(MsosaConstants.REPRESENTATIONID, null);
        association.put(MsosaConstants.COLLABORATIONUSEIDS, new ArrayList());
        association.put(MsosaConstants.GENERALIZATIONIDS, new ArrayList());
        association.put(MsosaConstants.POWERTYPEEXTENTIDS, new ArrayList());
        association.put(MsosaConstants.ISABSTRACT, false);
        association.put(MsosaConstants.ISFINALSPECIALIZATION, false);
        association.put(MsosaConstants.REDEFINEDCLASSIFIERIDS, new ArrayList());
        association.put(MsosaConstants.SUBSTITUTIONIDS, new ArrayList());
        association.put(MsosaConstants.ISDERIVED, false);
        association.put(MsosaConstants.NAVIGABLEOWNEDENDIDS, new ArrayList());
        return association;
    }
}
