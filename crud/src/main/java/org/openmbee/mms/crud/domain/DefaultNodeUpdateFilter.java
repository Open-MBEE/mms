package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.json.BaseJson;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class DefaultNodeUpdateFilter implements NodeUpdateFilter {
    private static final Logger logger = LoggerFactory.getLogger(DefaultNodeUpdateFilter.class);

    @Override
    public boolean filterUpdate(NodeChangeInfo info, ElementJson updated, ElementJson existing) {
        if (!info.getOverwrite()) {
            if (Constants.TRUE.equals(existing.getIsDeleted()) || isUpdated(updated, existing, info)) {
                return diffUpdateJson(updated, existing, info);
            } else {
                return false;
            }
        } else {
            updated.setCreator(existing.getCreator());
            updated.setCreated(existing.getCreated());
        }
        return true;
    }

    protected boolean isUpdated(BaseJson<?> element, Map<String, Object> existing, NodeChangeInfo info) {

        if (element.isPartialOf(existing)) {
            info.addRejection(element.getId(), new Rejection(element, 304, "Is Equivalent"));
            return false;
        }
        return true;
    }

    protected boolean diffUpdateJson(BaseJson<?> element, Map<String, Object> existing, NodeChangeInfo info) {

        String jsonModified = element.getModified();
        Object existingModified = existing.get(BaseJson.MODIFIED);
        if (jsonModified != null && !jsonModified.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(Formats.DATE_FORMAT);
                Date jsonModDate = dateFormat.parse(jsonModified);
                Date existingModDate = dateFormat.parse(existingModified.toString());
                if (jsonModDate.before(existingModDate)) {
                    info.addRejection(element.getId(), new Rejection(element, 409, "Conflict Detected"));
                    return false;
                }
            } catch (ParseException e) {
                logger.info("date parse exception: {} {}", jsonModified, existingModified);
            }
        }
        element.merge(existing);
        element.remove(ElementJson.IS_DELETED);
        return true;
    }

}
