package com.geros.backend.mailconfig;

import jakarta.persistence.*;

@Entity
@Table(name = "mail_config")
public class MailConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(length = 255)
    private String host;

    @Column(nullable = false)
    private int port = 587;

    @Column(length = 150)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "use_tls", nullable = false)
    private boolean useTls = true;

    @Column(name = "use_ssl", nullable = false)
    private boolean useSsl = false;

    @Column(name = "from_address", length = 150)
    private String fromAddress;

    @Column(name = "from_name", length = 100)
    private String fromName;

    public Long getId()              { return id; }

    public boolean isEnabled()       { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getHost()          { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort()             { return port; }
    public void setPort(int port)    { this.port = port; }

    public String getUsername()      { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword()      { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isUseTls()        { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public boolean isUseSsl()        { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }

    public String getFromAddress()   { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getFromName()      { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
