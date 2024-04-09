package org.openmbee.mms.rdb.repositories;

import org.openmbee.mms.data.dao.WebhookDAO;
import org.openmbee.mms.data.domains.global.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WebhookDAOImpl implements WebhookDAO {
    private WebhookRepository webhookRepository;

    @Autowired
    public void setWebhookRepository(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Override
    public List<Webhook> findAllByProject_ProjectId(String id) {
        return webhookRepository.findAllByProject_ProjectId(id);
    }

    @Override
    public Webhook save(Webhook webhook) {
        return webhookRepository.save(webhook);
    }

    @Override
    public void delete(Webhook org) {
        webhookRepository.delete(org);
    }

    @Override
    public Optional<Webhook> findByProject_ProjectIdAndUrl(String id, String url) {
        return webhookRepository.findByProject_ProjectIdAndUrl(id, url);
    }

    @Override
    public List<Webhook> findAll() {
        return webhookRepository.findAll();
    }

    @Override
    public Optional<Webhook> findById(Long id) {
        return webhookRepository.findById(id);
    }
}
