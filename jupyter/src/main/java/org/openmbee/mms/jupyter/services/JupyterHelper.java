package org.openmbee.mms.jupyter.services;

import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.jupyter.JupyterConstants;
import org.openmbee.mms.jupyter.JupyterNodeType;
import org.springframework.stereotype.Component;
import org.openmbee.mms.core.utils.ElementUtils;

@Component
public class JupyterHelper implements ElementUtils{

    @Override
    public JupyterNodeType getNodeType(ElementJson e) {
        if (e.containsKey(JupyterConstants.CELLTYPE))
            return JupyterNodeType.CELL;
        return JupyterNodeType.NOTEBOOK;
    }

}
