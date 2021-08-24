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
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.crud.services.NodeGetHelper;
import org.openmbee.mms.crud.services.NodePostHelper;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.json.ElementJson;
import org.springframework.stereotype.Service;

@Service("cameoViewService")
public class CameoViewService extends CameoNodeService {

    public ElementsResponse getDocuments(String projectId, String refId, Map<String, String> params) {
        ContextHolder.setContext(projectId, refId);
        List<Node> documents = this.nodeRepository.findAllByNodeType(CameoNodeType.DOCUMENT.getValue());
        NodeGetHelper nodeGetHelper = getNodeGetHelper();
        ElementsResponse res = this.getViews(projectId, refId, buildRequest(nodeGetHelper.convertNodesToMap(documents).keySet()), params);
        for (ElementJson e: res.getElements()) {
            Optional<ElementJson> parent = nodeGetHelper.getFirstRelationshipOfType(e,
                Arrays.asList(CameoNodeType.GROUP.getValue()), CameoConstants.OWNERID);
            if (parent.isPresent()) {
                e.put("_groupId", parent.get().getId());
            }
        }
        return res;
    }

    public ElementsResponse getView(String projectId, String refId, String elementId, Map<String, String> params) {
        return this.getViews(projectId, refId, buildRequest(elementId), params);
    }

    public ElementsResponse getViews(String projectId, String refId, ElementsRequest req, Map<String, String> params) {
        ElementsResponse res = this.read(projectId, refId, req, params);
        addChildViews(res, params);
        return res;
    }

    public void addChildViews(ElementsResponse res, Map<String, String> params) {
        for (ElementJson element: res.getElements()) {
            if (cameoHelper.isView(element)) {
                List<String> ownedAttributeIds = (List) element.get(CameoConstants.OWNEDATTRIBUTEIDS);
                ElementsResponse ownedAttributes = this.read(element.getProjectId(), element.getRefId(),
                    buildRequest(ownedAttributeIds), params);
                NodeGetHelper nodeGetHelper = getNodeGetHelper();
                List<ElementJson> sorted = nodeGetHelper.sort(ownedAttributeIds, ownedAttributes.getElements());
                List<Map> childViews = new ArrayList<>();
                for (ElementJson attr : sorted) {
                    String childId = (String) attr.get(CameoConstants.TYPEID);
                    if ("Property".equals(attr.getType()) && childId != null && !childId.isEmpty()) {
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
        ContextHolder.setContext(projectId, refId);
        List<Node> groups = this.nodeRepository.findAllByNodeType(CameoNodeType.GROUP.getValue());
        NodeGetHelper nodeGetHelper = getNodeGetHelper();
        ElementsResponse res = this.read(projectId, refId, buildRequest(nodeGetHelper.convertNodesToMap(groups).keySet()), params);
        for (ElementJson e: res.getElements()) {
            Optional<ElementJson> parent = nodeGetHelper.getFirstRelationshipOfType(e,
                Arrays.asList(CameoNodeType.GROUP.getValue()), CameoConstants.OWNERID);
            if (parent.isPresent()) {
                e.put("_parentId", parent.get().getId());
            }
        }
        return res;
}

    @Override
    public void extraProcessPostedElement(ElementJson element, Node node, NodeChangeInfo info) {
        //handle _childViews
        List<Map<String, String>> newChildViews = (List)element.remove(CameoConstants.CHILDVIEWS);
        if (newChildViews == null) {
            super.extraProcessPostedElement(element, node, info);
            return;
        }
        //gather data on "old" attributes
        List<String> oldOwnedAttributeIds = (List)element.get(CameoConstants.OWNEDATTRIBUTEIDS);
        //use helper to get access to Nodes
        NodeGetHelper nodeGetHelper = getNodeGetHelper();
        NodeGetInfo oldInfo = nodeGetHelper.processGetJson(buildRequest(oldOwnedAttributeIds).getElements(), null);
        List<PropertyData> oldProperties = new ArrayList<>();
        Map<String, PropertyData> oldPropertiesTypeMapping = new HashMap<>(); //typeId to PropertyData
        for (String oldOwnedAttributeId: oldOwnedAttributeIds) {
            if (!oldInfo.getActiveElementMap().containsKey(oldOwnedAttributeId)) {
                continue; //property doesn't exist anymore? indicates existing model inconsistency
            }
            Node oldNode = oldInfo.getExistingNodeMap().get(oldOwnedAttributeId);
            ElementJson oldJson = oldInfo.getActiveElementMap().get(oldOwnedAttributeId);
            PropertyData oldData = new PropertyData();
            oldData.setPropertyJson(oldJson);
            oldData.setPropertyNode(oldNode);
            oldProperties.add(oldData);
            String typeId = (String)oldJson.get(CameoConstants.TYPEID);
            if (typeId == null || typeId.isEmpty()) {
                continue; //property has no type
            }
            oldPropertiesTypeMapping.put(typeId, oldData);
        }
        //include project usages when finding types
        ElementsResponse oldTypeJsons = this.read(element.getProjectId(), element.getRefId(),
            buildRequest(oldPropertiesTypeMapping.keySet()), Collections.EMPTY_MAP);
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
        Optional<ElementJson> p = getNodePostHelper().getFirstRelationshipOfType(element,
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
                newAttributeIds.add(data.getPropertyNode().getNodeId());
                continue;
            }
            //create new properties and association
            PropertyData newProperty = createElementsForView(newChildView.get(CameoConstants.AGGREGATION),
                typeId, element.getId(), packageId, info);
            newProperties.add(newProperty);
            newAttributeIds.add(newProperty.getPropertyNode().getNodeId());
        }
        //go through old attributes and add back any that wasn't to a view and delete ones that's to a view but not in newProperties
        List<PropertyData> toDelete = new ArrayList<>();
        for (PropertyData oldProperty: oldProperties) {
            if (!oldProperty.isView()) {
                newProperties.add(oldProperty);
                newAttributeIds.add(oldProperty.getPropertyNode().getNodeId());
                continue;
            }
            if (newProperties.contains(oldProperty)) {
                continue; //already added
            }
            toDelete.add(oldProperty);
        }
        deletePropertyElements(toDelete, info);
        //new derived ownedAttributeIds based on changes
        element.put(CameoConstants.OWNEDATTRIBUTEIDS, newAttributeIds);
        super.extraProcessPostedElement(element, node, info);
    }

    private PropertyData createElementsForView(String aggregation, String typeId, String parentId, String packageId, NodeChangeInfo info) {
        //create new properties and association
        Node newPropertyNode = new Node();
        Node newAssocNode = new Node();
        Node newAssocPropertyNode = new Node();
        String newPropertyId = UUID.randomUUID().toString();
        String newAssocId = UUID.randomUUID().toString();
        String newAssocPropertyId = UUID.randomUUID().toString();
        ElementJson newPropertyJson = cameoHelper.createProperty(newPropertyId, "", parentId,
            aggregation, typeId, newAssocId);
        ElementJson newAssocJson = cameoHelper.createAssociation(newAssocId, packageId,
            newAssocPropertyId, newPropertyId);
        ElementJson newAssocPropertyJson = cameoHelper.createProperty(newAssocPropertyId, "",
            newAssocId, "none", parentId, newAssocId);
        NodePostHelper nodePostHelper = getNodePostHelper();
        nodePostHelper.processElementAdded(newPropertyJson, newPropertyNode, info);
        nodePostHelper.processElementAdded(newAssocJson, newAssocNode, info);
        nodePostHelper.processElementAdded(newAssocPropertyJson, newAssocPropertyNode, info);
        super.extraProcessPostedElement(newPropertyJson, newPropertyNode, info);
        super.extraProcessPostedElement(newAssocJson, newAssocNode, info);
        super.extraProcessPostedElement(newAssocPropertyJson, newAssocPropertyNode, info);
        PropertyData newProperty = new PropertyData();
        newProperty.setAssocJson(newAssocJson);
        newProperty.setAssocNode(newAssocNode);
        newProperty.setPropertyJson(newPropertyJson);
        newProperty.setPropertyNode(newPropertyNode);
        newProperty.setAssocPropertyNode(newAssocPropertyNode);
        newProperty.setAssocPropertyJson(newAssocPropertyJson);
        newProperty.setView(true);
        return newProperty;
    }

    private void deletePropertyElements(List<PropertyData> properties, NodeChangeInfo info) {
        Set<String> assocToDelete = new HashSet<>();
        NodePostHelper nodePostHelper = getNodePostHelper();
        for (PropertyData oldProperty: properties) {
            Node oldPropertyNode = oldProperty.getPropertyNode();
            ElementJson oldPropertyJson = oldProperty.getPropertyJson();
            nodePostHelper.processElementDeleted(oldPropertyJson, oldPropertyNode, info);
            String assocId = (String)oldPropertyJson.get(CameoConstants.ASSOCIATIONID);
            if (assocId == null) {
                continue;
            }
            assocToDelete.add(assocId);
        }
        Set<String> assocPropToDelete = new HashSet<>();
        NodeGetHelper nodeGetHelper = getNodeGetHelper();
        NodeGetInfo assocInfo = nodeGetHelper.processGetJson(buildRequest(assocToDelete).getElements(), null);
        for (ElementJson assocJson: assocInfo.getActiveElementMap().values()) {
            Node assocNode = assocInfo.getExistingNodeMap().get(assocJson.getId());
            nodePostHelper.processElementDeleted(assocJson, assocNode, info);
            List<String> ownedEndIds = (List<String>)assocJson.get(CameoConstants.OWNEDENDIDS);
            if (ownedEndIds == null) {
                continue;
            }
            assocPropToDelete.addAll(ownedEndIds);
        }
        NodeGetInfo assocPropInfo = nodeGetHelper.processGetJson(buildRequest(assocPropToDelete).getElements(), null);
        for (ElementJson assocPropJson: assocPropInfo.getActiveElementMap().values()) {
            Node assocPropNode = assocPropInfo.getExistingNodeMap().get(assocPropJson.getId());
            nodePostHelper.processElementDeleted(assocPropJson, assocPropNode, info);
        }
    }
}
