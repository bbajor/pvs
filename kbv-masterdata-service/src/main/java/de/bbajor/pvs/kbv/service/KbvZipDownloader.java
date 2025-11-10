package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KbvZipDownloader {

  private static final Logger log = LoggerFactory.getLogger(KbvZipDownloader.class);
  private static final String USER_AGENT = "PVS-KBV-Importer/1.0";
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration READ_TIMEOUT = Duration.ofMinutes(5);

  public List<Path> downloadAll(List<String> urls, Path targetDir) throws IOException {
    if (urls == null || urls.isEmpty()) {
      return List.of();
    }
    var normalizedTarget = targetDir.toAbsolutePath().normalize();
    Files.createDirectories(normalizedTarget);

    List<Path> extractedFiles = new ArrayList<>();
    for (String url : urls) {
      Path downloadedFile = download(url, normalizedTarget);
      if (isZipFile(downloadedFile)) {
        extractedFiles.addAll(extractZip(downloadedFile, normalizedTarget));
      } else {
        extractedFiles.add(downloadedFile);
      }
    }
    return extractedFiles;
  }

  public Path download(String urlString, Path targetDir) throws IOException {
    URL url = new URL(urlString);
    URLConnection connection = url.openConnection();
    HttpURLConnection httpConnection = null;
    if (connection instanceof HttpURLConnection http) {
      httpConnection = http;
      http.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
      http.setReadTimeout((int) READ_TIMEOUT.toMillis());
      http.setRequestProperty("User-Agent", USER_AGENT);
      http.connect();
      int responseCode = http.getResponseCode();
      if (responseCode >= 400) {
        throw new IOException("Failed to download %s: HTTP %d".formatted(urlString, responseCode));
      }
    } else {
      connection.connect();
    }

    String fileName = determineFileName(url);
    Path targetFile = targetDir.resolve(fileName).normalize();
    if (!targetFile.startsWith(targetDir)) {
      throw new IOException("Refusing to write file outside of target directory: " + fileName);
    }

    Files.createDirectories(targetFile.getParent());
    try (InputStream in = connection.getInputStream();
        OutputStream out = Files.newOutputStream(targetFile)) {
      in.transferTo(out);
    } finally {
      if (httpConnection != null) {
        httpConnection.disconnect();
      }
    }
    return targetFile;
  }

  private List<Path> extractZip(Path zipFile, Path targetDir) throws IOException {
    var normalizedTarget = targetDir.toAbsolutePath().normalize();
    Files.createDirectories(normalizedTarget);

    List<Path> extractedFiles = new ArrayList<>();
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        handleZipEntry(zipInputStream, entry, normalizedTarget, extractedFiles);
        zipInputStream.closeEntry();
      }
    }
    return extractedFiles;
  }

  private void handleZipEntry(ZipInputStream zipInputStream, ZipEntry entry, Path targetDir, List<Path> extractedFiles)
      throws IOException {
    Path resolvedPath = resolveZipEntry(targetDir, entry);
    if (entry.isDirectory()) {
      Files.createDirectories(resolvedPath);
      return;
    }

    Files.createDirectories(Objects.requireNonNull(resolvedPath.getParent()));
    try (OutputStream out = Files.newOutputStream(resolvedPath)) {
      zipInputStream.transferTo(out);
    }

    if (isZipFile(resolvedPath)) {
      log.debug("Extracting nested zip {}", resolvedPath);
      extractedFiles.addAll(extractZip(resolvedPath, resolvedPath.getParent()));
    } else {
      extractedFiles.add(resolvedPath);
    }
  }

  private Path resolveZipEntry(Path targetDir, ZipEntry entry) throws IOException {
    Path resolved = targetDir.resolve(entry.getName()).normalize();
    if (!resolved.startsWith(targetDir)) {
      throw new IOException("Zip entry outside target directory detected: " + entry.getName());
    }
    return resolved;
  }

  private boolean isZipFile(Path path) {
    String fileName = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("");
    return fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
  }

  private String determineFileName(URL url) {
    String path = url.getPath();
    if (path == null || path.isBlank()) {
      return "kbv-download-" + System.currentTimeMillis();
    }
    Path fileName = Path.of(path).getFileName();
    if (fileName == null || fileName.toString().isBlank()) {
      return "kbv-download-" + System.currentTimeMillis();
    }
    return fileName.toString();
  }
}
