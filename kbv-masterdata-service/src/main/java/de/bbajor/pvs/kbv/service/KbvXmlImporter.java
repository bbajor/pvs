package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class KbvXmlImporter {

  private static final Logger log = LoggerFactory.getLogger(KbvXmlImporter.class);
  private final JdbcTemplate jdbcTemplate;

  public KbvXmlImporter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void importFromDirectory(Path root) throws IOException {
    if (!Files.exists(root)) {
      log.warn("KBV import directory {} does not exist", root);
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      paths.filter(p -> p.toString().endsWith(".xml")).forEach(this::importXmlFile);
    }
  }

  private void importXmlFile(Path file) {
    // Placeholder: extract minimal info for demo purposes
    String relative = file.toString();
    log.info("KBV import: registering {}", relative);
    jdbcTemplate.update(
        "insert into kbv_icd_entry(code, text_content, valid_from) values (?,?, current_date)",
        relative, "Imported placeholder");
  }
}
