package org.openmbee.mms.elp.config;

import org.openmbee.mms.core.config.ProjectSchemas;
import org.openmbee.mms.json.SchemaJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import static org.openmbee.mms.elp.Constants.ELP_SCHEMA;

@Configuration
public class ElpSchemaConfig {

    @Autowired
    public void registerSchema(ProjectSchemas schemas) {
        schemas.addSchema(new SchemaJson().setName(ELP_SCHEMA).setDescription("Element level permissions handling example"));
    }
}
