package de.bbajor.pvs.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlywayMigrationVersionTest {

    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void versionedMigrationsUseUniqueVersions() throws IOException {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");

        try (Stream<Path> migrationFiles = Files.list(migrationDirectory)) {
            Map<String, List<String>> migrationsByVersion = migrationFiles
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .map(FlywayMigrationVersionTest::toVersionedMigration)
                    .flatMap(Stream::ofNullable)
                    .collect(Collectors.groupingBy(
                            VersionedMigration::version,
                            Collectors.mapping(VersionedMigration::fileName, Collectors.toList())));

            Map<String, List<String>> duplicates = migrationsByVersion.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            assertThat(duplicates)
                    .as("Flyway aborts application startup when multiple SQL migrations share the same version")
                    .isEmpty();
        }
    }

    private static VersionedMigration toVersionedMigration(String fileName) {
        Matcher matcher = VERSIONED_MIGRATION_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }

        return new VersionedMigration(matcher.group(1), fileName);
    }

    private record VersionedMigration(String version, String fileName) {
    }
}
