package org.openmbee.mms.msosa;

import java.util.HashMap;
import java.util.Map;

public enum MsosaNodeType {
    ELEMENT(1),
    SITE(2),
    PROJECT(3),
    DOCUMENT(4),
    COMMENT(5),
    CONSTRAINT(6),
    INSTANCESPECIFICATION(7),
    OPERATION(8),
    PACKAGE(9),
    PROPERTY(10),
    PARAMETER(11),
    VIEW(12),
    VIEWPOINT(13),
    GROUP(14),
    HOLDINGBIN(15),
    PROJECTUSAGE(16);

    private static Map<Integer, MsosaNodeType> map = new HashMap<>();

    static {
        for (MsosaNodeType n : MsosaNodeType.values()) {
            map.put(n.value, n);
        }
    }

    private int value;

    MsosaNodeType(int value) {
        this.value = value;
    }

    public static MsosaNodeType valueOf(int n) {
        return map.get(n);
    }

    public int getValue() {
        return value;
    }
}


