package de.bbajor.pvs.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class YamlProfileConfigurationTest {

    private static final Pattern APPLICATION_YAML = Pattern.compile("application(?:-[\\w-]+)?\\.ya?ml");
    private static final Pattern ROOT_KEY = Pattern.compile("^[A-Za-z0-9_.-]+:\\s*(?:#.*)?$");

    @Test
    void applicationYamlFilesShouldNotDeclareDuplicateRootKeys() throws IOException {
        List<String> duplicateKeys = new ArrayList<>();

        try (Stream<Path> files = Files.list(Path.of("src/main/resources"))) {
            for (Path file : files
                    .filter(path -> APPLICATION_YAML.matcher(path.getFileName().toString()).matches())
                    .toList()) {
                duplicateKeys.addAll(duplicateRootKeysIn(file));
            }
        }

        assertThat(duplicateKeys)
                .as("Duplicate top-level YAML keys can make Spring ignore earlier profile settings")
                .isEmpty();
    }

    private static List<String> duplicateRootKeysIn(Path file) throws IOException {
        Map<String, Integer> firstLineByKey = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);

        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1);
            if (line.isBlank() || line.startsWith(" ") || line.startsWith("\t") || line.startsWith("#")) {
                continue;
            }
            if (!ROOT_KEY.matcher(line).matches()) {
                continue;
            }

            String key = line.substring(0, line.indexOf(':'));
            Integer firstLine = firstLineByKey.putIfAbsent(key, lineNumber);
            if (firstLine != null) {
                duplicates.add("%s declares '%s' at lines %d and %d"
                        .formatted(file.getFileName(), key, firstLine, lineNumber));
            }
        }

        return duplicates;
    }
}
