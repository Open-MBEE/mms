package org.openmbee.mms.twc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.twc.constants.TwcConstants;

public class TeamworkCloud {
	private String url;
	private String protocol;
	private String port;
	private List<String> aliases;
	private Set<String> knownNames;
	private String adminUsername;
	private String adminPwd;
	private TeamworkCloudRolesMapping roles;
	private Map<String, List<String>> twcmmsRolesMap;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	// TODO:: add encrypted password storage
	public String getAdminPwd() {
		return adminPwd;
	}

	public void setAdminPwd(String adminPwd) {
		this.adminPwd = adminPwd;
	}

	synchronized public TeamworkCloudRolesMapping getRoles() {
		return roles;
	}

	synchronized public void setRoles(TeamworkCloudRolesMapping roles) {
		this.roles = roles;
	}

	synchronized public void setTwcmmsRolesMap(Map<String, List<String>> twcmmsRolesMap) {
		this.twcmmsRolesMap = twcmmsRolesMap;
	}

    synchronized public Set<String> getKnownNames() {
        if(knownNames == null){
            HashSet<String> tempNames = new HashSet<>();
            tempNames.add(url.toLowerCase());
            if(aliases != null) {
                aliases.stream().map(String::toLowerCase).forEach(v -> tempNames.add(v));
            }
            knownNames = tempNames;
        }
        return knownNames;
    }

    public boolean hasKnownName(String name) {
        return getKnownNames().contains(name.toLowerCase());
    }

	synchronized public Map<String, List<String>> getTwcmmsRolesMap() {

		if (twcmmsRolesMap == null) {

			twcmmsRolesMap = new HashMap<String, List<String>>();

			twcmmsRolesMap.put(Privileges.PROJECT_READ.toString(),
					Arrays.asList(roles.getProject_read().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.PROJECT_EDIT.toString(),
					Arrays.asList(roles.getProject_edit().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.PROJECT_READ_COMMITS.toString(),
					Arrays.asList(roles.getProject_read_commits().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.PROJECT_READ_PERMISSIONS.toString(),
					Arrays.asList(roles.getProject_read_permissions().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.PROJECT_UPDATE_PERMISSIONS.toString(),
					Arrays.asList(roles.getProject_update_permissions().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.BRANCH_READ.toString(),
					Arrays.asList(roles.getBranch_read().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.BRANCH_READ_PERMISSIONS.toString(),
					Arrays.asList(roles.getBranch_read_permissions().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.BRANCH_EDIT_CONTENT.toString(),
					Arrays.asList(roles.getBranch_edit_content().split(TwcConstants.COMMA)));

			twcmmsRolesMap.put(Privileges.BRANCH_UPDATE_PERMISSIONS.toString(),
					Arrays.asList(roles.getBranch_update_permissions().split(TwcConstants.COMMA)));
		}

		return twcmmsRolesMap;
	}

	public boolean hasTwcRoles(String privilege) {
		return getTwcmmsRolesMap().containsKey(privilege);
	}

}
