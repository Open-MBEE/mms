package org.openmbee.mms.crud.controllers.elements;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.openmbee.mms.core.objects.ElementsCommitResponse;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.services.CommitService;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.crud.controllers.BaseController;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.core.pubsub.EmbeddedHookService;
import org.openmbee.mms.crud.hooks.ElementUpdateHook;
import org.openmbee.mms.json.ElementJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/projects/{projectId}/refs/{refId}/elements")
@Tag(name = "Elements")
public class ElementsController extends BaseController {

    private EmbeddedHookService embeddedHookService;
    private CommitService commitService;

    @Value("${mms.stream.batch.size:5000}")
    private int streamLimit;

    @Autowired
    public void setEmbeddedHookService(EmbeddedHookService embeddedHookService) {
        this.embeddedHookService = embeddedHookService;
    }

    @Autowired
    public void setCommitService(@Qualifier("defaultCommitService")CommitService commitService) {
        this.commitService = commitService;
    }

    private static final JsonFactory jfactory = new JsonFactory();

    @GetMapping
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    @ApiResponse(responseCode = "200", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = ElementsResponse.class)),
        @Content(mediaType = "application/x-ndjson")
    })
    public ResponseEntity<StreamingResponseBody> getAllElements(
            @PathVariable String projectId,
            @PathVariable String refId,
            @RequestParam(required = false) String commitId,
            @RequestParam(required = false) Map<String, String> params,
            @Parameter(hidden = true) @RequestHeader(value = "Accept", defaultValue = "application/json") String accept) {

        NodeService nodeService = getNodeService(projectId);
        if (commitId != null && !commitId.isEmpty()) {
            commitService.getCommit(projectId, commitId); //check commit exists
        }
        StreamingResponseBody stream = outputStream -> nodeService.readAsStream(projectId, refId, params, outputStream, accept);
        return ResponseEntity.ok()
            .header("Content-Type", accept.equals("application/x-ndjson") ? accept : "application/json")
            .body(stream);
    }

    @GetMapping(value = "/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public ElementsResponse getElement(
            @PathVariable String projectId,
            @PathVariable String refId,
            @PathVariable String elementId,
            @RequestParam(required = false) String commitId,
            @RequestParam(required = false) Map<String, String> params) {

        NodeService nodeService = getNodeService(projectId);
        ElementsResponse res = nodeService.read(projectId, refId, elementId, params);
        handleSingleResponse(res);
        return res;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_EDIT_CONTENT', false)")
    public ElementsCommitResponse createOrUpdateElements(
            @PathVariable String projectId,
            @PathVariable String refId,
            @RequestBody ElementsRequest req,
            @RequestParam(required = false) String overwrite,
            @RequestParam(required = false) Map<String, String> params,
            Authentication auth) {

        embeddedHookService.hook(new ElementUpdateHook(ElementUpdateHook.Action.ADD_UPDATE, projectId, refId,
            req.getElements(), params, auth));

        ElementsResponse response = new ElementsResponse();
        if (!req.getElements().isEmpty()) {
            NodeService nodeService = getNodeService(projectId);
            return nodeService.createOrUpdate(projectId, refId, req, params, auth.getName());
        }
        throw new BadRequestException(response.addMessage(CrudConstants.EMPTY));
    }
    /*
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_EDIT_CONTENT', false)")
    */
    public ResponseEntity<StreamingResponseBody> createOrUpdateElementsStream(
            @PathVariable String projectId,
            @PathVariable String refId,
            @RequestParam(required = false) Map<String, String> params,
            @Parameter(hidden = true) @RequestHeader(value = "Accept", defaultValue = "application/json") String accept,
            Authentication auth,
            HttpEntity<byte[]> requestEntity) {

        String commitId = UUID.randomUUID().toString(); // Generate a commitId from the start
        params.put("commitId", commitId);
        ElementsRequest req = new ElementsRequest();
        List<ElementJson> elements = new ArrayList<>();

        InputStream stream = new ByteArrayInputStream(Objects.requireNonNull(requestEntity.getBody()));
        StreamingResponseBody response =  outputStream -> {
            ObjectMapper om = new ObjectMapper();
            try (JsonParser parser = jfactory.createParser(stream)) {
                if(parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new BadRequestException("Expected an object");
                }
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    logger.debug("Current Token: " + parser.getCurrentName());
                    if (parser.nextToken() == JsonToken.START_ARRAY && "elements".equals(parser.getCurrentName())) {
                        logger.debug("Found Array: " + parser.getCurrentName());
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            ElementJson node = om.readValue(parser, ElementJson.class);
                            elements.add(node);
                            //outputStream.write(node.getType().getBytes(StandardCharsets.UTF_8));
                            //outputStream.write(node.get("id").toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                req.setElements(elements);
                if (!req.getElements().isEmpty()) {
                    NodeService nodeService = getNodeService(projectId);
                    nodeService.createOrUpdate(projectId, refId, req, params, auth.getName());
                }
            } catch (IOException e) {
                logger.debug("Error in stream handling: ", e);
            }
        };
        return ResponseEntity.ok()
            .header("Content-Type", accept.equals("application/x-ndjson") ? accept : "application/json")
            .body(response);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public ElementsResponse getElements(
            @PathVariable String projectId,
            @PathVariable String refId,
            @RequestBody ElementsRequest req,
            @RequestParam(required = false) String commitId,
            @RequestParam(required = false) Map<String, String> params) {

        ElementsResponse response = new ElementsResponse();
        if (!req.getElements().isEmpty()) {
            NodeService nodeService = getNodeService(projectId);
            return nodeService.read(projectId, refId, req, params);
        }
        throw new BadRequestException(response.addMessage(CrudConstants.EMPTY));
    }

    @DeleteMapping(value = "/{elementId}")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_EDIT_CONTENT', false)")
    public ElementsCommitResponse deleteElement(
            @PathVariable String projectId,
            @PathVariable String refId,
            @PathVariable String elementId,
            Authentication auth) {

        ElementsCommitResponse res = getNodeService(projectId).delete(projectId, refId, elementId, auth.getName());
        handleSingleResponse(res);
        return res;
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_EDIT_CONTENT', false)")
    public ElementsResponse deleteElements(
            @PathVariable String projectId,
            @PathVariable String refId,
            @RequestBody ElementsRequest req,
            Authentication auth) {

        return getNodeService(projectId).delete(projectId, refId, req, auth.getName());
    }
}
