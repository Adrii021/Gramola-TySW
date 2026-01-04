package edu.uclm.es.gramola;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.uclm.es.gramola.dao.SelectedTrackDao;
import edu.uclm.es.gramola.dao.UserDao;
import edu.uclm.es.gramola.model.Token;
import edu.uclm.es.gramola.model.User;
import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class GramolaFunctionalTests {

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private SelectedTrackDao trackDao;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        
        options.addArguments("--disable-features=PasswordLeakDetection");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--guest");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20)); 
        driver.manage().window().maximize();
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ===================================================================
    // ESCENARIO 1: EL FLUJO FREEMIUM COMPLETO (PAGO OK)
    // ===================================================================
    @Test
    public void testScenario1_Freemium_PayAndAdd() {
        // 1. PREPARACIÓN: Usuario SIN pagar con claves de Spotify
        String email = "vip" + System.currentTimeMillis() + "@test.com";
        String password = "password123";
        
        User user = new User();
        user.setEmail(email);
        user.setPwd(password);
        user.setBar("Bar VIP");
        // CLAVES REALES (del chat anterior)
        user.setClientId("35dce5653a984b54b813115ba82f578b"); 
        user.setClientSecret("887354fe270744a3ba4a02eb77a24d42");
        
        Token token = new Token();
        user.setCreationToken(token);
        userDao.save(user);

        // 2. LOGIN
        driver.get("http://localhost:4200/login");
        
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
        emailInput.sendKeys(email);
        driver.findElement(By.name("pwd")).sendKeys(password);
        driver.findElement(By.cssSelector(".btn-primary")).click();

        // 3. BUSCAR Y "PRIMER INTENTO"
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Queen");
        driver.findElement(By.xpath("//button[contains(text(),'Buscar')]")).click();

        WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".add-btn")));
        addBtn.click();

        // 4. GESTIÓN ALERTA "PAGA PRIMERO"
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
        } catch (Exception e) {}

        // 5. PAGO EN STRIPE (CORREGIDO) 🟢
        wait.until(ExpectedConditions.urlContains("/payment"));
        
        // Esperamos a que el iframe de Stripe (js.stripe.com) sea VISIBLE.
        // Esto evita pillar el iframe oculto de control.
        WebElement stripeIframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe[src*='js.stripe.com']")));
        driver.switchTo().frame(stripeIframe);
        
        // Ahora sí encontraremos el input
        WebElement cardInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("cardnumber")));
        cardInput.sendKeys("4242424242424242"); 
        cardInput.sendKeys("12"); 
        cardInput.sendKeys("34"); 
        cardInput.sendKeys("123"); 
        
        driver.switchTo().defaultContent();
        driver.findElement(By.cssSelector(".btn-primary")).click();

        // 6. REDIRECCIÓN TRAS PAGO
        wait.until(ExpectedConditions.urlContains("/login"));
        System.out.println("✅ Pago completado. Volviendo a entrar...");

        // 7. SEGUNDO LOGIN (Ahora es Premium)
        // Volvemos a buscar los elementos para evitar referencias viejas
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
        driver.findElement(By.name("pwd")).sendKeys(password);
        driver.findElement(By.cssSelector(".btn-primary")).click();

        // 8. AÑADIR CANCIÓN (SEGUNDO INTENTO - ÉXITO)
        WebElement searchInput2 = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder*='Busca']")));
        searchInput2.clear();
        searchInput2.sendKeys("Queen");
        driver.findElement(By.xpath("//button[contains(text(),'Buscar')]")).click();
        
        WebElement addBtn2 = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".add-btn")));
        addBtn2.click();

        // Manejar alerta de éxito
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
        } catch (Exception e) {}

        // 9. VERIFICACIÓN FINAL
        User updatedUser = userDao.findById(email).get();
        assertTrue(updatedUser.getCreationToken().isUsed(), "El token debería constar como usado");

        try { Thread.sleep(2000); } catch (Exception e) {}
        var canciones = trackDao.findByUserId(email);
        assertTrue(canciones.size() > 0, "La canción debería haberse guardado en BD");

        System.out.println("✅ ESCENARIO 1 (ÉXITO) COMPLETADO");
    }

    // ===================================================================
    // ESCENARIO 2: EL MOROSO (PAGO FALLIDO)
    // ===================================================================
    @Test
    public void testScenario2_Freemium_PaymentError() {
        String email = "moroso" + System.currentTimeMillis() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPwd("1234");
        user.setBar("Bar Moroso");
        // CLAVES REALES TAMBIÉN AQUÍ PARA PODER BUSCAR
        user.setClientId("35dce5653a984b54b813115ba82f578b"); 
        user.setClientSecret("887354fe270744a3ba4a02eb77a24d42");
        user.setCreationToken(new Token());
        userDao.save(user);

        // LOGIN Y BÚSQUEDA
        driver.get("http://localhost:4200/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
        driver.findElement(By.name("pwd")).sendKeys("1234");
        driver.findElement(By.cssSelector(".btn-primary")).click();

        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Queen");
        driver.findElement(By.xpath("//button[contains(text(),'Buscar')]")).click();

        // INTENTO DE AÑADIR -> BLOQUEO
        WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".add-btn")));
        addBtn.click();
        
        try {
            wait.until(ExpectedConditions.alertIsPresent()).accept(); 
        } catch (Exception e) {}

        // INTENTO DE PAGO CON TARJETA MALA (CORREGIDO) 🟢
        wait.until(ExpectedConditions.urlContains("/payment"));
        
        // Iframe específico y visible
        WebElement stripeIframe = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe[src*='js.stripe.com']")));
        driver.switchTo().frame(stripeIframe);
        
        WebElement cardInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("cardnumber")));
        cardInput.sendKeys("4000000000000099"); 
        cardInput.sendKeys("12");
        cardInput.sendKeys("34");
        cardInput.sendKeys("123");
        
        driver.switchTo().defaultContent();
        driver.findElement(By.cssSelector(".btn-primary")).click();

        // VERIFICAR QUE FALLA
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            System.out.println("✅ Alerta de error recibida: " + alert.getText());
            alert.accept();
        } catch (Exception e) {
            System.out.println("⚠️ No salió alerta, verificamos URL...");
        }

        boolean enLogin = driver.getCurrentUrl().contains("/login");
        assertFalse(enLogin, "No debería redirigir al login si el pago falla");
        
        User dbUser = userDao.findById(email).get();
        assertFalse(dbUser.getCreationToken().isUsed(), "El token NO debería haberse marcado como usado");

        System.out.println("✅ ESCENARIO 2 (ERROR) COMPLETADO");
    }
}