package de.bbajor.pvs.kbv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KbvZipDownloaderTest {

  private final KbvZipDownloader downloader = new KbvZipDownloader();

  @TempDir
  Path tempDir;

  @Test
  void downloadAllExtractsXmlFilesAndNestedArchives() throws IOException {
    Path sourceZip = createZipWithNestedArchive(tempDir.resolve("source.zip"));
    Path target = tempDir.resolve("target");

    List<Path> extracted = downloader.downloadAll(List.of(sourceZip.toUri().toString()), target);

    Path primaryXml = target.resolve("icd/Test.xml");
    Path nestedXml = target.resolve("nested/Nested.xml");

    assertThat(extracted).contains(primaryXml, nestedXml);
    assertThat(Files.readString(primaryXml)).contains("ICD-TEST");
    assertThat(Files.readString(nestedXml)).contains("Nested-Content");
  }

  @Test
  void downloadAllPreventsZipSlip() throws IOException {
    Path maliciousZip = createZip(tempDir.resolve("malicious.zip"), "../evil.xml", "<evil/>");
    Path target = tempDir.resolve("danger");

    assertThatThrownBy(() -> downloader.downloadAll(List.of(maliciousZip.toUri().toString()), target))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("outside target directory");

    assertThat(Files.exists(target.resolve("evil.xml"))).isFalse();
  }

  private Path createZipWithNestedArchive(Path zipPath) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      addEntry(zos, "icd/Test.xml", "<ICD-TEST/>");
      byte[] nestedZipBytes = buildNestedZip();
      zos.putNextEntry(new ZipEntry("nested.zip"));
      zos.write(nestedZipBytes);
      zos.closeEntry();
    }
    return zipPath;
  }

  private byte[] buildNestedZip() throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream nested = new ZipOutputStream(baos)) {
      addEntry(nested, "nested/Nested.xml", "<Nested-Content/>");
      nested.finish();
      return baos.toByteArray();
    }
  }

  private Path createZip(Path zipPath, String entryName, String content) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      addEntry(zos, entryName, content);
    }
    return zipPath;
  }

  private void addEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
    zos.putNextEntry(new ZipEntry(entryName));
    zos.write(content.getBytes(StandardCharsets.UTF_8));
    zos.closeEntry();
  }
}
