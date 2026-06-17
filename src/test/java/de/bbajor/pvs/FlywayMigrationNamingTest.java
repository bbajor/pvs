package de.bbajor.pvs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationNamingTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void versionedMigrationsUseUniqueVersions() throws IOException {
        Map<String, List<String>> migrationsByVersion;
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            migrationsByVersion = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(VERSIONED_MIGRATION_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .collect(Collectors.groupingBy(matcher -> matcher.group(1),
                            Collectors.mapping(Matcher::group, Collectors.toList())));
        }

        List<String> duplicateVersions = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .sorted(Map.Entry.comparingByKey(FlywayMigrationNamingTest::compareVersions))
                .map(entry -> "V" + entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .toList();

        assertTrue(duplicateVersions.isEmpty(),
                () -> "Flyway migration versions must be unique: " + String.join("; ", duplicateVersions));
    }

    private static int compareVersions(String left, String right) {
        List<Integer> leftParts = versionParts(left);
        List<Integer> rightParts = versionParts(right);
        int maxLength = Math.max(leftParts.size(), rightParts.size());
        for (int index = 0; index < maxLength; index++) {
            int leftPart = index < leftParts.size() ? leftParts.get(index) : 0;
            int rightPart = index < rightParts.size() ? rightParts.get(index) : 0;
            int comparison = Integer.compare(leftPart, rightPart);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static List<Integer> versionParts(String version) {
        return Pattern.compile("\\.").splitAsStream(version)
                .map(Integer::parseInt)
                .toList();
    }
}
