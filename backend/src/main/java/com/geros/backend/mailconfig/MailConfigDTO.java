package com.geros.backend.mailconfig;

public class MailConfigDTO {

    public static class Request {
        private boolean enabled;
        private String host;
        private int port;
        private String username;
        private String password;
        private boolean useTls;
        private boolean useSsl;
        private String fromAddress;
        private String fromName;

        public boolean isEnabled()      { return enabled; }
        public String getHost()         { return host; }
        public int getPort()            { return port; }
        public String getUsername()     { return username; }
        public String getPassword()     { return password; }
        public boolean isUseTls()       { return useTls; }
        public boolean isUseSsl()       { return useSsl; }
        public String getFromAddress()  { return fromAddress; }
        public String getFromName()     { return fromName; }
    }

    public static class Response {
        private boolean enabled;
        private String host;
        private int port;
        private String username;
        private boolean useTls;
        private boolean useSsl;
        private String fromAddress;
        private String fromName;

        public boolean isEnabled()      { return enabled; }
        public String getHost()         { return host; }
        public int getPort()            { return port; }
        public String getUsername()     { return username; }
        public boolean isUseTls()       { return useTls; }
        public boolean isUseSsl()       { return useSsl; }
        public String getFromAddress()  { return fromAddress; }
        public String getFromName()     { return fromName; }

        public static Response from(MailConfig c) {
            Response r = new Response();
            r.enabled     = c.isEnabled();
            r.host        = c.getHost();
            r.port        = c.getPort();
            r.username    = c.getUsername();
            r.useTls      = c.isUseTls();
            r.useSsl      = c.isUseSsl();
            r.fromAddress = c.getFromAddress();
            r.fromName    = c.getFromName();
            return r;
        }
    }
}
