package de.bbajor.pvs.kbv.service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class KbvZipDownloader {

  public void downloadAll(List<String> urls, Path targetDir) throws IOException {
    if (urls == null || urls.isEmpty()) {
      return;
    }
    Files.createDirectories(targetDir);
    for (String url : urls) {
      download(url, targetDir);
    }
  }

  public Path download(String urlString, Path targetDir) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
    connection.setReadTimeout((int) Duration.ofMinutes(5).toMillis());
    connection.setRequestProperty("User-Agent", "PVS-KBV-Importer/1.0");
    connection.connect();
    if (connection.getResponseCode() != 200) {
      throw new IOException("Failed to download " + urlString + ": HTTP " + connection.getResponseCode());
    }
    String fileName = Path.of(url.getPath()).getFileName().toString();
    Path targetFile = targetDir.resolve(fileName);
    try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
        FileOutputStream fileOutputStream = new FileOutputStream(targetFile.toFile())) {
      byte[] dataBuffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
        fileOutputStream.write(dataBuffer, 0, bytesRead);
      }
    }
    return targetFile;
  }
}
