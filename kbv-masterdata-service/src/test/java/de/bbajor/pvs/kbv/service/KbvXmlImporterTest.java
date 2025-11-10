package de.bbajor.pvs.kbv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class KbvXmlImporterTest {

  private static final String SAMPLE_XML = """
      <?xml version="1.0" encoding="UTF-8"?>
      <ClaML>
        <Class code="A00">
          <Rubric kind="preferred">
            <Label xml:lang="de">Cholera</Label>
          </Rubric>
        </Class>
        <Class code="A01">
          <Rubric kind="preferred">
            <Label xml:lang="de">Typhus</Label>
          </Rubric>
        </Class>
      </ClaML>
      """;

  private RecordingJdbcTemplate jdbcTemplate;
  private KbvXmlImporter importer;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    jdbcTemplate = new RecordingJdbcTemplate();
    importer = new KbvXmlImporter(jdbcTemplate);
  }

  @AfterEach
  void tearDown() {
    jdbcTemplate.clear();
  }

  @Test
  void importsEntriesFromXmlFile() throws IOException {
    Path xmlFile = tempDir.resolve("icd.xml");
    Files.writeString(xmlFile, SAMPLE_XML, StandardCharsets.UTF_8);

    importer.importFromDirectory(tempDir);

    assertThat(jdbcTemplate.dumpEntries())
        .hasSize(2)
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .containsExactlyInAnyOrder(
            tuple("A00", "Cholera"),
            tuple("A01", "Typhus"));
  }

  @Test
  void importsEntriesFromZipArchive() throws IOException {
    Path zipFile = tempDir.resolve("icd.zip");
    try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      outputStream.putNextEntry(new ZipEntry("icd/icd.xml"));
      outputStream.write(SAMPLE_XML.getBytes(StandardCharsets.UTF_8));
      outputStream.closeEntry();
    }

    importer.importFromDirectory(tempDir);

    assertThat(jdbcTemplate.dumpEntries())
        .hasSize(2)
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .containsExactlyInAnyOrder(
            tuple("A00", "Cholera"),
            tuple("A01", "Typhus"));
  }

  @Test
  void reimportUpdatesExistingEntriesWithoutDuplicates() throws IOException {
    Path xmlFile = tempDir.resolve("icd.xml");
    Files.writeString(xmlFile, SAMPLE_XML, StandardCharsets.UTF_8);
    importer.importFromDirectory(tempDir);

    String updatedXml = SAMPLE_XML.replace("Cholera", "Cholera (aktualisiert)");
    Files.writeString(xmlFile, updatedXml, StandardCharsets.UTF_8);
    importer.importFromDirectory(tempDir);

    assertThat(jdbcTemplate.dumpEntries())
        .hasSize(2)
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .containsExactlyInAnyOrder(
            tuple("A00", "Cholera (aktualisiert)"),
            tuple("A01", "Typhus"));
  }

  private static final class RecordingJdbcTemplate extends JdbcTemplate {
    private final java.util.LinkedHashMap<String, String> entries = new java.util.LinkedHashMap<>();

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
      int[] results = new int[batchArgs.size()];
      for (int i = 0; i < batchArgs.size(); i++) {
        Object[] args = batchArgs.get(i);
        entries.put((String) args[0], (String) args[1]);
        results[i] = 1;
      }
      return results;
    }

    void clear() {
      entries.clear();
    }

    List<Map.Entry<String, String>> dumpEntries() {
      return new java.util.ArrayList<>(entries.entrySet());
    }
  }
}

