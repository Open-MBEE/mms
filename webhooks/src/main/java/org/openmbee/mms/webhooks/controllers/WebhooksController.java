package org.openmbee.mms.webhooks.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.webhooks.json.WebhookJson;
import org.openmbee.mms.webhooks.objects.WebhookRequest;
import org.openmbee.mms.webhooks.objects.WebhookResponse;
import org.openmbee.mms.webhooks.persistence.WebhookPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/projects/{projectId}/webhooks")
@Tag(name = "Webhooks")
public class WebhooksController {

    private ProjectPersistence projectPersistence;
    private WebhookPersistence webhookPersistence;
    private ObjectMapper om;

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setWebhookPersistence(WebhookPersistence webhookPersistence) {
        this.webhookPersistence = webhookPersistence;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper om) {
        this.om = om;
    }

    @GetMapping
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_READ', true)")
    public WebhookResponse getAllWebhooks(@PathVariable String projectId) {

        WebhookResponse response = new WebhookResponse();
        List<WebhookJson> webhooks = webhookPersistence.findAllByProjectId(projectId);
        response.getWebhooks().addAll(webhooks);
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_CREATE_WEBHOOKS', false)")
    public WebhookResponse createOrUpdateWebhooks(@PathVariable String projectId, @RequestBody WebhookRequest webhooksPost) {

        if (webhooksPost.getWebhooks().isEmpty()) {
            throw new BadRequestException(new WebhookResponse().addMessage("No web hooks provided"));
        }

        WebhookResponse response = new WebhookResponse();
        Optional<ProjectJson> project = projectPersistence.findById(projectId);
        if(project.isEmpty()) {
            throw new NotFoundException("Project not found");
        }

        for (WebhookJson json: webhooksPost.getWebhooks()) {
            Optional<WebhookJson> existing = webhookExists(json, projectId);
            if (existing.isPresent()) {
                if (!projectId.equals(existing.get().getProjectId())) {
                    throw new BadRequestException("Cannot move webhooks between projects");
                }
                json.merge(existing.get());
            }
            json.setProjectId(projectId);
            if(json.getId() == null) {
                json.setId(UUID.randomUUID().toString());
            }
            response.getWebhooks().add(webhookPersistence.save(json));
        }
        return response;
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_CREATE_WEBHOOKS', false)")
    public WebhookResponse deleteWebhooks(@PathVariable String projectId, @RequestBody WebhookRequest webhookRequest) {

        Set<String> uris = new HashSet<>();
        for (WebhookJson webhookJson : webhookRequest.getWebhooks()) {
            uris.add(webhookJson.getUrl());
        }

        WebhookResponse response = new WebhookResponse();
        List<WebhookJson> webhooks = webhookPersistence.findAllByProjectId(projectId);
        if (webhooks.isEmpty()) {
            throw new NotFoundException(response.addMessage("No web hooks found for project"));
        }
        for (WebhookJson webhook : webhooks) {
            if (uris.contains(webhook.getUrl())) {
                webhookPersistence.delete(webhook);
                response.addMessage(String.format("Web hook for project %s to %s deleted", projectId, webhook.getUrl()));
            }
        }
        return response;
    }

    public Map<String, Object> convertToMap(Object obj) {
        return om.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    private Optional<WebhookJson> webhookExists(WebhookJson json, String projectId) {
        if (json.getId() != null) {
            Optional<WebhookJson> hook = webhookPersistence.findById(json.getId());
            if(hook.isEmpty()) {
                hook = webhookPersistence.findByProjectIdAndUrl(projectId, json.getUrl());
            }
            if (hook.isPresent() && hook.get().getProjectId().equals(projectId)) {
                return hook; //ensure hook by id matches the project being requested
            }
        }
        return webhookPersistence.findByProjectIdAndUrl(projectId, json.getUrl());
    }
}
