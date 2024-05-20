package org.openmbee.mms.cameo.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.openmbee.mms.cameo.CameoConstants;
import org.openmbee.mms.cameo.CameoNodeType;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.dao.NodePersistence;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.crud.domain.JsonDomain;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.view.services.PropertyData;
import org.openmbee.mms.view.services.ViewService;
import org.springframework.stereotype.Service;

@Service("cameoViewService")
public class CameoViewService extends CameoNodeService implements ViewService {

    @Override
    public ElementsResponse getDocuments(String projectId, String refId, Map<String, String> params) {
        String commitId = params.getOrDefault(CameoConstants.COMMITID, null);
        List<ElementJson> documents = getNodePersistence().findAllByNodeType(projectId, refId,
            commitId, CameoNodeType.DOCUMENT.getValue());
        ElementsResponse res = this.getViews(projectId, refId, buildRequestFromJsons(documents), params);
        for (ElementJson e: res.getElements()) {
            Optional<ElementJson> parent = getFirstRelationshipOfType(projectId, refId, commitId, e,
                List.of(CameoNodeType.GROUP.getValue()), CameoConstants.OWNERID);
            parent.ifPresent(elementJson -> e.put(CameoConstants.SITECHARACTERIZATIONID, elementJson.getId()));

        }
        return res;
    }

    @Override
    public ElementsResponse getView(String projectId, String refId, String elementId, Map<String, String> params) {
        return this.getViews(projectId, refId, buildRequest(elementId), params);
    }

    @Override
    public ElementsResponse getViews(String projectId, String refId, ElementsRequest req, Map<String, String> params) {
        ElementsResponse res = this.read(projectId, refId, req, params);
        addChildViews(res, params);
        return res;
    }

    @Override
    public void addChildViews(ElementsResponse res, Map<String, String> params) {
        for (ElementJson element: res.getElements()) {
            if (cameoHelper.isView(element)) {
                List<String> ownedAttributeIds = (List) element.get(CameoConstants.OWNEDATTRIBUTEIDS);
                if (ownedAttributeIds == null) {
                    ownedAttributeIds = new ArrayList<>();
                }
                ElementsResponse ownedAttributes = this.read(element.getProjectId(), element.getRefId(),
                    buildRequest(ownedAttributeIds), params);
                List<ElementJson> filtered = JsonDomain.filter(ownedAttributeIds, ownedAttributes.getElements());
                List<Map> childViews = new ArrayList<>();
                for (ElementJson attr : filtered) {
                    String childId = (String) attr.get(CameoConstants.TYPEID);
                    if (CameoConstants.PROPERTY.equals(attr.getType()) && childId != null && !childId.isEmpty()) {
                        Map<String, String> child = new HashMap<>();
                        child.put(ElementJson.ID, childId);
                        child.put(CameoConstants.AGGREGATION, (String) attr.get(CameoConstants.AGGREGATION));
                        child.put(CameoConstants.PROPERTYID, attr.getId());
                        childViews.add(child);
                    }
                }
                element.put(CameoConstants.CHILDVIEWS, childViews);
            }
        }
    }

    public ElementsResponse getGroups(String projectId, String refId, Map<String, String> params) {
        String commitId = params.getOrDefault(CameoConstants.COMMITID, null);
        List<ElementJson> groups = getNodePersistence().findAllByNodeType(projectId, refId, commitId,
            CameoNodeType.GROUP.getValue());

        ElementsResponse res = new ElementsResponse().setElements(groups);
        for (ElementJson e: groups) {
            Optional<ElementJson> parent = getFirstRelationshipOfType(projectId, refId, commitId, e,
                List.of(CameoNodeType.GROUP.getValue()), CameoConstants.OWNERID);
            parent.ifPresent(elementJson -> e.put(CameoConstants.PARENTID, elementJson.getId()));
        }
        return res;
}

    @Override
    public void extraProcessPostedElement(NodeChangeInfo info, ElementJson element) {
        //handle _childViews
        List<Map<String, String>> newChildViews = (List)element.remove(CameoConstants.CHILDVIEWS);
        if (newChildViews == null) {
            super.extraProcessPostedElement(info, element);
            return;
        }
        //gather data on "old" attributes
        List<String> oldOwnedAttributeIds = (List)element.get(CameoConstants.OWNEDATTRIBUTEIDS);
        //use helper to get access to Nodes
        String projectId = info.getCommitJson().getProjectId();
        String refId = info.getCommitJson().getRefId();
        NodeGetInfo oldInfo = getNodePersistence().findAll(projectId, refId, null,
            buildRequest(oldOwnedAttributeIds).getElements());
        List<PropertyData> oldProperties = new ArrayList<>();
        Map<String, PropertyData> oldPropertiesTypeMapping = new HashMap<>(); //typeId to PropertyData
        for (String oldOwnedAttributeId: oldOwnedAttributeIds) {
            if (!oldInfo.getActiveElementMap().containsKey(oldOwnedAttributeId)) {
                continue; //property doesn't exist anymore? indicates existing model inconsistency
            }
            //TODO This probably breaks view editor. move to federated domain somehow
            //Node oldNode = oldInfo.getExistingNodeMap().get(oldOwnedAttributeId);
            ElementJson oldJson = oldInfo.getActiveElementMap().get(oldOwnedAttributeId);
            PropertyData oldData = new PropertyData();
            oldData.setPropertyJson(oldJson);
            //TODO This probably breaks view editor. move to federated domain somehow
            //oldData.setPropertyNode(oldNode);
            oldProperties.add(oldData);
            String typeId = (String)oldJson.get(CameoConstants.TYPEID);
            if (typeId == null || typeId.isEmpty()) {
                continue; //property has no type
            }
            oldPropertiesTypeMapping.put(typeId, oldData);
        }
        //include project usages when finding types
        ElementsResponse oldTypeJsons = this.read(element.getProjectId(), element.getRefId(),
            buildRequest(oldPropertiesTypeMapping.keySet()), Collections.emptyMap());
        for (ElementJson oldType: oldTypeJsons.getElements()) {
            oldPropertiesTypeMapping.get(oldType.getId()).setTypeJson(oldType);
            oldPropertiesTypeMapping.get(oldType.getId()).setView(cameoHelper.isView(oldType));
        }
        //now oldProperties is a list of existing property data in existing order (can include ones with
        //      no type or types that're not views
        //reset context since previous type finding could have looked in submodules
        ContextHolder.setContext(element.getProjectId(), element.getRefId());
        //go through requested _childView changes
        //get the first package element that's in the owner chain of parent class
        //  cameo/sysml1 requires associations to be placed in the first owning package, is this rule still valid?
        Optional<ElementJson> p = getFirstRelationshipOfType(projectId, refId, null, element,
            Arrays.asList(CameoNodeType.PACKAGE.getValue(), CameoNodeType.GROUP.getValue()), CameoConstants.OWNERID);
        String packageId = p.isPresent() ? p.get().getId() : CameoConstants.HOLDING_BIN_PREFIX + element.getProjectId();
        List<PropertyData> newProperties = new ArrayList<>();
        List<String> newAttributeIds = new ArrayList<>();
        for (Map<String, String> newChildView: newChildViews) {
            String typeId = newChildView.get(ElementJson.ID);
            if (oldPropertiesTypeMapping.containsKey(typeId)) {
                //existing property and type, reuse
                PropertyData data = oldPropertiesTypeMapping.get(typeId);
                newProperties.add(data);
                newAttributeIds.add(data.getPropertyJson().getId());
                continue;
            }
            //create new properties and association
            PropertyData newProperty = createElementsForView(info, newChildView.get(CameoConstants.AGGREGATION),
                typeId, element.getId(), packageId);
            newProperties.add(newProperty);
            newAttributeIds.add(newProperty.getPropertyJson().getId());
        }
        //go through old attributes and add back any that wasn't to a view and delete ones that's to a view but not in newProperties
        List<PropertyData> toDelete = new ArrayList<>();
        for (PropertyData oldProperty: oldProperties) {
            if (!oldProperty.isView()) {
                newProperties.add(oldProperty);
                newAttributeIds.add(oldProperty.getPropertyJson().getId());
                continue;
            }
            if (newProperties.contains(oldProperty)) {
                continue; //already added
            }
            toDelete.add(oldProperty);
        }
        deletePropertyElements(projectId, refId, toDelete, info);
        //new derived ownedAttributeIds based on changes
        element.put(CameoConstants.OWNEDATTRIBUTEIDS, newAttributeIds);
        super.extraProcessPostedElement(info, element);
    }

    private PropertyData createElementsForView(NodeChangeInfo info, String aggregation, String typeId, String parentId, String packageId) {
        //create new properties and association
        String newPropertyId = UUID.randomUUID().toString();
        String newAssocId = UUID.randomUUID().toString();
        String newAssocPropertyId = UUID.randomUUID().toString();
        ElementJson newPropertyJson = cameoHelper.createProperty(newPropertyId, "", parentId,
            aggregation, typeId, newAssocId);
        ElementJson newAssocJson = cameoHelper.createAssociation(newAssocId, packageId,
            newAssocPropertyId, newPropertyId);
        ElementJson newAssocPropertyJson = cameoHelper.createProperty(newAssocPropertyId, "",
            newAssocId, CameoConstants.NONE, parentId, newAssocId);

        NodePersistence nodeChangeDomain = getNodePersistence();
        nodeChangeDomain.prepareAddsUpdates(info, List.of(newPropertyJson, newAssocJson, newAssocPropertyJson));
        super.extraProcessPostedElement(info, newPropertyJson);
        super.extraProcessPostedElement(info, newAssocJson);
        super.extraProcessPostedElement(info, newAssocPropertyJson);
        PropertyData newProperty = new PropertyData();
        newProperty.setAssocJson(newAssocJson);
        newProperty.setPropertyJson(newPropertyJson);
        newProperty.setAssocPropertyJson(newAssocPropertyJson);
        newProperty.setView(true);
        return newProperty;
    }

    private void deletePropertyElements(String projectId, String refId, List<PropertyData> properties, NodeChangeInfo info) {
        Set<String> assocToDelete = new HashSet<>();
        for (PropertyData oldProperty: properties) {
            ElementJson oldPropertyJson = oldProperty.getPropertyJson();
            getNodePersistence().prepareDeletes(info, List.of(oldPropertyJson));
            String assocId = (String)oldPropertyJson.get(CameoConstants.ASSOCIATIONID);
            if (assocId == null) {
                continue;
            }
            assocToDelete.add(assocId);
        }
        Set<String> assocPropToDelete = new HashSet<>();
        NodeGetInfo assocInfo = getNodePersistence().findAll(projectId, refId, null, buildRequest(assocToDelete).getElements());
        for (ElementJson assocJson: assocInfo.getActiveElementMap().values()) {
            getNodePersistence().prepareDeletes(info, List.of(assocJson));
            List<String> ownedEndIds = (List<String>)assocJson.get(CameoConstants.OWNEDENDIDS);
            if (ownedEndIds == null) {
                continue;
            }
            assocPropToDelete.addAll(ownedEndIds);
        }
        NodeGetInfo assocPropInfo = getNodePersistence().findAll(projectId, refId, null, buildRequest(assocPropToDelete).getElements());
        getNodePersistence().prepareDeletes(info, assocPropInfo.getActiveElementMap().values());
    }

    //find first element of type in types following e's relkey (assuming relkey's value is an element id)
    private Optional<ElementJson> getFirstRelationshipOfType(String projectId, String refId, String commitId,
            ElementJson e, List<Integer> types, String relkey) {
        //only for latest graph
        String nextId = (String)e.get(relkey);
        if (nextId == null || nextId.isEmpty()) {
            return Optional.empty();
        }

        NodeGetInfo getInfo = nodePersistence.findById(projectId, refId, commitId, nextId);
        Optional<ElementJson> next = Optional.of(getInfo.getActiveElementMap().get(nextId));

        while (next.isPresent()) {
            if (types.contains(cameoHelper.getNodeType(next.get()).getValue())) {
                return next;
            }
            nextId = (String)next.get().get(relkey);
            // If there isn't a next or if the nextId would be the project root (would return empty element)
            if (nextId == null || nextId.isEmpty() || next.get().getId().endsWith(CameoConstants.PROJECT_MODEL_SUFFIX)) {
                return Optional.empty();
            }
            getInfo = nodePersistence.findById(projectId, refId, commitId, nextId);
            next = Optional.of(getInfo.getActiveElementMap().get(nextId));
        }
        return Optional.empty();
    }
}
