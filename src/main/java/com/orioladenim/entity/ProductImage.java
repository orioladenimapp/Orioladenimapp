package com.orioladenim.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(exclude = "product")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La ruta de la imagen es requerida")
    @Column(name = "image_path")
    private String imagePath;

    @NotBlank(message = "El nombre del archivo es requerido")
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Método para obtener la URL completa de la imagen
    public String getImageUrl() {
        return "/uploads/" + this.imagePath;
    }

    // Método para obtener el thumbnail
    public String getThumbnailUrl() {
        return "/uploads/thumbnails/" + this.imagePath;
    }

    // Getters y Setters manuales (por si Lombok no funciona)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}

