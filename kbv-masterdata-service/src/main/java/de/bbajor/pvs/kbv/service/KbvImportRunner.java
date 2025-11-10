package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
      var target = Path.of(properties.getTargetDir());
      List<String> zipUrls = properties.getZipUrls();
      if (zipUrls == null || zipUrls.isEmpty()) {
        log.info("KBV download skipped: no URLs configured, importing existing files from {}", target);
      } else {
        log.info("Starting KBV download of {} urls to {}", zipUrls.size(), target);
        var extractedFiles = downloader.downloadAll(zipUrls, target);
        log.info("KBV download finished, {} files extracted to {}", extractedFiles.size(), target);
      }
      importer.importFromDirectory(target);
    } catch (IOException e) {
      log.error("KBV download failed", e);
    }
  }
}
