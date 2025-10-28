package de.bbajor.pvs.ai.extraction;

public interface EntityExtractor<T> {
    
    ExtractionResult<T> extract(String text);
    
    Class<T> getEntityType();
    
    String getEntityName();
    
}

