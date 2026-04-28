# UI-Testing mit Selenium

Diese Dokumentation beschreibt, wie UI-Tests für die Vaadin-Anwendung mit **Selenium WebDriver** durchgeführt werden können.

## Warum Selenium statt Vaadin TestBench?

- ✅ **Frei für kommerzielle Nutzung** (Apache 2.0 Lizenz)
- ✅ **Keine Kosten** - im Gegensatz zu Vaadin TestBench (kostenpflichtig)
- ✅ **Große Community** und umfangreiche Dokumentation
- ✅ **JUnit 5 Integration** via Selenium-Jupiter
- ✅ **Multi-Browser Support** (Chrome, Firefox, Edge, Safari)

## Voraussetzungen

- Java 21
- Gradle (bereits vorhanden)
- Chrome oder Firefox Browser (für lokale Tests)

## Setup

Die notwendigen Dependencies sind bereits in `build.gradle` konfiguriert:

```gradle
testImplementation 'io.github.bonigarcia:selenium-jupiter:5.0.1'
testImplementation 'org.seleniumhq.selenium:selenium-java:4.26.0'
testImplementation 'org.seleniumhq.selenium:selenium-support:4.26.0'
```

## Verwendung

### Basis-Test-Klasse

Alle E2E-Tests sollten von `BaseE2ETest` erben:

```java
class MyE2ETest extends BaseE2ETest {
    @Test
    void testSomething() {
        navigateTo("/my-page");
        // Test-Logik
    }
}
```

### Beispiel-Test

Siehe `src/test/java/de/bbajor/pvs/e2e/LoginE2ETest.java` für ein Beispiel.

## Browser-Konfiguration

### Lokale Tests (mit sichtbarem Browser)

```bash
# Chrome mit sichtbarem Browser
./gradlew test -De2e.headless=false

# Firefox
./gradlew test -De2e.browser=firefox -De2e.headless=false
```

### CI/CD (Headless)

```bash
# Headless Chrome (Standard)
./gradlew test

# Headless Firefox
./gradlew test -De2e.browser=firefox
```

## Vaadin-spezifische Besonderheiten

### Shadow DOM

Vaadin-Komponenten nutzen Shadow DOM, daher funktionieren normale CSS-Selektoren oft nicht direkt. 

**Lösung 1: JavaScript-Zugriff**
```java
WebElement button = (WebElement) ((JavascriptExecutor) driver)
    .executeScript("return document.querySelector('vaadin-button').shadowRoot.querySelector('button');");
```

**Lösung 2: data-testid Attribute**
Füge in deinen Vaadin-Komponenten `data-testid` Attribute hinzu:
```java
Button button = new Button("Klick mich");
button.getElement().setAttribute("data-testid", "my-button");
```

Dann im Test:
```java
WebElement button = driver.findElement(By.cssSelector("[data-testid='my-button']"));
```

### Warten auf Vaadin-Requests

Die `BaseE2ETest` Klasse bietet bereits eine `waitForVaadin()` Methode, die auf abgeschlossene Client-Server-Kommunikation wartet:

```java
navigateTo("/my-page");
waitForVaadin(); // Wartet, bis alle Vaadin-Requests abgeschlossen sind
```

## Best Practices

### 1. Page Object Pattern

Erstelle Page-Objekte für wiederkehrende UI-Elemente:

```java
public class LoginPage {
    private final WebDriver driver;
    
    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }
    
    public void enterUsername(String username) {
        WebElement field = driver.findElement(By.id("username"));
        field.sendKeys(username);
    }
    
    public void clickLogin() {
        driver.findElement(By.id("login-button")).click();
        waitForVaadin();
    }
}
```

### 2. Explizite Waits statt Thread.sleep()

```java
// ❌ Schlecht
Thread.sleep(2000);

// ✅ Gut
wait.until(ExpectedConditions.elementToBeClickable(By.id("button")));
```

### 3. Testdaten-Isolation

Nutze `@Transactional` oder Testcontainers für isolierte Testdaten:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class MyE2ETest extends BaseE2ETest {
    // Tests laufen in Transaktionen und werden automatisch zurückgerollt
}
```

### 4. Screenshots bei Fehlern

Die `BaseE2ETest` Klasse kann erweitert werden, um automatisch Screenshots bei Fehlern zu erstellen:

```java
@AfterEach
void takeScreenshotOnFailure(TestInfo testInfo) {
    if (testInfo.getTags().contains("failed")) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        // Speichere Screenshot
    }
}
```

## Alternative: Playwright

Falls Selenium zu langsam oder instabil ist, kann **Playwright** als Alternative genutzt werden:

### Vorteile von Playwright

- Schneller als Selenium
- Bessere Auto-Wait-Funktionalität
- Integrierte Screenshot/Video-Aufnahme
- Bessere Shadow DOM Unterstützung

### Setup Playwright

```gradle
testImplementation 'com.microsoft.playwright:playwright:1.40.0'
```

### Beispiel mit Playwright

```java
import com.microsoft.playwright.*;

class PlaywrightE2ETest {
    @Test
    void testLogin() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
            page.navigate("http://localhost:8080/login");
            // Test-Logik
        }
    }
}
```

## CI/CD Integration

### GitHub Actions Beispiel

```yaml
- name: Run E2E Tests
  run: |
    ./gradlew test --tests "*E2ETest" -De2e.headless=true
  env:
    E2E_BROWSER: chrome
```

### Docker für CI/CD

Für CI/CD-Umgebungen kann ein Docker-Container mit Browser genutzt werden:

```dockerfile
FROM maven:3.9-eclipse-temurin-21
RUN apt-get update && apt-get install -y \
    chromium \
    chromium-driver
```

## Troubleshooting

### "ChromeDriver not found"

WebDriverManager sollte automatisch den Driver herunterladen. Falls nicht:

```bash
# Manuell installieren
./gradlew test --refresh-dependencies
```

### "Element not found" bei Vaadin-Komponenten

- Nutze `waitForVaadin()` nach Navigation
- Prüfe Shadow DOM - nutze JavaScript-Zugriff oder `data-testid` Attribute
- Erhöhe Timeout-Werte in `BaseE2ETest`

### Tests sind zu langsam

- Nutze Headless-Modus
- Reduziere Anzahl der E2E-Tests (nur kritische Pfade)
- Nutze Unit-Tests für UI-Logik, E2E nur für Integration

## Weitere Ressourcen

- [Selenium Documentation](https://www.selenium.dev/documentation/)
- [Selenium-Jupiter Documentation](https://bonigarcia.dev/selenium-jupiter/)
- [Vaadin Testing Best Practices](https://vaadin.com/docs/flow/testing)

## Lizenz

Selenium WebDriver ist unter **Apache License 2.0** lizenziert und frei für kommerzielle Nutzung.

