package de.bbajor.pvs.ai.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.ai.domain.AiUsageLog;
import de.bbajor.pvs.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageLogRepository repository;

    @Transactional
    public void logUsage(String provider, String requestType, Long tokenCount, boolean success,
            String errorMessage) {
        AiUsageLog log = new AiUsageLog();
        log.setTimestamp(LocalDateTime.now());
        log.setProvider(provider);
        log.setRequestType(requestType);
        log.setTokenCount(tokenCount);
        log.setStatus(success ? "success" : "error");
        log.setErrorMessage(errorMessage);
        repository.save(log);
    }

    public long getUsageCountForMonth(String provider, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return repository.countByProviderAndMonth(provider, start, end);
    }

    public List<AiUsageLog> getUsageForMonth(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return repository.findByMonth(start, end);
    }

    public long getUsageCountForCurrentMonth(String provider) {
        return getUsageCountForMonth(provider, YearMonth.now());
    }

}

