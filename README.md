# 🎵 Gramola Virtual - Práctica TySW

Aplicación web Fullstack para la gestión de música en establecimientos (Bares/Pubs), permitiendo a los dueños gestionar la reproducción y a los clientes solicitar canciones desde sus dispositivos móviles.

## 🚀 Tecnologías Utilizadas

* **Frontend:** Angular (v17+), HTML5, CSS3 (Diseño Responsive).
* **Backend:** Java Spring Boot (Maven).
* **Base de Datos:** MySQL / H2 (según configuración).
* **Integraciones:**
    * **Spotify API:** Búsqueda y reproducción de música.
    * **Stripe:** Pasarela de pagos para canciones prioritarias.
    * **Nominatim (OpenStreetMap):** Geolocalización y coordenadas del bar.
    * **Selenium:** Pruebas funcionales automatizadas e2e.

---

## 🛠️ Instalación y Puesta en Marcha

Sigue estos pasos en orden para arrancar la aplicación correctamente.

### 1. Base de Datos
Asegúrate de tener una base de datos MySQL corriendo (o revisa el `application.properties` del backend para la configuración).
* La aplicación está configurada para crear/actualizar las tablas automáticamente (`hibernate.ddl-auto=update`).

### 2. Backend (Spring Boot)
1.  Abre una terminal y entra en la carpeta del servidor:
    ```bash
    cd gramolabe
    ```
2.  Ejecuta la aplicación usando el wrapper de Maven:
    ```bash
    ./mvnw spring-boot:run
    ```
    * *Nota: En Windows usa `mvnw.cmd spring-boot:run` si tienes problemas con el script bash.*
3.  El servidor arrancará en el puerto **8080**.

### 3. Frontend (Angular)
1.  Abre **otra terminal** y entra en la carpeta del cliente:
    ```bash
    cd gramolafe
    ```
2.  Instala las dependencias (si es la primera vez):
    ```bash
    npm install
    ```
3.  Arranca el servidor de desarrollo:
    ```bash
    npm start
    ```
    *(O usa `ng serve`)*.
4.  La aplicación estará disponible en **http://localhost:4200**.

---

## 🧪 Ejecución de Tests Funcionales

El proyecto incluye tests automáticos con **Selenium** que prueban el flujo completo (Registro, Login, Compra de canción, Errores de pago).

Para ejecutarlos:
1.  Asegúrate de que el **Frontend (Angular)** está corriendo en el puerto 4200.
2.  Detén el Backend si lo tenías corriendo manualmente (los tests levantan su propia instancia).
3.  Desde la carpeta `gramolabe`, ejecuta:
    ```bash
    ./mvnw test
    ```

---

## 📖 Manual de Uso Básico

### 1. Registro de Nuevo Bar (Owner)
* Ve a "Registrarse".
* Rellena los datos. **Importante:**
    * **Dirección Postal:** Introduce una dirección real (ej: *Calle Toledo 1, Ciudad Real*) para que el sistema obtenga las coordenadas GPS automáticamente mediante la API de Nominatim.
* Tras el registro, confirma tu cuenta (simulado) y loguéate.

### 2. Panel del Dueño (Home)
* **Buscador:** Escribe el nombre de una canción o artista y pulsa Enter.
* **Añadir:** Pulsa "Añadir" en los resultados para mandar la canción a la cola.
* **Reproductor:** Verás la barra de progreso avanzar. La música se sincroniza mediante eventos del servidor (SSE).
* **Configuración:** Pulsa en el enlace inferior para ver los **Datos de Mi Bar** (verás las coordenadas y dirección obtenidas) o cambiar tu contraseña.

### 3. Modo Cliente (Móvil)
* El cliente accede mediante la URL del bar (o escaneando un QR simulado).
* **Geolocalización:** Al entrar, el navegador pedirá permiso de ubicación.
    * ✅ Si estás a menos de **100 metros** de las coordenadas del bar, podrás usar la app.
    * 🚫 Si estás lejos, aparecerá una pantalla de bloqueo restringiendo el acceso.
* **Pagos:** El cliente puede elegir "Añadir" (gratis/cola normal) o "Prioritaria" (Pago con Stripe).
    * Tarjeta de prueba Stripe (Éxito): `4242 4242 4242 4242`
    * Tarjeta de prueba Stripe (Fallo): `4000 0000 0000 0002`

---

## 📱 Diseño Responsive

La aplicación es totalmente adaptable:
* **Escritorio:** Vista completa con grid de resultados y listas laterales.
* **Móvil:** Interfaz simplificada en una sola columna, botones táctiles grandes y menú optimizado.

---

## 👥 Autores
* Adrián Alameda Alcaide
* Asignatura: Tecnologías y Sistemas Web