package com.geros.backend.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservedUsernameService {

    private final ReservedUsernameRepository repository;

    public List<ReservedUsernameDTO.Response> findAll() {
        return repository.findAll().stream()
                .map(ReservedUsernameDTO.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservedUsernameDTO.Response create(ReservedUsernameDTO.Request request) {
        if (repository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new RuntimeException("El usuario '" + request.getUsername() + "' ya está reservado.");
        }

        ReservedUsername reserved = new ReservedUsername();
        reserved.setUsername(request.getUsername().toLowerCase().trim());
        reserved.setDescription(request.getDescription());
        reserved.setSystem(false);

        return ReservedUsernameDTO.Response.from(repository.save(reserved));
    }

    @Transactional
    public void delete(Long id) {
        ReservedUsername entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No encontrado"));
        if (entity.isSystem()) throw new RuntimeException("No se pueden eliminar registros del sistema");
        repository.delete(entity);
    }
}