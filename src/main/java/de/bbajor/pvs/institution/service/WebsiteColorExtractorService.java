package de.bbajor.pvs.institution.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Service to extract color information from websites for layout customization.
 * Analyzes HTML and CSS to identify primary brand colors.
 */
@Service
@RequiredArgsConstructor
public class WebsiteColorExtractorService {

    private static final Logger LOG = LogManager.getLogger(WebsiteColorExtractorService.class);
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Pattern to match hex colors (#rgb, #rrggbb, #rrggbbaa)
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#([0-9a-fA-F]{3,8})\\b");
    
    // Pattern to match rgb/rgba colors
    private static final Pattern RGB_COLOR_PATTERN = Pattern.compile(
            "rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)(?:\\s*,\\s*[\\d.]+)?\\s*\\)", 
            Pattern.CASE_INSENSITIVE);

    /**
     * Extracts layout colors from a website URL.
     * 
     * @param websiteUrl The URL of the website to analyze
     * @return LayoutColors object containing extracted colors, or null if extraction fails
     */
    public LayoutColors extractColors(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }

        try {
            // Normalize URL
            String normalizedUrl = normalizeUrl(websiteUrl);
            LOG.info("Extracting colors from website: {}", normalizedUrl);

            // Fetch HTML content
            String htmlContent = fetchHtml(normalizedUrl);
            if (htmlContent == null || htmlContent.isBlank()) {
                LOG.warn("Failed to fetch HTML content from: {}", normalizedUrl);
                return null;
            }

            // Extract all colors from HTML and CSS
            List<String> allColors = extractAllColors(htmlContent);
            
            if (allColors.isEmpty()) {
                LOG.warn("No colors found on website: {}", normalizedUrl);
                return null;
            }

            // Analyze colors and identify primary colors
            LayoutColors layoutColors = analyzeColors(allColors);
            
            LOG.info("Extracted colors from {}: primary={}, secondary={}, background={}, text={}, accent={}",
                    normalizedUrl,
                    layoutColors.getPrimaryColor(),
                    layoutColors.getSecondaryColor(),
                    layoutColors.getBackgroundColor(),
                    layoutColors.getTextColor(),
                    layoutColors.getAccentColor());

            return layoutColors;

        } catch (Exception e) {
            LOG.error("Error extracting colors from website: {}", websiteUrl, e);
            return null;
        }
    }

    private String normalizeUrl(String url) {
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    private String fetchHtml(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; PVS-ColorExtractor/1.0)")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                LOG.warn("HTTP request failed with status {} for URL: {}", response.statusCode(), url);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error fetching HTML from URL: {}", url, e);
            return null;
        }
    }

    private List<String> extractAllColors(String htmlContent) {
        List<String> colors = new ArrayList<>();

        // Extract hex colors
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(htmlContent);
        while (hexMatcher.find()) {
            String hex = "#" + hexMatcher.group(1);
            // Normalize 3-digit hex to 6-digit
            if (hex.length() == 4) {
                hex = "#" + hex.charAt(1) + hex.charAt(1) 
                    + hex.charAt(2) + hex.charAt(2) 
                    + hex.charAt(3) + hex.charAt(3);
            }
            // Only keep 6-digit hex colors (ignore alpha channel)
            if (hex.length() == 7) {
                colors.add(hex.toLowerCase());
            }
        }

        // Extract rgb/rgba colors and convert to hex
        Matcher rgbMatcher = RGB_COLOR_PATTERN.matcher(htmlContent);
        while (rgbMatcher.find()) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));
            String hex = rgbToHex(r, g, b);
            colors.add(hex);
        }

        return colors;
    }

    private String rgbToHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x", 
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b)));
    }

    private LayoutColors analyzeColors(List<String> colors) {
        if (colors.isEmpty()) {
            return new LayoutColors();
        }

        // Count color frequencies
        Map<String, Integer> colorFrequency = new HashMap<>();
        for (String color : colors) {
            colorFrequency.put(color, colorFrequency.getOrDefault(color, 0) + 1);
        }

        // Filter out common web colors (white, black, grays)
        List<Map.Entry<String, Integer>> filteredColors = colorFrequency.entrySet().stream()
                .filter(entry -> !isCommonColor(entry.getKey()))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        LayoutColors layoutColors = new LayoutColors();

        // Primary color: most frequent non-common color
        if (!filteredColors.isEmpty()) {
            layoutColors.setPrimaryColor(filteredColors.get(0).getKey());
            
            // Secondary color: second most frequent
            if (filteredColors.size() > 1) {
                layoutColors.setSecondaryColor(filteredColors.get(1).getKey());
            }
            
            // Accent color: third most frequent or a complementary color
            if (filteredColors.size() > 2) {
                layoutColors.setAccentColor(filteredColors.get(2).getKey());
            } else if (!filteredColors.isEmpty()) {
                // Use a lighter/darker variant of primary as accent
                layoutColors.setAccentColor(adjustBrightness(filteredColors.get(0).getKey(), 1.2f));
            }
        }

        // Background: most common light color (or white)
        String background = findLightColor(colorFrequency);
        layoutColors.setBackgroundColor(background != null ? background : "#ffffff");

        // Text: most common dark color (or black)
        String text = findDarkColor(colorFrequency);
        layoutColors.setTextColor(text != null ? text : "#000000");

        return layoutColors;
    }

    private boolean isCommonColor(String color) {
        // Common web colors to filter out
        String[] commonColors = {
            "#ffffff", "#fff", "#000000", "#000",
            "#f5f5f5", "#f0f0f0", "#e0e0e0", "#cccccc",
            "#333333", "#666666", "#999999", "#aaaaaa"
        };
        
        String lowerColor = color.toLowerCase();
        for (String common : commonColors) {
            if (lowerColor.equals(common) || lowerColor.equals(common + "ff")) {
                return true;
            }
        }
        
        // Also filter very light or very dark colors
        if (isVeryLightOrDark(color)) {
            return true;
        }
        
        return false;
    }

    private boolean isVeryLightOrDark(String hex) {
        if (hex.length() != 7 || !hex.startsWith("#")) {
            return false;
        }
        
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            
            // Calculate brightness
            double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
            
            // Very light (brightness > 240) or very dark (brightness < 30)
            return brightness > 240 || brightness < 30;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String findLightColor(Map<String, Integer> colorFrequency) {
        return colorFrequency.entrySet().stream()
                .filter(entry -> !isCommonColor(entry.getKey()) && isLightColor(entry.getKey()))
                .max((a, b) -> a.getValue().compareTo(b.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String findDarkColor(Map<String, Integer> colorFrequency) {
        return colorFrequency.entrySet().stream()
                .filter(entry -> !isCommonColor(entry.getKey()) && isDarkColor(entry.getKey()))
                .max((a, b) -> a.getValue().compareTo(b.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean isLightColor(String hex) {
        if (hex.length() != 7 || !hex.startsWith("#")) {
            return false;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
            return brightness > 200;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDarkColor(String hex) {
        if (hex.length() != 7 || !hex.startsWith("#")) {
            return false;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
            return brightness < 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String adjustBrightness(String hex, float factor) {
        if (hex.length() != 7 || !hex.startsWith("#")) {
            return hex;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            
            r = Math.min(255, Math.max(0, Math.round(r * factor)));
            g = Math.min(255, Math.max(0, Math.round(g * factor)));
            b = Math.min(255, Math.max(0, Math.round(b * factor)));
            
            return rgbToHex(r, g, b);
        } catch (NumberFormatException e) {
            return hex;
        }
    }

    /**
     * Data class for extracted layout colors.
     */
    @Getter
    @Setter
    public static class LayoutColors {
        private String primaryColor;
        private String secondaryColor;
        private String backgroundColor;
        private String textColor;
        private String accentColor;
    }
}

