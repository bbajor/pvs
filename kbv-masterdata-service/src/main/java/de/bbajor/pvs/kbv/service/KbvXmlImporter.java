package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.lang.Nullable;

@Service
public class KbvXmlImporter {

  private static final Logger log = LoggerFactory.getLogger(KbvXmlImporter.class);
  private static final int BATCH_SIZE = 500;
  private static final XMLInputFactory XML_FACTORY = XMLInputFactory.newFactory();

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public KbvXmlImporter(JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, null);
  }

  @Autowired
  public KbvXmlImporter(JdbcTemplate jdbcTemplate, @Nullable Clock clock) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.clock = clock != null ? clock : Clock.systemUTC();
  }

  public void importFromDirectory(Path root) throws IOException {
    if (root == null) {
      log.warn("KBV import requested with null root path");
      return;
    }
    if (!Files.exists(root)) {
      log.warn("KBV import directory {} does not exist", root);
      return;
    }

    log.info("KBV import: scanning {}", root.toAbsolutePath());

    Deque<Path> directories = new ArrayDeque<>();
    directories.push(root);

    int processedFiles = 0;
    while (!directories.isEmpty()) {
      Path directory = directories.pop();
      if (!Files.isDirectory(directory)) {
        continue;
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
        for (Path entry : stream) {
          if (Files.isDirectory(entry)) {
            directories.push(entry);
            continue;
          }
          String filename = entry.getFileName().toString().toLowerCase(Locale.ROOT);
          if (filename.endsWith(".zip")) {
            try {
              Path extracted = unzipArchive(entry);
              directories.push(extracted);
            } catch (IOException unzipException) {
              log.error("KBV import: failed to unzip {}", entry, unzipException);
            }
          } else if (filename.endsWith(".xml")) {
            processedFiles++;
            importXmlFile(entry);
          }
        }
      }
    }
    if (processedFiles == 0) {
      log.warn("KBV import: no XML files found under {}", root);
    }
  }

  private Path unzipArchive(Path archive) throws IOException {
    Path parent = archive.getParent();
    String fileName = archive.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');
    String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    Path targetDir = (parent != null ? parent : archive.toAbsolutePath().getParent())
        .resolve(baseName);

    cleanDirectory(targetDir);
    Files.createDirectories(targetDir);

    log.info("KBV import: extracting {} to {}", archive, targetDir);
    try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        Path resolved = targetDir.resolve(entry.getName()).normalize();
        if (!resolved.startsWith(targetDir)) {
          throw new IOException(
              "Zip entry attempted to escape target directory: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else {
          Files.createDirectories(resolved.getParent());
          try (OutputStream outputStream = Files.newOutputStream(resolved)) {
            zipStream.transferTo(outputStream);
          }
        }
        zipStream.closeEntry();
      }
    }
    return targetDir;
  }

  private void cleanDirectory(Path directory) throws IOException {
    if (directory == null || !Files.exists(directory)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(directory)) {
      walk.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException deleteException) {
          throw new RuntimeException(deleteException);
        }
      });
    } catch (RuntimeException wrapped) {
      if (wrapped.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      throw wrapped;
    }
  }

  private void importXmlFile(Path file) {
    int inserted = 0;
    List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

    try (InputStream inputStream = Files.newInputStream(file)) {
      XMLStreamReader reader = XML_FACTORY.createXMLStreamReader(inputStream);

      String currentCode = null;
      String currentLabel = null;
      boolean inPreferredRubric = false;
      StringBuilder labelBuilder = null;
      LocalDate importDate = LocalDate.now(clock);

      while (reader.hasNext()) {
        int event = reader.next();
        switch (event) {
          case XMLStreamConstants.START_ELEMENT -> {
            String localName = reader.getLocalName();
            if ("Class".equals(localName)) {
              currentCode = reader.getAttributeValue(null, "code");
              currentLabel = null;
            } else if ("Rubric".equals(localName)) {
              String kind = reader.getAttributeValue(null, "kind");
              inPreferredRubric = "preferred".equalsIgnoreCase(kind);
            } else if (inPreferredRubric && "Label".equals(localName)) {
              labelBuilder = new StringBuilder();
            }
          }
          case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
            if (labelBuilder != null) {
              labelBuilder.append(reader.getText());
            }
          }
          case XMLStreamConstants.END_ELEMENT -> {
            String localName = reader.getLocalName();
            if ("Label".equals(localName) && labelBuilder != null) {
              currentLabel = labelBuilder.toString().trim();
              labelBuilder = null;
            } else if ("Rubric".equals(localName)) {
              inPreferredRubric = false;
              labelBuilder = null;
            } else if ("Class".equals(localName)) {
              if (StringUtils.hasText(currentCode) && StringUtils.hasText(currentLabel)) {
                batch.add(new Object[] {currentCode, currentLabel, importDate});
                if (batch.size() >= BATCH_SIZE) {
                  inserted += flushBatch(batch);
                }
              }
              currentCode = null;
              currentLabel = null;
            }
          }
          default -> {
          }
        }
      }
      reader.close();
      inserted += flushBatch(batch);
      log.info("KBV import: {} entries imported from {}", inserted, file.getFileName());
    } catch (IOException | XMLStreamException e) {
      log.error("KBV import: failed to parse {}", file, e);
    }
  }

  private int flushBatch(List<Object[]> batch) {
    if (batch.isEmpty()) {
      return 0;
    }
    int[] updates = jdbcTemplate.batchUpdate("""
        insert into kbv_icd_entry(code, text_content, valid_from)
        values (?,?, ?)
        on conflict (code, valid_from) do update set text_content = excluded.text_content
        """,
        batch);
    batch.clear();
    return Arrays.stream(updates).sum();
  }
}

