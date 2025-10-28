package de.bbajor.pvs.ai.extraction;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ExtractionResult<T> {
    
    private T entity;
    private double confidence;
    private Map<String, Double> fieldConfidences;
    private String rawText;
    
    public boolean isConfident(double threshold) {
        return confidence >= threshold;
    }
    
}

