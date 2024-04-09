package org.openmbee.mms.data.dao;

import org.openmbee.mms.data.domains.global.Webhook;

import java.util.List;
import java.util.Optional;

public interface WebhookDAO {

    Webhook save(Webhook webhook);

    List<Webhook> findAll();

    Optional<Webhook> findById(Long id);

    List<Webhook> findAllByProject_ProjectId(String projectId);

    Optional<Webhook> findByProject_ProjectIdAndUrl(String projectId, String url);

    void delete(Webhook webhook);
}
