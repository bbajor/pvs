package de.bbajor.pvs.e2e;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Basis-Klasse für E2E-Tests mit Selenium.
 * 
 * <p>Diese Klasse stellt die Grundlage für UI-Tests dar und startet automatisch
 * einen Browser (Chrome oder Firefox). Die Anwendung wird über Spring Boot Test
 * gestartet.</p>
 * 
 * <p><strong>Lizenz:</strong> Selenium ist unter Apache 2.0 lizenziert und
 * frei für kommerzielle Nutzung.</p>
 * 
 * <p><strong>Verwendung:</strong>
 * <pre>{@code
 * class MyE2ETest extends BaseE2ETest {
 *     @Test
 *     void testLogin() {
 *         driver.get(getBaseUrl() + "/login");
 *         // ... Test-Logik
 *     }
 * }
 * }</pre>
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int serverPort;

    protected WebDriver driver;
    protected WebDriverWait wait;

    /**
     * Browser-Typ für Tests. Kann über System-Property {@code e2e.browser}
     * gesetzt werden (chrome, firefox). Default: chrome
     */
    protected enum BrowserType {
        CHROME, FIREFOX
    }

    @BeforeEach
    void setUpDriver() {
        BrowserType browserType = getBrowserType();
        
        switch (browserType) {
            case CHROME:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                // Headless für CI/CD
                if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                chromeOptions.addArguments("--disable-gpu");
                chromeOptions.addArguments("--window-size=1920,1080");
                driver = new ChromeDriver(chromeOptions);
                break;
            case FIREFOX:
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
                    firefoxOptions.addArguments("--headless");
                }
                driver = new FirefoxDriver(firefoxOptions);
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browserType);
        }

        // Timeout für explizite Waits
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Gibt die Base-URL der Test-Anwendung zurück.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + serverPort;
    }

    /**
     * Wartet auf Vaadin-Client-Server-Kommunikation.
     * 
     * <p>Vaadin nutzt AJAX für Client-Server-Kommunikation. Diese Methode
     * wartet, bis alle laufenden Requests abgeschlossen sind.</p>
     */
    protected void waitForVaadin() {
        wait.until(webDriver -> {
            // Prüfe, ob vaadin-Bridge bereit ist und keine laufenden Requests hat
            Object result = ((org.openqa.selenium.JavascriptExecutor) webDriver)
                    .executeScript(
                            "return window.Vaadin && window.Vaadin.Flow && " +
                            "window.Vaadin.Flow.clients && " +
                            "Object.keys(window.Vaadin.Flow.clients).length > 0 && " +
                            "!window.Vaadin.Flow.clients[Object.keys(window.Vaadin.Flow.clients)[0]].isActive();");
            return result != null && (Boolean) result;
        });
    }

    /**
     * Wartet, bis ein Element sichtbar ist (für Vaadin-Komponenten).
     */
    protected void waitForElementVisible(org.openqa.selenium.By locator) {
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wartet, bis ein Element klickbar ist.
     */
    protected void waitForElementClickable(org.openqa.selenium.By locator) {
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Ermittelt den Browser-Typ aus System-Property oder Umgebungsvariable.
     */
    private BrowserType getBrowserType() {
        String browser = System.getProperty("e2e.browser", System.getenv("E2E_BROWSER"));
        if (browser == null || browser.isEmpty()) {
            return BrowserType.CHROME; // Default
        }
        try {
            return BrowserType.valueOf(browser.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BrowserType.CHROME; // Fallback
        }
    }

    /**
     * Navigiert zur angegebenen URL und wartet auf Vaadin.
     */
    protected void navigateTo(String path) {
        driver.get(getBaseUrl() + path);
        waitForVaadin();
    }
}

