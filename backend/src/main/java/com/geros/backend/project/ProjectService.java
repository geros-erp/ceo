package com.geros.backend.project;

import com.geros.backend.contract.Contract;
import com.geros.backend.contract.ContractRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final ContractRepository contractRepository;
    private final SecurityLogService securityLogService;

    public ProjectService(ProjectRepository repository, 
                          ContractRepository contractRepository, 
                          SecurityLogService securityLogService) {
        this.repository = repository;
        this.contractRepository = contractRepository;
        this.securityLogService = securityLogService;
    }

    public Page<ProjectDTO.Response> findByContract(Long contractId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return repository.findByContractId(contractId, pageable).map(ProjectDTO.Response::from);
        }
        return repository.searchByContractIdAndKeyword(contractId, search, pageable).map(ProjectDTO.Response::from);
    }

    public ProjectDTO.Response findById(Long id) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return ProjectDTO.Response.from(project);
    }

    @Transactional
    public ProjectDTO.Response create(ProjectDTO.Request request, String performedBy) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El codigo de proyecto ya existe");
        }

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("El contrato no existe"));

        Project project = new Project();
        project.setContract(contract);
        mapToEntity(request, project);
        project.setCreatedBy(performedBy);

        Project saved = repository.save(project);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("PROJECT_CREATED")
                .origin("Proyectos")
                .target(saved.getCodigo())
                .performedBy(performedBy)
                .description("Proyecto creado: " + saved.getNombre()));

        return ProjectDTO.Response.from(saved);
    }

    @Transactional
    public ProjectDTO.Response update(Long id, ProjectDTO.Request request, String performedBy) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El codigo de proyecto ya existe");
        }

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("El contrato no existe"));

        project.setContract(contract);
        mapToEntity(request, project);
        project.setUpdatedBy(performedBy);

        Project saved = repository.save(project);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("PROJECT_UPDATED")
                .origin("Proyectos")
                .target(saved.getCodigo())
                .performedBy(performedBy)
                .description("Proyecto actualizado: " + saved.getNombre()));

        return ProjectDTO.Response.from(saved);
    }

    @Transactional
    public void delete(Long id, String performedBy) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        repository.delete(project);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventCode("PROJECT_DELETED")
                .origin("Proyectos")
                .target(project.getCodigo())
                .performedBy(performedBy)
                .description("Proyecto eliminado: " + project.getNombre()));
    }

    private void mapToEntity(ProjectDTO.Request req, Project p) {
        p.setCodigo(req.getCodigo());
        p.setNombre(req.getNombre());
        p.setZona(req.getZona());
        p.setEstado(req.getEstado() != null ? req.getEstado() : "Activo");
        p.setFechaInicio(req.getFechaInicio());
        p.setFechaFin(req.getFechaFin());
        p.setObservaciones(req.getObservaciones());
    }
}
