package org.openmbee.mms.elastic;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.openmbee.mms.data.dao.CommitIndexDAO;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.elastic.utils.Index;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.CommitJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommitElasticDAOImpl extends BaseElasticDAOImpl<CommitJson> implements CommitIndexDAO {

    @Value("${elasticsearch.limit.commit:10000}")
    int commitLimit;

    protected CommitJson newInstance() {
        return new CommitJson();
    }

    public void indexAll(Collection<? extends BaseJson> jsons) {
        this.indexAll(getIndex(), jsons);
    }

    public void index(BaseJson json) {
        index((CommitJson) json);
    }

    public void index(CommitJson json) {
        int commitCount = getCommitSize(json);
        if (commitCount > commitLimit) {
            List<Map<String, Object>> allActions = new ArrayList<>();
            allActions.addAll(json.getAdded().stream().peek(toAdd -> toAdd.put("action", "added")).collect(Collectors.toList()));
            allActions.addAll(json.getUpdated().stream().peek(toUpdate -> toUpdate.put("action", "updated")).collect(Collectors.toList()));
            allActions.addAll(json.getDeleted().stream().peek(toDelete -> toDelete.put("action", "deleted")).collect(Collectors.toList()));

            while (!allActions.isEmpty()) {
                CommitJson currentCommitCopy = CommitJson.copy(new CommitJson(), json);
                currentCommitCopy.setAdded(new ArrayList<>());
                currentCommitCopy.setUpdated(new ArrayList<>());
                currentCommitCopy.setDeleted(new ArrayList<>());
                currentCommitCopy.setDocId(UUID.randomUUID().toString());
                do {
                    Map<String, Object> action = allActions.remove(0);
                    String compare = action.getOrDefault("action", "none").toString();
                    action.remove("action");
                    switch (compare) {
                        case "added":
                            currentCommitCopy.getAdded().add(action);
                            break;
                        case "updated":
                            currentCommitCopy.getUpdated().add(action);
                            break;
                        case "deleted":
                            currentCommitCopy.getDeleted().add(action);
                            break;
                    }
                } while(getCommitSize(currentCommitCopy) < commitLimit && !allActions.isEmpty());
                this.index(getIndex(), currentCommitCopy);
            }

        } else {
            this.index(getIndex(), json);
        }
    }

    public Optional<CommitJson> findById(String commitId) {
        return getFullCommit(commitId);
    }

    public List<CommitJson> findAllById(Set<String> commitIds) {
        return getFullCommits(commitIds);
    }

    public void deleteById(String commitId) {
        List<CommitJson> docs = getDocs(commitId);
        docs.forEach(commit -> {
           this.deleteById(getIndex(), commit.getDocId());
        });
    }

    public void deleteAll(Collection<? extends BaseJson> jsons) {
        this.deleteAll(getIndex(), jsons);
    }

    public boolean existsById(String commitId) {
        List<CommitJson> docs = getDocs(commitId);
        return !docs.isEmpty();
    }

    /**
     * Gets the JSON document of a bool : should commit query result printed as Json looks like: {
     * "bool":{"should":[{"term":{"added.id":id}}, {"term":{"updated.id":id}},
     * {"term":{"deleted.id":id}}], "filter": {"terms": {"id": [commitIds]}}} }
     *
     * @param id the nodeId to add to the term search
     * @param commitIds relevant commit ids
     * @return QueryBuilder q
     */
    private QueryBuilder getCommitHistoryQuery(String id, Set<String> commitIds) {
        QueryBuilder addedQuery = QueryBuilders.termQuery("added.id", id);
        QueryBuilder updatedQuery = QueryBuilders.termQuery("updated.id", id);
        QueryBuilder deletedQuery = QueryBuilders.termQuery("deleted.id", id);
        QueryBuilder query = QueryBuilders.boolQuery()
            .should(addedQuery)
            .should(updatedQuery)
            .should(deletedQuery)
            .filter(QueryBuilders.termsQuery(CommitJson.ID, commitIds))
            .minimumShouldMatch(1);
        return query;
    }

    @Override
    public List<CommitJson> elementDeletedHistory(String id, Collection<String> commitIds) {
        QueryBuilder query = QueryBuilders.boolQuery()
            .filter(QueryBuilders.termQuery("deleted.id", id))
            .filter(QueryBuilders.termsQuery(CommitJson.ID, commitIds));
        try {
            List<CommitJson> commits = new ArrayList<>();
            SearchHits hits = getCommitResults(query);
            if (hits.getTotalHits().value == 0) {
                return new ArrayList<>();
            }
            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();// gets "_source"
                CommitJson ob = newInstance();
                ob.putAll(source);
                ob.remove(CommitJson.ADDED);
                ob.remove(CommitJson.UPDATED);
                ob.remove(CommitJson.DELETED);
                commits.add(ob);
            }
            return commits;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new InternalErrorException(e);
        }
    }

    /**
     * Returns the commit history of a element
     * <p> Returns a list of commit metadata for the specified id
     *
     * <p>
     *
     * @param nodeId sysmlId
     * @param commitIds list of commitIds for the relevant branch
     * @return JSONArray array or empty json array
     */
    @Override
    public List<CommitJson> elementHistory(String nodeId, Set<String> commitIds) {
        try {
            List<CommitJson> commits = new ArrayList<>();
            QueryBuilder query = getCommitHistoryQuery(nodeId, commitIds);
            SearchHits hits = getCommitResults(query);
            if (hits.getTotalHits().value == 0) {
                return new ArrayList<>();
            }
            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();// gets "_source"
                CommitJson ob = newInstance();
                ob.putAll(source);
                ob.remove(CommitJson.ADDED);
                ob.remove(CommitJson.UPDATED);
                ob.remove(CommitJson.DELETED);
                commits.add(ob);
            }
            return commits;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new InternalErrorException(e);
        }
    }

    @Override
    protected String getIndex() {
        return Index.COMMIT.get();
    }

    @Override
    public CommitJson update(CommitJson commitJson) {
        return this.update(getIndex(), commitJson);
    }

    private List<CommitJson> getDocs(String commitId) {
        try {
            QueryBuilder commitQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(CommitJson.ID, commitId));
            SearchHits hits = getCommitResults(commitQuery);
            List<CommitJson> rawCommits = new ArrayList<>();
            if (hits.getTotalHits().value > 0) {
                for (SearchHit hit : hits.getHits()) {
                    CommitJson ob = new CommitJson();
                    ob.putAll(hit.getSourceAsMap());
                    rawCommits.add(ob); // gets "_source"
                }
            }
            if (rawCommits.isEmpty()) {
                //try getting directly using id
                Optional<CommitJson> c = super.findById(this.getIndex(), commitId);
                c.ifPresent(commit -> rawCommits.add(commit));
            }
            return rawCommits;
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new InternalErrorException(ioe);
        }
    }

    private List<CommitJson> getFullCommits(Collection<String> commitIds) {
        return commitIds.stream().map(this::getFullCommit).filter(Optional::isPresent)
            .map(Optional::get).collect(Collectors.toList());
    }

    private Optional<CommitJson> getFullCommit(String commitId) {
        List<CommitJson> commits = getDocs(commitId);
        if (commits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mungCommits(commits));
    }

    private SearchHits getCommitResults(QueryBuilder query) throws IOException {
        SearchRequest searchRequest = new SearchRequest(getIndex());
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(query);
        sourceBuilder.size(this.resultLimit); // TODO handle paging requests
        sourceBuilder.sort(new FieldSortBuilder(CommitJson.CREATED).order(SortOrder.DESC));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, REQUEST_OPTIONS);
        return searchResponse.getHits();
    }

    private static CommitJson mungCommits(List<CommitJson> commits) {
        return commits.stream().reduce(new CommitJson(), CommitJson::copy);
    }

    private static int getCommitSize(CommitJson commitJson) {
        int commitCount = 0;
        if (commitJson.getAdded() != null && !commitJson.getAdded().isEmpty()) {
            commitCount = commitCount + commitJson.getAdded().size();
        }
        if (commitJson.getUpdated() != null && !commitJson.getUpdated().isEmpty()) {
            commitCount = commitCount + commitJson.getUpdated().size();
        }
        if (commitJson.getDeleted() != null && !commitJson.getDeleted().isEmpty()) {
            commitCount = commitCount + commitJson.getDeleted().size();
        }
        return commitCount;
    }
}
