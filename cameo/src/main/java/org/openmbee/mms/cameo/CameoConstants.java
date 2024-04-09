package org.openmbee.mms.cameo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CameoConstants {

    public static final String AGGREGATION = "aggregation";
    public static final String APPLIEDSTEREOTYPEINSTANCEID = "appliedStereotypeInstanceId";
    public static final String ASSOCIATIONENDID = "associationEndId";
    public static final String ASSOCIATIONID = "associationId";
    public static final String CLASSIFIERIDS = "classifierIds";
    public static final String CLIENTDEPENDENCYIDS = "clientDependencyIds";
    public static final String COLLABORATIONUSEIDS = "collaborationUseIds";
    public static final String COMMITID = "commitId";
    public static final String DATATYPEID = "datatypeId";
    public static final String DEFAULTVALUE = "defaultValue";
    public static final String DEFININGFEATUREID = "definingFeatureId";
    public static final String DEPLOYMENTIDS = "deploymentIds";
    public static final String DESCRIPTION = "description";
    public static final String DOCUMENTATION = "documentation";
    public static final String ELEMENTIMPORTIDS = "elementImportIds";
    public static final String ENDAPATHIDS = "endAPathIds";
    public static final String ENDBPATHIDS = "endBPathIds";
    public static final String ENDIDS = "endIds";
    public static final String GENERALIZATIONIDS = "generalizationIds";
    public static final String INTERFACEID = "interfaceId";
    public static final String ISABSTRACT = "isAbstract";
    public static final String ISDERIVED = "isDerived";
    public static final String ISDERIVEDUNION = "isDerivedUnion";
    public static final String ISFINALSPECIALIZATION = "isFinalSpecialization";
    public static final String ISID = "isID";
    public static final String ISLEAF = "isLeaf";
    public static final String ISORDERED = "isOrdered";
    public static final String ISREADONLY = "isReadOnly";
    public static final String ISSTATIC = "isStatic";
    public static final String ISUNIQUE = "isUnique";
    public static final String LOWERVALUE = "lowerValue";
    public static final String KEYWORDS = "keywords";
    public static final String MDEXTENSIONSIDS = "mdExtensionsIds";
    public static final String MEMBERENDIDS = "memberEndIds";
    public static final String METADATA = "metadata";
    public static final String MOUNTEDELEMENTPROJECTID = "mountedElementProjectId";
    public static final String MOUNTEDREFID = "mountedRefId";
    public static final String NAME = "name";
    public static final String NAMEEXPRESSION = "nameExpression";
    public static final String NAVIGABLEOWNEDENDIDS = "navigableOwnedEndIds";
    public static final String NONE = "none";
    public static final String OWNEDATTRIBUTEIDS = "ownedAttributeIds";
    public static final String OWNEDENDIDS = "ownedEndIds";
    public static final String OWNERID = "ownerId";
    public static final String PACKAGEIMPORTIDS = "packageImportIds";
    public static final String PACKAGEMERGEIDS = "packageMergeIds";
    public static final String PARENTID = "_parentId";
    public static final String POWERTYPEEXTENTIDS = "powertypeExtentIds";
    public static final String PROPERTY = "Property";
    public static final String PROPERTYID = "propertyId";
    public static final String PROPERTYTYPE = "propertyType";
    public static final String PROFILEAPPLICATIONIDS = "profileApplicationIds";
    public static final String QUALIFIERIDS = "qualifierIds";
    public static final String REDEFINEDCLASSIFIERIDS = "redefinedClassifierIds";
    public static final String REDEFINEDPROPERTYIDS = "redefinedPropertyIds";
    public static final String REPRESENTATIONID = "representationId";
    public static final String SLOTIDS = "slotIds";
    public static final String SPECIFICATION = "specification";
    public static final String STEREOTYPEDELEMENTID = "stereotypedElementId";
    public static final String SUBSETTEDPROPERTYIDS = "subsettedPropertyIds";
    public static final String SUBSTITUTIONIDS = "substitutionIds";
    public static final String SUPPLIERDEPENDENCYIDS = "supplierDependencyIds";
    public static final String SYNCELEMENTID = "syncElementId";
    public static final String TARGET = "targetId";
    public static final String TEMPLATEBINDINGIDS = "templateBindingIds";
    public static final String TEMPLATEPARAMETERID = "templateParameterId";
    public static final String TYPE = "type";
    public static final String TYPEID = "typeId";
    public static final String UPPERVALUE = "upperValue";
    public static final String URI = "URI";
    public static final String USECASEIDS = "useCaseIds";
    public static final String VALUEID = "valueId";
    public static final String VISIBILITY = "visibility";
    public static final String APPLIEDSTEREOTYPEIDS = "appliedStereotypeIds";
    public static final String OLDAPPLIEDSTEREOTYPEIDS = "_appliedStereotypeIds";
    public static final String CHILDVIEWS = "_childViews";
    public static final String CONTENTS = "_contents";
    public static final String ISGROUP = "_isGroup";
    public static final String MOUNTS = "_mounts";
    public static final String PARENTVIEWS = "_parentViews";
    public static final String PROPERTIES = "_properties";
    public static final String QUALIFIEDID = "_qualifiedId";
    public static final String QUALIFIEDNAME = "_qualifiedName";
    public static final String RELATEDDOCUMENTS = "_relatedDocuments";
    public static final String SITECHARACTERIZATIONID = "_groupId";
    public static final String SITES = "_sites";
    public static final String SLOTS = "_slots";
    public static final String HOLDING_BIN_PREFIX = "holding_bin_";
    public static final String VIEW_INSTANCES_BIN_PREFIX = "view_instances_bin_";
    public static final String PACKAGE_TYPE = "Package";
    public static final String PUBLIC_VISIBILITY = "public";

    public static final Map<String, String> STEREOTYPEIDS;
    static {
        STEREOTYPEIDS = new HashMap<>();
        STEREOTYPEIDS.put("_17_0_2_3_87b0275_1371477871400_792964_43374", "document");
        STEREOTYPEIDS.put("_17_0_1_232f03dc_1325612611695_581988_21583", "view");
        STEREOTYPEIDS.put("_11_5EAPbeta_be00301_1147420760998_43940_227", "view");
        STEREOTYPEIDS.put("_18_0beta_9150291_1392290067481_33752_4359", "view");
        STEREOTYPEIDS.put("_17_0_1_407019f_1332453225141_893756_11936", "view");
        STEREOTYPEIDS.put("_17_0_2_3_407019f_1389807639137_860750_29082", "conforms");
        STEREOTYPEIDS.put("_16_5_4_409a058d_1259862803278_226185_1083", "exposes");
        STEREOTYPEIDS.put("_17_0_5_1_8660276_1407362513794_939259_26181", "characterizes");
    }

    public static final Map<String, String> PROPERTYSIDS;
    static {
        PROPERTYSIDS = new HashMap<>();
        PROPERTYSIDS.put("composite", "_15_0_be00301_1199377756297_348405_2678");
        PROPERTYSIDS.put("none", "_15_0_be00301_1199378032543_992832_3096");
        PROPERTYSIDS.put("shared", "_15_0_be00301_1199378020836_340320_3071");
    }

    public static final String VALUEPROPERTY = "_12_0_be00301_1164123483951_695645_2041";
    public static final String CONSTRAINTPROPERTY = "_11_5EAPbeta_be00301_1147767840464_372327_467";

    public static final Set<String> VIEWSIDS;
    static {
        VIEWSIDS = new HashSet<>();
        VIEWSIDS.add("_17_0_2_3_87b0275_1371477871400_792964_43374");
        VIEWSIDS.add("_17_0_1_232f03dc_1325612611695_581988_21583");
        VIEWSIDS.add("_11_5EAPbeta_be00301_1147420760998_43940_227");
        VIEWSIDS.add("_18_0beta_9150291_1392290067481_33752_4359");
        VIEWSIDS.add("_17_0_1_407019f_1332453225141_893756_11936");
    }
    public static final String DOCUMENTSID = "_17_0_2_3_87b0275_1371477871400_792964_43374";
}
