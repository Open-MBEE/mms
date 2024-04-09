package org.openmbee.mms.webhooks.persistence;

import org.openmbee.mms.webhooks.json.WebhookJson;

import java.util.List;
import java.util.Optional;

public interface WebhookPersistence {

    WebhookJson save(WebhookJson webhook);

    Optional<WebhookJson> findById(String id);

    List<WebhookJson> findAllByProjectId(String projectId);

    Optional<WebhookJson> findByProjectIdAndUrl(String projectId, String url);

    void delete(WebhookJson webhook);
}
