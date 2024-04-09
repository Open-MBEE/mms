package org.openmbee.mms.elastic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.openmbee.mms.data.dao.BranchIndexDAO;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.RefJson;
import org.springframework.stereotype.Component;

@Component
public class BranchElasticDAOImpl extends BaseElasticDAOImpl<RefJson> implements BranchIndexDAO {

    protected RefJson newInstance() {
        return new RefJson();
    }

    @Override
    public void indexAll(Collection<? extends BaseJson> jsons) {
        this.indexAll(getIndex(), jsons);
    }

    @Override
    public void index(BaseJson json) {
        this.index(getIndex(), json);
    }

    public Optional<RefJson> findById(String docId) {
        return this.findById(getIndex(), docId);
    }

    public List<RefJson> findAllById(Set<String> docIds) {
        return this.findAllById(getIndex(), docIds);
    }

    public void deleteById(String docId) {
        this.deleteById(getIndex(), docId);
    }

    public void deleteAll(Collection<? extends BaseJson> jsons) {
        this.deleteAll(getIndex(), jsons);
    }

    public boolean existsById(String docId) {
        return this.existsById(getIndex(), docId);
    }

    @Override
    public RefJson update(RefJson refJson) {
        return this.update(getIndex(), refJson);
    }

    @Override
    protected String getIndex() {
        return super.getIndex() + "_metadata";
    }

    @Override
    public String createDocId(RefJson branch) {
        return UUID.randomUUID().toString();
    }
}

