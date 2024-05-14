package org.openmbee.mms.crud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptimizationConfig {

    @Value("${mms.optimize-for-federated:true}")
    private boolean optimizeForFederated;

    public boolean isOptimizeForFederated() {
        return optimizeForFederated;
    }

    public void setOptimizeForFederated(boolean optimizeForFederated) {
        this.optimizeForFederated = optimizeForFederated;
    }


}
