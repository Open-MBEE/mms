package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.data.dao.ProjectDAO;
import org.openmbee.mms.data.dao.WebhookDAO;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.data.domains.global.Webhook;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.webhooks.json.WebhookJson;
import org.openmbee.mms.webhooks.persistence.WebhookPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FederatedWebhookPersistence implements WebhookPersistence {

    private static Logger logger = LoggerFactory.getLogger(FederatedWebhookPersistence.class);

    private WebhookDAO webhookDao;
    private ProjectDAO projectDao;
    private FederatedJsonUtils jsonUtils;


    @Autowired
    public void setWebhookDao(WebhookDAO webhookDao) {
        this.webhookDao = webhookDao;
    }

    @Autowired
    public void setProjectDao(ProjectDAO projectDao) {
        this.projectDao = projectDao;
    }

    @Autowired
    public void setJsonUtils(FederatedJsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    @Override
    public WebhookJson save(WebhookJson webhookJson) {
        if(webhookJson.getProjectId() == null || webhookJson.getProjectId().isEmpty()) {
            logger.error("Webhook cannot be saved, missing project id");
            throw new InternalErrorException("Missing project id");
        }
        Optional<Project> project = projectDao.findByProjectId(webhookJson.getProjectId());
        if(project.isEmpty()) {
            throw new InternalErrorException("Cannot find project");
        }

        Optional<Webhook> existing = getWebhookById(webhookJson.getId());
        if(existing.isEmpty()) {
            existing = webhookDao.findByProject_ProjectIdAndUrl(webhookJson.getProjectId(), webhookJson.getUrl());
            if(existing.isPresent()) {
                //This webhook already exists, so just return what's there.
                return existing.map(this::getWebhookJson).orElseThrow(() ->
                        new InternalErrorException("Could not generated webhook json from webhook"));
            }
        }

        Webhook webhook;
        if (existing.isEmpty()) {
            //Webhook doesn't exist, create it
            webhook = new Webhook();
            webhook.setProject(project.get());
        } else {
            //Webhook is there, but needs to have url updated
            webhook = existing.get();
        }
        webhook.setUrl(webhookJson.getUrl());
        Webhook saved = webhookDao.save(webhook);
        webhookJson.setId(String.valueOf(saved.getId()));
        return webhookJson;
    }


    @Override
    public Optional<WebhookJson> findById(String id) {
        if(id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return getWebhookById(id).map(this::getWebhookJson);
    }

    @Override
    public List<WebhookJson> findAllByProjectId(String projectId) {
        List<Webhook> webhooks = webhookDao.findAllByProject_ProjectId(projectId);

        return webhooks.stream().map(webhook -> {
                WebhookJson webhookJson = getWebhookJson(webhook);
                webhookJson.setProjectId(projectId);
                return webhookJson;
            }).collect(Collectors.toList());
    }


    @Override
    public Optional<WebhookJson> findByProjectIdAndUrl(String projectId, String url) {
        return webhookDao.findByProject_ProjectIdAndUrl(projectId, url).map(this::getWebhookJson);
    }

    @Override
    public void delete(WebhookJson webhookJson) {
        Optional<Webhook> webhook = getWebhookById(webhookJson.getId());
        webhook.ifPresent(w -> webhookDao.delete(w));
    }

    protected Optional<Webhook> getWebhookById(String id) {
        try {
            return webhookDao.findById(Long.parseLong(id));
        } catch(NumberFormatException ex) {
            logger.error("Invalid webhook id format: {}", id);
            return Optional.empty();
        }
    }

    protected WebhookJson getWebhookJson(Webhook webhook) {
        WebhookJson webhookJson = new WebhookJson();
        webhookJson.merge(jsonUtils.convertToMap(webhook));
        webhookJson.setId(webhook.getId().toString());
        return webhookJson;
    }

}
