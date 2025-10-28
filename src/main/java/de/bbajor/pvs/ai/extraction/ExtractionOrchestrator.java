package de.bbajor.pvs.ai.extraction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExtractionOrchestrator {

    private final AiProperties aiProperties;
    private final Map<Class<?>, EntityExtractor<?>> extractors = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public void setExtractors(List<EntityExtractor<?>> extractorList) {
        for (EntityExtractor<?> extractor : extractorList) {
            extractors.put(extractor.getEntityType(), extractor);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ExtractionResult<T> extract(String text, Class<T> entityType) {
        EntityExtractor<T> extractor = (EntityExtractor<T>) extractors.get(entityType);
        if (extractor == null) {
            throw new IllegalArgumentException("No extractor found for entity type: " + entityType.getName());
        }

        ExtractionResult<T> result = extractor.extract(text);

        // Validate confidence threshold
        double threshold = aiProperties.getExtraction().getConfidenceThreshold();
        if (!result.isConfident(threshold)) {
            // Still return result, but UI should show warning
        }

        return result;
    }

    public boolean hasExtractor(Class<?> entityType) {
        return extractors.containsKey(entityType);
    }

}

