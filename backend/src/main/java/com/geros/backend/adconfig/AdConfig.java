package com.geros.backend.adconfig;

import jakarta.persistence.*;

@Entity
@Table(name = "ad_config")
public class AdConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(length = 255)
    private String host;

    @Column(nullable = false)
    private int port = 389;

    @Column(length = 100)
    private String domain;

    @Column(name = "base_dn", length = 255)
    private String baseDn;

    @Column(name = "bind_user", length = 255)
    private String bindUser;

    @Column(name = "bind_password", length = 255)
    private String bindPassword;

    @Column(name = "use_ssl", nullable = false)
    private boolean useSsl = false;

    public Long getId()              { return id; }

    public boolean isEnabled()       { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getHost()          { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort()             { return port; }
    public void setPort(int port)    { this.port = port; }

    public String getDomain()        { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getBaseDn()        { return baseDn; }
    public void setBaseDn(String baseDn) { this.baseDn = baseDn; }

    public String getBindUser()      { return bindUser; }
    public void setBindUser(String bindUser) { this.bindUser = bindUser; }

    public String getBindPassword()  { return bindPassword; }
    public void setBindPassword(String bindPassword) { this.bindPassword = bindPassword; }

    public boolean isUseSsl()        { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
}
