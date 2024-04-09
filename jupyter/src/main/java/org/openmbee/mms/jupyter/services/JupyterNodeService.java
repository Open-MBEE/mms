package org.openmbee.mms.jupyter.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.crud.domain.JsonDomain;
import org.openmbee.mms.json.ElementJson;
import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.jupyter.JupyterConstants;
import org.openmbee.mms.jupyter.JupyterNodeType;
import org.openmbee.mms.jupyter.controllers.NotebooksRequest;
import org.openmbee.mms.jupyter.controllers.NotebooksResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("jupyterNodeService")
public class JupyterNodeService extends DefaultNodeService implements NodeService {

    private JupyterHelper jupyterHelper;

    @Autowired
    public void setJupyterHelper(JupyterHelper jupyterHelper) {
        this.jupyterHelper = jupyterHelper;
    }

    public ElementsResponse readNotebooks(String projectId, String refId, String elementId, Map<String, String> params) {
        ElementsRequest req = new ElementsRequest();
        List<ElementJson> reqs = new ArrayList<>();
        if (elementId == null || elementId.isEmpty()) {
           reqs.addAll(getNodePersistence().findAllByNodeType(projectId, refId, null,JupyterNodeType.NOTEBOOK.getValue()));
        } else {
            reqs.add((new ElementJson()).setId(elementId));
        }
        req.setElements(reqs);
        return readNotebooks(projectId, refId, req, params);
    }

    public ElementsResponse readNotebooks(String projectId, String refId, ElementsRequest req,
            Map<String, String> params) {
        ElementsResponse res = this.read(projectId, refId, req, params);
        List<Rejection> rejected = new ArrayList<>(res.getRejected());
        List<ElementJson> nonNotebooks = new ArrayList<>();
        for (ElementJson e: res.getElements()) {
            List<ElementJson> req2s = new ArrayList<>();
            if (!e.containsKey(JupyterConstants.CELLS) || e.get(JupyterConstants.CELLS) == null) {
                rejected.add(new Rejection(e, 400, "not a notebook"));
                nonNotebooks.add(e);
                continue;
            }
            for (String cellId: (List<String>)e.get(JupyterConstants.CELLS)) { //stored notebooks have cells as list of ids, use cellIds?
                req2s.add((new ElementJson()).setId(cellId));
            }
            ElementsRequest req2 = new ElementsRequest();
            req2.setElements(req2s);
            ElementsResponse cells = this.read(projectId, refId, req2, params);
            Map<String, ElementJson> cellmap = JsonDomain.convertJsonToMap(cells.getElements());
            e.put(JupyterConstants.CELLS, order((List<String>)e.get(JupyterConstants.CELLS), cellmap));
        }
        res.getElements().removeAll(nonNotebooks);
        res.setRejected(rejected);
        return res;
    }

    public NotebooksResponse createOrUpdateNotebooks(String projectId, String refId, NotebooksRequest req,
            Map<String, String> params, String user) {
        List<ElementJson> postReqs = new ArrayList<>();
        List<ElementJson> resReqs = new ArrayList<>();
        for (ElementJson notebook: req.getNotebooks()) {
            List<String> cells = new ArrayList<>(); //to replace cells with list of cell ids
            for (Map<String, Object> cell: (List<Map<String, Object>>)notebook.get(JupyterConstants.CELLS)) {
                if (!cell.containsKey("id")) { //check metadata?
                    cell.put("id", UUID.randomUUID().toString());
                }
                cells.add((String)cell.get("id"));
                ElementJson postCell = new ElementJson();
                postCell.putAll(cell);
                postReqs.add(postCell);
            }
            if (!notebook.containsKey("id")) {
                notebook.setId(UUID.randomUUID().toString());
            }
            resReqs.add((new ElementJson()).setId(notebook.getId()));
            ElementJson postNotebook = new ElementJson();
            postNotebook.putAll(notebook);
            postNotebook.put(JupyterConstants.CELLS, cells); //either replace cells or remove cells and use cellIds
            postReqs.add(postNotebook);
        }
        ElementsRequest postReq = new ElementsRequest();
        postReq.setElements(postReqs);
        this.createOrUpdate(projectId, refId, postReq, params, user);

        ElementsRequest resReq = new ElementsRequest();
        resReq.setElements(resReqs);
        ElementsResponse res = this.readNotebooks(projectId, refId, resReq, params);
        NotebooksResponse r = new NotebooksResponse();
        r.setNotebooks(res.getElements());
        return r;
    }

    protected List<ElementJson> order(List<String> ids, Map<String, ElementJson> map) {
        List<ElementJson> res = new ArrayList<>();
        for (String id: ids) {
            res.add(map.get(id));
        }
        return res;
    }
}
