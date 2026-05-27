package com.geros.backend.inventory.service;

import com.geros.backend.inventory.dto.ProductDTO;
import com.geros.backend.inventory.model.Product;
import com.geros.backend.common.model.UnitOfMeasure;
import com.geros.backend.inventory.repository.ProductRepository;
import com.geros.backend.common.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO.Response> findAll() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO.Response findById(Long id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
    }

    @Transactional
    public ProductDTO.Response createProduct(ProductDTO.Request request) {
        if (productRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("El código del producto ya existe: " + request.getCode());
        }
        Product product = new Product();
        product.setCreatedBy(getCurrentUser());
        return saveOrUpdate(product, request);
    }

    @Transactional
    public ProductDTO.Response updateProduct(Long id, ProductDTO.Request request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getCode().equals(request.getCode()) && productRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("El nuevo código ya está en uso");
        }

        return saveOrUpdate(product, request);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("El producto no existe");
        }
        productRepository.deleteById(id);
    }

    private ProductDTO.Response saveOrUpdate(Product product, ProductDTO.Request request) {
        UnitOfMeasure uom = unitOfMeasureRepository.findById(request.getUnitOfMeasureId())
                .orElseThrow(() -> new RuntimeException("Unidad de medida no encontrada"));

        product.setCode(request.getCode());
        product.setDescription(request.getDescription());
        product.setUnitOfMeasure(uom);
        product.setRequiresSerial(request.getRequiresSerial());
        product.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        product.setUpdatedBy(getCurrentUser());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "system";
        }
        return auth.getName();
    }

    private ProductDTO.Response mapToResponse(Product product) {
        return ProductDTO.Response.builder()
                .id(product.getId())
                .code(product.getCode())
                .description(product.getDescription())
                .unitOfMeasureId(product.getUnitOfMeasure().getId())
                .unitOfMeasureName(product.getUnitOfMeasure().getDescription())
                .requiresSerial(product.isRequiresSerial())
                .isActive(product.isActive())
                .build();
    }
}
