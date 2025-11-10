package de.bbajor.pvs.kbv.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.bbajor.pvs.kbv.client.dto.KbvChangeComparison;
import de.bbajor.pvs.kbv.client.dto.KbvCostCarrierDto;
import de.bbajor.pvs.kbv.client.dto.KbvIcdEntryDto;
import de.bbajor.pvs.kbv.client.dto.KbvImportHistoryDto;
import de.bbajor.pvs.kbv.client.dto.KbvInsuranceDto;

@Component
public class KbvServiceClient {

    private static final Logger log = LoggerFactory.getLogger(KbvServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public KbvServiceClient(
            RestTemplate restTemplate,
            @Value("${kbv.service.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Optional<List<KbvIcdEntryDto>> getIcdEntries(String quarter, String code, LocalDate date) {
        try {
            StringBuilder url = new StringBuilder(baseUrl + "/api/kbv/icd");
            boolean firstParam = true;
            if (quarter != null) {
                url.append(firstParam ? "?" : "&").append("quarter=").append(quarter);
                firstParam = false;
            }
            if (code != null) {
                url.append(firstParam ? "?" : "&").append("code=").append(code);
                firstParam = false;
            }
            if (date != null) {
                url.append(firstParam ? "?" : "&").append("date=").append(date);
            }

            ResponseEntity<List<KbvIcdEntryDto>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<KbvIcdEntryDto>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error fetching ICD entries from KBV service", e);
        }
        return Optional.empty();
    }

    public Optional<List<KbvCostCarrierDto>> getCostCarriers(String quarter, String code, LocalDate date) {
        try {
            StringBuilder url = new StringBuilder(baseUrl + "/api/kbv/cost-carriers");
            boolean firstParam = true;
            if (quarter != null) {
                url.append(firstParam ? "?" : "&").append("quarter=").append(quarter);
                firstParam = false;
            }
            if (code != null) {
                url.append(firstParam ? "?" : "&").append("code=").append(code);
                firstParam = false;
            }
            if (date != null) {
                url.append(firstParam ? "?" : "&").append("date=").append(date);
            }

            ResponseEntity<List<KbvCostCarrierDto>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<KbvCostCarrierDto>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error fetching cost carriers from KBV service", e);
        }
        return Optional.empty();
    }

    public Optional<List<KbvInsuranceDto>> getInsurances(String quarter, String code, LocalDate date) {
        try {
            StringBuilder url = new StringBuilder(baseUrl + "/api/kbv/insurances");
            boolean firstParam = true;
            if (quarter != null) {
                url.append(firstParam ? "?" : "&").append("quarter=").append(quarter);
                firstParam = false;
            }
            if (code != null) {
                url.append(firstParam ? "?" : "&").append("code=").append(code);
                firstParam = false;
            }
            if (date != null) {
                url.append(firstParam ? "?" : "&").append("date=").append(date);
            }

            ResponseEntity<List<KbvInsuranceDto>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<KbvInsuranceDto>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error fetching insurances from KBV service", e);
        }
        return Optional.empty();
    }

    public Optional<List<KbvImportHistoryDto>> getImportHistory(String quarter, String status) {
        try {
            StringBuilder url = new StringBuilder(baseUrl + "/api/kbv/import/history");
            boolean firstParam = true;
            if (quarter != null) {
                url.append(firstParam ? "?" : "&").append("quarter=").append(quarter);
                firstParam = false;
            }
            if (status != null) {
                url.append(firstParam ? "?" : "&").append("status=").append(status);
            }

            ResponseEntity<List<KbvImportHistoryDto>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<KbvImportHistoryDto>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error fetching import history from KBV service", e);
        }
        return Optional.empty();
    }

    public boolean triggerImport(String filePath, String quarter, String version) {
        try {
            ImportTriggerRequest request = new ImportTriggerRequest();
            request.setFilePath(filePath);
            request.setQuarter(quarter);
            request.setVersion(version);

            ResponseEntity<ImportTriggerResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/kbv/import/trigger",
                    request,
                    ImportTriggerResponse.class);

            return response.getStatusCode() == HttpStatus.OK 
                    && response.getBody() != null 
                    && response.getBody().isSuccess();
        } catch (RestClientException e) {
            log.error("Error triggering import in KBV service", e);
            return false;
        }
    }

    public Optional<KbvChangeComparison> getChanges(String fromQuarter, String toQuarter) {
        try {
            String url = baseUrl + "/api/kbv/changes?fromQuarter=" + fromQuarter + "&toQuarter=" + toQuarter;
            ResponseEntity<KbvChangeComparison> response = restTemplate.getForEntity(
                    url,
                    KbvChangeComparison.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error fetching changes from KBV service", e);
        }
        return Optional.empty();
    }

    public static class ImportTriggerRequest {
        private String filePath;
        private String quarter;
        private String version;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ImportTriggerResponse {
        private boolean success;
        private int recordsImported;
        private String importType;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRecordsImported() {
            return recordsImported;
        }

        public void setRecordsImported(int recordsImported) {
            this.recordsImported = recordsImported;
        }

        public String getImportType() {
            return importType;
        }

        public void setImportType(String importType) {
            this.importType = importType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
