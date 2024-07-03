package org.openmbee.mms.twc;

public enum TeamworkCloudEndpoints {
    LOGIN("login"),
    GETUSER("admin/users/%s"),
    GETROLESID("resources/%s/roles"),
    GETPROJECTUSERS("resources/%s/roles/%s/users");

    private String path;

    public String getPath() {
        return path;
    }
    
    TeamworkCloudEndpoints(){
    	
    }

    TeamworkCloudEndpoints(String path){
        this.path = path;
    }
    
    
    public String buildUrl(TeamworkCloud twc,String ...params){
    	String url = String.format("%s://%s:%s/osmc/%s", twc.getProtocol(), twc.getUrl(), twc.getPort(), getPath());
    	
    	if(params == null) {
    		return url;
    	}

        return String.format(url, (Object[]) params);
    }

}