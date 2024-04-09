package org.openmbee.mms.twc.services;

import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.CommitsResponse;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.crud.services.DefaultNodeService;
import org.openmbee.mms.json.CommitJson;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.twc.constants.TwcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("twcRevisionMmsCommitMapService")
public class TwcRevisionMmsCommitMapService extends DefaultNodeService implements NodeService {
    private BranchPersistence branchPersistence;

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    /**
     * Method to update the twc revision id with commit id
     * @param projectId
     * @param commitId
     * @param revisionId
     * @return
     */
    public CommitsResponse updateTwcRevisionID(String projectId, String commitId, String revisionId) {
        CommitsResponse commitsResponse = new CommitsResponse();
        if (revisionId == null || revisionId.isEmpty()) {
            return commitsResponse.addMessage("Revision id can not be empty");
        }
        Optional<CommitJson> commitJsonDetails = commitPersistence.findById(projectId, commitId);
        try {
            if (commitJsonDetails.isPresent()) {
                CommitJson commitObj = commitJsonDetails.get();
                commitObj.put(TwcConstants.TWCREVISIONID, revisionId);
                commitsResponse.getCommits().add(commitPersistence.update(commitObj));
            } else {
                throw new NotFoundException(commitsResponse);
            }
        } catch (Exception exception) {
            logger.error("Error occurred while associating commit ID and Twc Revision: " + exception.getMessage());
            throw new InternalErrorException(exception);
        }
        return commitsResponse;
    }

    /**
     * Method to get the twc revision list with project and branch id
     * @param projectId
     * @param refId
     * @param reverseOrder
     * @param limit
     * @return
     */
    public List<CommitJson> getTwcRevisionList(String projectId, String refId, Boolean reverseOrder, Integer limit) {
        List<CommitJson> commits = new ArrayList<>();
        Optional<RefJson> ref = branchPersistence.findById(projectId, refId);
        if (!ref.isPresent()) {
            throw new NotFoundException("Branch not found");
        }
        try {
            List<CommitJson> commitJsonList = commitPersistence.findByProjectAndRefAndTimestampAndLimit(projectId, refId, null, 0);
            if (null != commitJsonList && commitJsonList.size() > 0) {
                commitJsonList.stream().forEach(commitJsonData -> {
                    if (commitJsonData.containsKey(TwcConstants.TWCREVISIONID)) {
                        commits.add(commitJsonData);
                    }
                });
            }
            commits.sort(new CommitsComparator(reverseOrder));
            if(limit != null && limit > 0) {
                return commits.subList(0, Math.min(limit, commits.size()));
            }
            return commits;
        } catch (Exception exception) {
            logger.error("Error occurred while getting Twc Revision: " + exception.getMessage());
            throw new InternalErrorException(exception);
        }
    }

    private class CommitsComparator implements Comparator<CommitJson> {

        private boolean ascending = true;

        public CommitsComparator(Boolean reverseOrder) {
            if(reverseOrder != null && reverseOrder) {
                ascending = false;
            }
        }

        @Override
        public int compare(CommitJson o, CommitJson t1) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(Formats.DATE_FORMAT);
                Date d1 = dateFormat.parse((String) o.get(CommitJson.CREATED));
                Date d2 = dateFormat.parse((String) t1.get(CommitJson.CREATED));
                return ascending ? d1.compareTo(d2) : d2.compareTo(d1);
            } catch (ParseException e) {
                logger.error("Error parsing commit dates: " + e.getMessage());
                throw new InternalErrorException("Invalid commit created dates.");
            }
        }
    }
}
