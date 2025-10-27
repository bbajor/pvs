package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.kbv.config.KbvImportProperties;

@Component
public class KbvImportRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(KbvImportRunner.class);
  private final KbvImportProperties properties;
  private final KbvZipDownloader downloader;
  private final KbvXmlImporter importer;

  public KbvImportRunner(KbvImportProperties properties, KbvZipDownloader downloader, KbvXmlImporter importer) {
    this.properties = properties;
    this.downloader = downloader;
    this.importer = importer;
  }

  @Override
  public void run(String... args) throws Exception {
    if (!properties.isEnabled()) {
      log.info("KBV download disabled");
      return;
    }
    try {
      log.info("Starting KBV download of {} urls to {}", properties.getZipUrls().size(), properties.getTargetDir());
      var target = Path.of(properties.getTargetDir());
      downloader.downloadAll(properties.getZipUrls(), target);
      importer.importFromDirectory(target);
    } catch (IOException e) {
      log.error("KBV download failed", e);
    }
  }
}
