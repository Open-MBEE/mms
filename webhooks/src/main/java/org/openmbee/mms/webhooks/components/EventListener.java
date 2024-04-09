package org.openmbee.mms.webhooks.components;

import org.openmbee.mms.core.objects.EventObject;
import org.openmbee.mms.webhooks.json.WebhookJson;
import org.openmbee.mms.webhooks.persistence.WebhookPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@Component
public class EventListener implements ApplicationListener<EventObject> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${webhook.default.all:#{null}}")
    private Optional<String> allEvents;

    private WebhookPersistence eventRepository;

    @Autowired
    public void setEventRepository(WebhookPersistence eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void onApplicationEvent(EventObject eventObject) {
        List<WebhookJson> webhooks = eventRepository.findAllByProjectId(eventObject.getProjectId());

        for (WebhookJson webhook : webhooks) {
            sendWebhook(webhook.getUrl(), eventObject.getSource());
        }

        allEvents.ifPresent(event -> sendWebhook(event, eventObject.getSource()));
    }

    private void sendWebhook(String webhook, Object payload) {
        try {
            URI uri = new URI(webhook);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> result = restTemplate.postForEntity(uri, request, String.class);
            if (result.getStatusCodeValue() == 200) {
                logger.info("Sent event to " + webhook + " with payload: " + payload);
            }
        } catch (URISyntaxException se) {
            // Do nothing; Nowhere to post;
            logger.error("Error in web hook: " + webhook, se);
            return;
        } catch (Exception e) {
            logger.error("Some webhook error happened for " + webhook, e);
            return;
        }
    }
}
