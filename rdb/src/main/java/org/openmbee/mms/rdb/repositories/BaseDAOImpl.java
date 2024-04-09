package org.openmbee.mms.rdb.repositories;

import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.rdb.config.DatabaseDefinitionService;
import org.openmbee.mms.rdb.datasources.CrudDataSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.openmbee.mms.core.config.Constants.BRANCH_ID_VALID_PATTERN;

public abstract class BaseDAOImpl {

    private CrudDataSources crudDataSources;
    private PlatformTransactionManager transactionManager;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public void setCrudDataSources(CrudDataSources crudDataSources) {
        this.crudDataSources = crudDataSources;
    }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public JdbcTemplate getConn() {
        return new JdbcTemplate(crudDataSources.getDataSource(ContextHolder.getContext().getKey()));
    }

    public String getSuffix() {
        String refId = ContextHolder.getContext().getBranchId();
        if (BRANCH_ID_VALID_PATTERN.matcher(refId).matches()) {
            return DatabaseDefinitionService.getNodeTableName(refId);
        } else {
            throw new BadRequestException("Bad branch id, aborting current operation.");
        }
    }
}
