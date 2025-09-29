package com.orioladenim.controller;

import com.orioladenim.entity.Product;
import com.orioladenim.entity.Category;
import com.orioladenim.entity.Color;
import com.orioladenim.entity.ProductImage;
import com.orioladenim.enums.Talle;
import com.orioladenim.enums.Genero;
import com.orioladenim.enums.Temporada;
import com.orioladenim.repo.ProductRepository;
import com.orioladenim.repo.ProductImageRepository;
import com.orioladenim.service.CategoryService;
import com.orioladenim.service.ColorService;
import com.orioladenim.service.ImageProcessingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/products")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductImageRepository productImageRepository;
    
    @Autowired
    private ImageProcessingService imageProcessingService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private ColorService colorService;

    @GetMapping
    public String listProducts(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String activo,
                              Model model) {
        List<Product> products = productRepository.findAll();
        
        // Aplicar filtros
        if (search != null && !search.trim().isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        if (category != null && !category.trim().isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCategories().stream()
                            .anyMatch(c -> c.getName().toLowerCase().contains(category.toLowerCase())))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        if (activo != null && !activo.trim().isEmpty()) {
            Boolean activoFiltro = Boolean.parseBoolean(activo);
            products = products.stream()
                    .filter(p -> p.getActivo().equals(activoFiltro))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("activo", activo);
        return "admin/product-list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        Product product = new Product();
        product.setPId(null); // Asegurar que no tenga ID para detectar como nuevo
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("colors", colorService.getActiveColors());
        model.addAttribute("talles", Talle.values());
        model.addAttribute("generos", Genero.values());
        model.addAttribute("temporadas", Temporada.values());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String addProduct(@Valid Product product, 
                           @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                           @RequestParam(value = "colorIds", required = false) List<Long> colorIds,
                           @RequestParam(value = "talleNames", required = false) List<String> talleNames,
                           @RequestParam(value = "generoNames", required = false) List<String> generoNames,
                           @RequestParam(value = "temporadaNames", required = false) List<String> temporadaNames,
                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("colors", colorService.getActiveColors());
            model.addAttribute("talles", Talle.values());
            model.addAttribute("generos", Genero.values());
            model.addAttribute("temporadas", Temporada.values());
            return "admin/product-form";
        }
        
        // Manejar categorías múltiples
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> selectedCategories = new ArrayList<>();
            for (Long categoryId : categoryIds) {
                categoryService.findById(categoryId).ifPresent(selectedCategories::add);
            }
            product.setCategories(selectedCategories);
        }
        
        // Manejar colores múltiples
        if (colorIds != null && !colorIds.isEmpty()) {
            List<Color> selectedColors = new ArrayList<>();
            for (Long colorId : colorIds) {
                colorService.getColorById(colorId).ifPresent(selectedColors::add);
            }
            product.setColores(selectedColors);
        }
        
        // Manejar talles múltiples (enum)
        if (talleNames != null && !talleNames.isEmpty()) {
            List<Talle> selectedTalles = new ArrayList<>();
            for (String talleName : talleNames) {
                try {
                    Talle talle = Talle.valueOf(talleName);
                    selectedTalles.add(talle);
                } catch (IllegalArgumentException e) {
                    // Ignorar talles inválidos
                }
            }
            product.setTalles(selectedTalles);
        }
        
        // Manejar géneros múltiples (enum)
        if (generoNames != null && !generoNames.isEmpty()) {
            List<Genero> selectedGeneros = new ArrayList<>();
            for (String generoName : generoNames) {
                try {
                    Genero genero = Genero.valueOf(generoName);
                    selectedGeneros.add(genero);
                } catch (IllegalArgumentException e) {
                    // Ignorar géneros inválidos
                }
            }
            product.setGeneros(selectedGeneros);
        }
        
        // Manejar temporadas múltiples (enum)
        if (temporadaNames != null && !temporadaNames.isEmpty()) {
            List<Temporada> selectedTemporadas = new ArrayList<>();
            for (String temporadaName : temporadaNames) {
                try {
                    Temporada temporada = Temporada.valueOf(temporadaName);
                    selectedTemporadas.add(temporada);
                } catch (IllegalArgumentException e) {
                    // Ignorar temporadas inválidas
                }
            }
            product.setTemporadas(selectedTemporadas);
        }
        
        product.setFechaCreacion(java.time.LocalDateTime.now());
        product.setFechaActualizacion(java.time.LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        return "redirect:/admin/products/edit/" + savedProduct.getPId();
    }

    @GetMapping("/edit/{pId}")
    public String editProduct(@PathVariable Integer pId, Model model) {
        Product product = productRepository.findById(pId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("colors", colorService.getActiveColors());
        model.addAttribute("talles", Talle.values());
        model.addAttribute("generos", Genero.values());
        model.addAttribute("temporadas", Temporada.values());
        return "admin/product-form";
    }

    @PostMapping("/edit/{pId}")
    public String updateProduct(@PathVariable Integer pId, 
                              @Valid Product product,
                              @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                              @RequestParam(value = "colorIds", required = false) List<Long> colorIds,
                              @RequestParam(value = "talleNames", required = false) List<String> talleNames,
                              @RequestParam(value = "generoNames", required = false) List<String> generoNames,
                              @RequestParam(value = "temporadaNames", required = false) List<String> temporadaNames,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("colors", colorService.getActiveColors());
            model.addAttribute("talles", Talle.values());
            model.addAttribute("generos", Genero.values());
            model.addAttribute("temporadas", Temporada.values());
            return "admin/product-form";
        }
        
        // Obtener el producto existente
        Product existingProduct = productRepository.findById(pId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        // Actualizar campos básicos
        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setQty(product.getQty());
        existingProduct.setDescripcion(product.getDescripcion());
        existingProduct.setMaterial(product.getMaterial());
        existingProduct.setCuidados(product.getCuidados());
        // Los géneros y temporadas se manejan por separado más abajo
        existingProduct.setEdadRecomendada(product.getEdadRecomendada());
        existingProduct.setTallasDisponibles(product.getTallasDisponibles());
        existingProduct.setColoresDisponibles(product.getColoresDisponibles());
        existingProduct.setEsDestacado(product.getEsDestacado());
        existingProduct.setEsNuevo(product.getEsNuevo());
        existingProduct.setDescuentoPorcentaje(product.getDescuentoPorcentaje());
        existingProduct.setPrecioOriginal(product.getPrecioOriginal());
        existingProduct.setActivo(product.getActivo());
        existingProduct.setColorEntity(product.getColorEntity());
        existingProduct.setMedidas(product.getMedidas());
        existingProduct.setColor(product.getColor());
        
        // Manejar categorías múltiples
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> selectedCategories = new ArrayList<>();
            for (Long categoryId : categoryIds) {
                categoryService.findById(categoryId).ifPresent(selectedCategories::add);
            }
            existingProduct.setCategories(selectedCategories);
        } else {
            existingProduct.setCategories(new ArrayList<>());
        }
        
        // Manejar colores múltiples
        if (colorIds != null && !colorIds.isEmpty()) {
            List<Color> selectedColors = new ArrayList<>();
            for (Long colorId : colorIds) {
                colorService.getColorById(colorId).ifPresent(selectedColors::add);
            }
            existingProduct.setColores(selectedColors);
        } else {
            existingProduct.setColores(new ArrayList<>());
        }
        
        // Manejar talles múltiples
        if (talleNames != null && !talleNames.isEmpty()) {
            List<Talle> selectedTalles = new ArrayList<>();
            for (String talleName : talleNames) {
                try {
                    Talle talle = Talle.valueOf(talleName);
                    selectedTalles.add(talle);
                } catch (IllegalArgumentException e) {
                    // Ignorar talles inválidos
                }
            }
            existingProduct.setTalles(selectedTalles);
        } else {
            existingProduct.setTalles(new ArrayList<>());
        }
        
        // Manejar géneros múltiples
        if (generoNames != null && !generoNames.isEmpty()) {
            List<Genero> selectedGeneros = new ArrayList<>();
            for (String generoName : generoNames) {
                try {
                    Genero genero = Genero.valueOf(generoName);
                    selectedGeneros.add(genero);
                } catch (IllegalArgumentException e) {
                    // Ignorar géneros inválidos
                }
            }
            existingProduct.setGeneros(selectedGeneros);
        } else {
            existingProduct.setGeneros(new ArrayList<>());
        }
        
        // Manejar temporadas múltiples
        if (temporadaNames != null && !temporadaNames.isEmpty()) {
            List<Temporada> selectedTemporadas = new ArrayList<>();
            for (String temporadaName : temporadaNames) {
                try {
                    Temporada temporada = Temporada.valueOf(temporadaName);
                    selectedTemporadas.add(temporada);
                } catch (IllegalArgumentException e) {
                    // Ignorar temporadas inválidas
                }
            }
            existingProduct.setTemporadas(selectedTemporadas);
        } else {
            existingProduct.setTemporadas(new ArrayList<>());
        }
        
        existingProduct.setFechaActualizacion(java.time.LocalDateTime.now());
        productRepository.save(existingProduct);
        return "redirect:/admin/products";
    }

    @GetMapping("/delete/{pId}")
    public String deleteProduct(@PathVariable Integer pId) {
        productRepository.deleteById(pId);
        return "redirect:/admin/products";
    }

    @GetMapping("/{pId}/images")
    public String manageImages(@PathVariable Integer pId, Model model) {
        Product product = productRepository.findById(pId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        model.addAttribute("product", product);
        return "admin/product-images";
    }

    @PostMapping("/{pId}/images/upload")
    @ResponseBody
    public java.util.Map<String, Object> uploadImages(@PathVariable Integer pId, 
                              @RequestParam("images") MultipartFile[] images) {
        try {
            Product product = productRepository.findById(pId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            int savedCount = 0;
            for (int i = 0; i < images.length; i++) {
                MultipartFile file = images[i];
                if (!file.isEmpty()) {
                    // Usar el servicio de procesamiento de imágenes existente
                    ProductImage productImage = imageProcessingService.processAndSaveImage(file, pId, i == 0);
                    productImage.setProduct(product);
                    productImage.setDisplayOrder(i);
                    
                    productImageRepository.save(productImage);
                    savedCount++;
                }
            }
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Imágenes asociadas correctamente");
            response.put("count", savedCount);
            
            return response;
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Error al procesar las imágenes: " + e.getMessage());
            return response;
        }
    }
    
    @PostMapping("/{pId}/toggle-status")
    @ResponseBody
    public java.util.Map<String, Object> toggleProductStatus(@PathVariable Integer pId) {
        try {
            Product product = productRepository.findById(pId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            // Cambiar el estado
            product.setActivo(!product.getActivo());
            productRepository.save(product);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", product.getActivo() ? "Producto activado y publicado" : "Producto desactivado y oculto del catálogo");
            response.put("activo", product.getActivo());
            
            return response;
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Error al cambiar el estado del producto: " + e.getMessage());
            return response;
        }
    }
}

