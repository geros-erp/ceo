package com.geros.backend.contract;

import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

    private final ContractRepository repository;
    private final SecurityLogService securityLogService;

    public ContractService(ContractRepository repository, SecurityLogService securityLogService) {
        this.repository = repository;
        this.securityLogService = securityLogService;
    }

    public Page<ContractDTO.Response> findAll(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return repository.findAll(pageable).map(ContractDTO.Response::from);
        }
        return repository.searchContracts(search, pageable).map(ContractDTO.Response::from);
    }

    public ContractDTO.Response findById(Long id) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        return ContractDTO.Response.from(contract);
    }

    @Transactional
    public ContractDTO.Response create(ContractDTO.Request request, String performedBy) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El codigo de contrato ya existe");
        }

        Contract contract = new Contract();
        mapToEntity(request, contract);
        contract.setCreatedBy(performedBy);

        Contract saved = repository.save(contract);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("CONTRACT_CREATED")
                .origin("Contratos")
                .target(saved.getCodigo())
                .performedBy(performedBy)
                .description("Contrato creado: " + saved.getDescripcion()));

        return ContractDTO.Response.from(saved);
    }

    @Transactional
    public ContractDTO.Response update(Long id, ContractDTO.Request request, String performedBy) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (!contract.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El codigo de contrato ya existe");
        }

        mapToEntity(request, contract);
        contract.setUpdatedBy(performedBy);

        Contract saved = repository.save(contract);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("CONTRACT_UPDATED")
                .origin("Contratos")
                .target(saved.getCodigo())
                .performedBy(performedBy)
                .description("Contrato actualizado: " + saved.getDescripcion()));

        return ContractDTO.Response.from(saved);
    }

    @Transactional
    public void delete(Long id, String performedBy) {
        Contract contract = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        repository.delete(contract);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("CONTRACT_DELETED")
                .origin("Contratos")
                .target(contract.getCodigo())
                .performedBy(performedBy)
                .description("Contrato eliminado: " + contract.getDescripcion()));
    }

    private void mapToEntity(ContractDTO.Request req, Contract c) {
        c.setCodigo(req.getCodigo());
        c.setDescripcion(req.getDescripcion());
        c.setEstado(req.getEstado() != null ? req.getEstado() : "Activo");
        c.setResponsable(req.getResponsable());
        c.setContratista(req.getContratista());
        c.setValor(req.getValor());
        c.setOrdenCompra(req.getOrdenCompra());
        c.setCategoria(req.getCategoria());
        c.setFechaInicio(req.getFechaInicio());
        c.setFechaFin(req.getFechaFin());
        c.setObjeto(req.getObjeto());
        c.setAlcance(req.getAlcance());
    }
}
