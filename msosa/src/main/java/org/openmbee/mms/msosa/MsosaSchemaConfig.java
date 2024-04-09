package org.openmbee.mms.msosa;

import org.openmbee.mms.core.config.ProjectSchemas;
import org.openmbee.mms.json.SchemaJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsosaSchemaConfig {

    @Autowired
    public void registerSchema(ProjectSchemas schemas) {
        schemas.addSchema(new SchemaJson().setName("msosa").setDescription("msosa/md json handling, creates holding bins"));
    }
}
