package com.geros.backend.adconfig;

public class AdConfigDTO {

    public static class Request {
        private boolean enabled;
        private String host;
        private int port;
        private String domain;
        private String baseDn;
        private String bindUser;
        private String bindPassword;
        private boolean useSsl;

        public boolean isEnabled()       { return enabled; }
        public String getHost()          { return host; }
        public int getPort()             { return port; }
        public String getDomain()        { return domain; }
        public String getBaseDn()        { return baseDn; }
        public String getBindUser()      { return bindUser; }
        public String getBindPassword()  { return bindPassword; }
        public boolean isUseSsl()        { return useSsl; }
    }

    public static class Response {
        private boolean enabled;
        private String host;
        private int port;
        private String domain;
        private String baseDn;
        private String bindUser;
        private boolean useSsl;

        public boolean isEnabled()  { return enabled; }
        public String getHost()     { return host; }
        public int getPort()        { return port; }
        public String getDomain()   { return domain; }
        public String getBaseDn()   { return baseDn; }
        public String getBindUser() { return bindUser; }
        public boolean isUseSsl()   { return useSsl; }

        public static Response from(AdConfig c) {
            Response r = new Response();
            r.enabled  = c.isEnabled();
            r.host     = c.getHost();
            r.port     = c.getPort();
            r.domain   = c.getDomain();
            r.baseDn   = c.getBaseDn();
            r.bindUser = c.getBindUser();
            r.useSsl   = c.isUseSsl();
            return r;
        }
    }
}
