package de.bbajor.pvs.kbv.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kbv.download")
public class KbvImportProperties {

  private boolean enabled = false;
  private List<String> zipUrls = new ArrayList<>();
  private String targetDir = "/data/kbv";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getZipUrls() {
    return zipUrls;
  }

  public void setZipUrls(List<String> zipUrls) {
    this.zipUrls = zipUrls;
  }

  public String getTargetDir() {
    return targetDir;
  }

  public void setTargetDir(String targetDir) {
    this.targetDir = targetDir;
  }
}
