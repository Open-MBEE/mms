package org.openmbee.mms.elp.services;

import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.crud.services.NodeDeleteHelper;
import org.openmbee.mms.crud.services.NodeGetHelper;
import org.openmbee.mms.crud.services.NodePostHelper;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("elpNodeService")
public class ElpNodeService extends DefaultNodeService {

    @Override
    @Autowired
    @Qualifier("elpNodeGetHelper")
    public void setNodeGetHelperFactory(ObjectFactory<NodeGetHelper> nodeGetHelperFactory) {
        super.setNodeGetHelperFactory(nodeGetHelperFactory);
    }

    @Override
    @Autowired
    @Qualifier("elpNodePostHelper")
    public void setNodePostHelperFactory(ObjectFactory<NodePostHelper> nodePostHelperFactory) {
        super.setNodePostHelperFactory(nodePostHelperFactory);
    }

    @Override
    @Autowired
    @Qualifier("elpNodeDeleteHelper")
    public void setNodeDeleteHelperFactory(ObjectFactory<NodeDeleteHelper> nodeDeleteHelperFactory) {
        super.setNodeDeleteHelperFactory(nodeDeleteHelperFactory);
    }
}
