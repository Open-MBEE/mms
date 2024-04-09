package org.openmbee.mms.rdb.config;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.openmbee.mms.core.config.ContextObject;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.data.domains.scoped.NodeType;
import org.openmbee.mms.rdb.datasources.CrudDataSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseDefinitionService {

    private static final Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
    private static final String COPY_SQL = "INSERT INTO \"%s\" SELECT * FROM \"%s\"";
    private static final String COPY_IDX = "SELECT SETVAL('%s_id_seq', COALESCE((SELECT MAX(id) FROM \"%s\"), 1), true)";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private CrudDataSources crudDataSources;
    private Environment env;

    @Autowired
    public void setEnv(Environment env) {
        this.env = env;
    }

    @Autowired
    public void setCrudDataSources(CrudDataSources crudDataSources) {
        this.crudDataSources = crudDataSources;
    }

    public boolean createProjectDatabase(Project project) throws SQLException {
        ContextHolder.setContext(null);        
        JdbcTemplate jdbcTemplate = new JdbcTemplate(
            crudDataSources.getDataSource(ContextHolder.getContext().getKey()));
        List<Object> created = new ArrayList<>();
        try {
            logger.info("Creating database for " + project.getProjectId());
            jdbcTemplate.execute("CREATE DATABASE " + databaseProjectString(project)); //lgtm[java/sql-injection]
            created.add("Created Database");

            generateProjectSchemaFromModels(project);
            created.add("Created Tables");

        } catch (DataAccessException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                logger.error(cause.getLocalizedMessage());
            }
            logger.error(e.getLocalizedMessage());
            if (cause != null && cause.getLocalizedMessage() != null && cause.getLocalizedMessage().toLowerCase().contains("exists")) {
                generateProjectSchemaFromModels(project);
                throw (new SQLException("Database already exists"));
            } else {
                logger.error(e.getLocalizedMessage());
                throw (e);
            }
        }
        return !created.isEmpty();
    }

    public void deleteProjectDatabase(String projectId) throws SQLException {
        try (Connection connection = crudDataSources.getDataSource(ContextObject.DEFAULT_PROJECT).getConnection();
                Statement statement = connection.createStatement()) {
            if ("org.postgresql.Driver".equals(env.getProperty("spring.datasource.driver-class-name"))) {
                statement.setQueryTimeout(60);
                statement.execute(connection.nativeSQL(
                    "SELECT pid, pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '"
                        + databaseProjectString(projectId) + "';"));
            }
            statement.executeUpdate(connection.nativeSQL("DROP DATABASE " + databaseProjectString(projectId)));
			
			//TODO: if using PG 13, can use the following
			//statement.executeUpdate(connection.nativeSQL("DROP DATABASE " + databaseProjectString(project) + " WITH (FORCE)"));
        }
    }

    private String databaseProjectString(Project project) {
		return databaseProjectString(project.getProjectId());
    }

    private String databaseProjectString(String projectId) {
        String prefix = env.getProperty("rdb.project.prefix", "");
        return String.format("\"%s_%s\"", prefix, projectId);
    }

    public void createBranch() {
        logger.info(
            "Current Context is: {} on ref {}", ContextHolder.getContext().getProjectId(),
            ContextHolder.getContext().getBranchId());
        generateBranchSchemaFromModels();
    }

    public void generateProjectSchemaFromModels(Project project) throws SQLException {
        crudDataSources.addDataSource(project);

        ContextHolder.setContext(project.getProjectId());

        MetadataSources metadata = new MetadataSources(
            new StandardServiceRegistryBuilder()
                .applySettings(getSchemaProperties())
                .build()
        );

        metadata.addAnnotatedClass(Branch.class);
        metadata.addAnnotatedClass(Commit.class);
        metadata.addAnnotatedClass(Node.class);
        metadata.addAnnotatedClass(NodeType.class);

        new SchemaExport()
            .setHaltOnError(false)
            .setFormat(true)
            .setDelimiter(";")
            .createOnly(EnumSet.of(TargetType.DATABASE),
                metadata.getMetadataBuilder().build());
    }

    public void generateBranchSchemaFromModels() {
        MetadataSources metadata = new MetadataSources(
            new StandardServiceRegistryBuilder()
                .applySettings(getSchemaProperties())
                .build()
        );

        metadata.addAnnotatedClass(Node.class);

        new SchemaExport()
            .setHaltOnError(true)
            .setFormat(true)
            .setDelimiter(";")
            .createOnly(EnumSet.of(TargetType.DATABASE),
                metadata.getMetadataBuilder().build());
    }

    public void copyTablesFromParent(String target, String parent, String parentCommit) {
        if (parentCommit != null) {
            return;
        }

        final String targetNodeTable = String.format("nodes%s", getNodeTableName(target));
        StringBuilder parentNodeTable = new StringBuilder("nodes");

        if (parent != null && !parent.equalsIgnoreCase("master")) {
            parentNodeTable.append(String.format("%s", getNodeTableName(parent)));
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(
            crudDataSources.getDataSource(ContextHolder.getContext().getKey()));

        jdbcTemplate.execute("BEGIN");

        jdbcTemplate.execute(String.format(COPY_SQL, targetNodeTable, parentNodeTable)); //lgtm[java/sql-injection]

        //reset db auto increment sequence for postgresql only
        if ("org.postgresql.Driver".equals(env.getProperty("spring.datasource.driver-class-name"))) {
            jdbcTemplate.execute(String.format(COPY_IDX, targetNodeTable, parentNodeTable)); //lgtm[java/sql-injection]
        }

        jdbcTemplate.execute("COMMIT");
    }

    private Map<String, Object> getSchemaProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties
            .put("hibernate.dialect", env.getProperty("spring.jpa.properties.hibernate.dialect",
                "org.hibernate.dialect.PostgreSQL10Dialect"));

        properties.put("hibernate.jdbc.lob.non_contextual_creation", true);
        properties.put("hibernate.current_session_context_class", "thread");
        properties.put("hibernate.multiTenancy", MultiTenancyStrategy.DATABASE);

        DataSourceBasedMultiTenantConnectionProviderImpl dataSourceProvider = new DataSourceBasedMultiTenantConnectionProviderImpl();
        dataSourceProvider.setCrudDataSources(crudDataSources);
        properties.put("hibernate.multi_tenant_connection_provider", dataSourceProvider);

        CurrentTenantIdentifierResolverImpl identifierResolver = new CurrentTenantIdentifierResolverImpl();
        properties.put("hibernate.tenant_identifier_resolver", identifierResolver);

        properties.put("hibernate.physical_naming_strategy",
            "org.openmbee.mms.rdb.config.SuffixedPhysicalNamingStrategy");

        return properties;
    }

    /**
     * Returns the suffix that should be appended to 'nodes' table given refId
     * Empty for 'master', lowercased refId if refId.length <= 50, SHA-1 hash of refId otherwise
     *
     * @param refId
     * @return
     */
    static public String getNodeTableName(String refId) {
        String res = refId;
        if (refId.equals(ContextObject.MASTER_BRANCH)) {
            res = "";
        } else if (refId.length() <= 50) {
            res = refId.toLowerCase();
        } else {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] encodedhash = digest.digest(refId.getBytes(StandardCharsets.UTF_8));
                res = new BigInteger(1, encodedhash).toString(16).toLowerCase();
            } catch (NoSuchAlgorithmException e) {
                res = refId.toLowerCase().substring(0, 50);
            }
        }
        return res;
    }
}
