package org.openmbee.mms.twc.config;

import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.twc.security.TwcAuthenticationProvider;
import org.openmbee.mms.twc.utilities.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@ConfigurationProperties("twc")
public class TwcConfig {

    @Autowired
    private RestUtils restUtils;

    private List<TeamworkCloud> instances;

    private boolean useAuthDelegation = false;

    public List<TeamworkCloud> getInstances() {
        return instances;
    }

    public void setInstances(List<TeamworkCloud> instances) {
        this.instances = instances;
    }

    public boolean isUseAuthDelegation() {
        return useAuthDelegation;
    }

    public void setUseAuthDelegation(boolean useAuthDelegation) {
        this.useAuthDelegation = useAuthDelegation;
    }

    public TwcAuthenticationProvider getAuthNProvider(String associatedTWC) {
        if(associatedTWC == null)
            return null;

        String strippedHost = stripHost(associatedTWC);
        for(TeamworkCloud twc : getInstances()){
            if(twc.hasKnownName(strippedHost)){
                return new TwcAuthenticationProvider(restUtils, twc);
            }
        }
        return null;
    }

    public TeamworkCloud getTeamworkCloud(String twcUrl) {
        String host = stripHost(twcUrl);
        for(TeamworkCloud twc : getInstances()) {
            if (twc.hasKnownName(host)) {
                return twc;
            }
        }
        return null;
    }

    private static String stripHost(String url) {
        Pattern pattern = Pattern.compile("(https?://)?([\\w-\\.]*)(:\\d+)?");
        Matcher matcher = pattern.matcher(url);
        if(matcher.matches()) {
            return matcher.group(2);
        }
        return url;
    }
}
