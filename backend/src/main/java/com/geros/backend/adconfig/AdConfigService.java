package com.geros.backend.adconfig;

import org.springframework.stereotype.Service;

@Service
public class AdConfigService {

    private final AdConfigRepository repository;

    public AdConfigService(AdConfigRepository repository) {
        this.repository = repository;
    }

    public AdConfigDTO.Response get() {
        return AdConfigDTO.Response.from(getOrCreate());
    }

    public AdConfigDTO.Response update(AdConfigDTO.Request request) {
        AdConfig config = getOrCreate();
        config.setEnabled(request.isEnabled());
        config.setHost(request.getHost());
        config.setPort(request.getPort() > 0 ? request.getPort() : 389);
        config.setDomain(request.getDomain());
        config.setBaseDn(request.getBaseDn());
        config.setBindUser(request.getBindUser());
        if (request.getBindPassword() != null && !request.getBindPassword().isBlank())
            config.setBindPassword(request.getBindPassword());
        config.setUseSsl(request.isUseSsl());
        return AdConfigDTO.Response.from(repository.save(config));
    }

    private AdConfig getOrCreate() {
        return repository.findById(1L).orElseGet(() -> repository.save(new AdConfig()));
    }
}
