package com.example.AudIon.dto.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    
    @Min(value = 0, message = "Page number must be 0 or greater")
    @Builder.Default
    private int page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    @Builder.Default
    private int size = 20;
    
    @Builder.Default
    private String sort = "createdAt";
    
    @Builder.Default
    private String direction = "desc";
    
    public org.springframework.data.domain.PageRequest toSpringPageRequest() {
        org.springframework.data.domain.Sort.Direction sortDirection = 
            "asc".equalsIgnoreCase(direction) ? 
                org.springframework.data.domain.Sort.Direction.ASC : 
                org.springframework.data.domain.Sort.Direction.DESC;
        
        return org.springframework.data.domain.PageRequest.of(
            page, 
            size, 
            org.springframework.data.domain.Sort.by(sortDirection, sort)
        );
    }
}