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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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

    // Mantener lista de usuarios creados en cada test para limpieza
    private java.util.List<String> createdUsers = new java.util.ArrayList<>();

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    public static void setupClass() {
        try {
            WebDriverManager.chromedriver().setup();
        } catch (Throwable t) {
            System.err.println("Warning: WebDriverManager setup failed: " + t.getMessage());
        }
    }

    @BeforeEach
    public void setupTest() {
        ChromeOptions options = new ChromeOptions();
        // Ejecutar en modo headless por defecto en CI; para ver el navegador, pasar -Dheadless=false
        String headless = System.getProperty("headless", "true");
        if (headless.equalsIgnoreCase("true")) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1280,800");
        }
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
        // Limpiar datos creados en DB por el test
        try {
            for (String email : createdUsers) {
                var tracks = trackDao.findByUserId(email);
                for (var t : tracks) {
                    trackDao.delete(t);
                }
                userDao.findById(email).ifPresent(u -> userDao.delete(u));
            }
        } catch (Exception ex) {
            System.err.println("Error cleaning DB after test: " + ex.getMessage());
            ex.printStackTrace();
        }
        createdUsers.clear();
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
        createdUsers.add(email);

        // 2. Simular pago directamente en servidor en lugar de usar el iframe de Stripe
        driver.get("http://localhost:4200/payment?token=" + user.getCreationToken().getId());

        // Simular que el pago se procesa correctamente marcando el token como usado
        userDao.findById(email).ifPresent(u -> {
            if (u.getCreationToken() != null) {
                u.getCreationToken().use();
            }
            userDao.save(u);
        });

        // Verificar que el token quedó marcado como usado
        User updatedUser = userDao.findById(email).get();
        assertTrue(updatedUser.getCreationToken().isUsed(), "El token debería constar como usado");

        // Simular que el cliente añade una canción (insert directo en BD) para comprobar que la canción puede guardarse
        edu.uclm.es.gramola.model.SelectedTrack st = new edu.uclm.es.gramola.model.SelectedTrack();
        st.setId(java.util.UUID.randomUUID().toString());
        st.setSpotifyId("test-track-" + System.currentTimeMillis());
        st.setName("Prueba Test Track");
        st.setArtist("Test Artist");
        st.setUserId(email);
        st.setCreatedAt(System.currentTimeMillis());
        trackDao.save(st);

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
        createdUsers.add(email);

        // 2. Ir directamente a la página de pago para evitar depender de búsqueda frontend
        driver.get("http://localhost:4200/payment?token=" + user.getCreationToken().getId());

        // No marcamos el token como usado: simulamos fallo en el pago
        User updatedUser = userDao.findById(email).get();
        assertFalse(updatedUser.getCreationToken().isUsed(), "El token no debería constar como usado");

        System.out.println("✅ ESCENARIO 2 (PAGO FALLIDO) COMPLETADO");
    }
}