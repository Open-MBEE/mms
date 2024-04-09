package org.openmbee.mms.artifacts.pubsub;

import org.openmbee.mms.artifacts.json.ArtifactJson;
import org.openmbee.mms.core.pubsub.EmbeddedHookSubscriber;
import org.openmbee.mms.crud.hooks.ElementUpdateHook;

import java.util.List;

public class ArtifactsElementsHookSubscriber implements EmbeddedHookSubscriber {

    @Override
    public Iterable<Class> getSubscriptions() {
        return List.of(ElementUpdateHook.class);
    }

    @Override
    public void acceptHook(Object payload) {
        if(! (payload instanceof ElementUpdateHook)) {
            return;
        }

        ElementUpdateHook elementUpdateHook = (ElementUpdateHook) payload;
        if(elementUpdateHook.getElements() != null) {
            //Ignore any artifact changes coming in outside of the Artifacts Controller
            elementUpdateHook.getElements().parallelStream().forEach(v ->
                v.remove(ArtifactJson.ARTIFACTS)
            );
        }
    }
}
