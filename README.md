# Nexo TV

**Nexo TV** es una aplicación Android nativa diseñada principalmente para **Android TV / Google TV** (con interfaz secundaria táctil para móviles), orientada al acceso seguro a páginas web de transmisiones y eventos deportivos en vivo bajo una política de **protección estricta** contra publicidad abusiva, popups y redirecciones no autorizadas.

---

## 1. Características Principales

1. **Doble Interfaz Nativa (TV y Móvil)**:
   - **Android TV (Principal)**: Controlada exclusivamente con las flechas del control remoto (D-Pad), botón central **OK** y tecla **Atrás**.
   - **Móvil (Secundaria)**: Interfaz táctil para gestionar favoritos, crear eventos manuales, generar y escanear códigos QR de transferencia.
2. **Navegador con Protección Estricta**:
   - **Tres niveles de contención**:
     - *Filtrado de recursos*: Bloqueo preventivo de redes de anuncios, rastreadores y mineros mediante coincidencia estricta de subdominios (`HostNormalizer`).
     - *Contención de navegación*: `shouldOverrideUrlLoading` detiene escapes a dominios extranjeros no autorizados.
     - *Rechazo de ventanas emergentes*: `onCreateWindow` rechaza `window.open` y `target="_blank"`.
   - **Tres modos de control remoto en el navegador**:
     - *Foco DOM*: Navegación natural entre elementos interactivos de la página.
     - *Puntero Virtual (Cursor)*: Flechas mueven un cursor en pantalla y OK despacha eventos táctiles directos en las coordenadas exactas.
     - *Desplazamiento (Scroll)*: Flechas desplazan suavemente el documento web.
   - **Compatibilidad con Video a Pantalla Completa**:
     - Manejo de `WebChromeClient.onShowCustomView` y `onHideCustomView`.
     - Bloqueo de apagado de pantalla (`FLAG_KEEP_SCREEN_ON`) activo únicamente durante la reproducción a pantalla completa.
3. **8 Favoritos Preconfigurados**:
   - TV Libre (`https://tvlibre.st/`)
   - Fútbol para Todos (`https://futbolparatodos.ad/home.php`)
   - Roja Directa (`https://rojadirectatvmas.net/`)
   - Rústico TV (`https://rusticotvplus.org/`)
   - Pelota Libre (`https://pelotalibre.la/default.php`)
   - Fútbol Libre Full HD (`https://futbollibrefullhd.org/`)
   - Al Ángulo TV (`https://alangulotv.sx/`)
   - Tu Deporte (`https://tudeporte.lat/`)
4. **Agenda Deportiva Nativa**:
   - Adaptadores HTML modulares con Jsoup (`TvLibreAdapter`, `FutbolLibreAdapter`).
   - Gestión y alta de eventos manuales con enlaces directos.
   - Deduplicación automática de partidos entre diferentes fuentes.
5. **Transferencia de Datos**:
   - Exportación e importación en formato JSON versionado (`schemaVersion: 1`).
   - Transferencia compacta con código de texto `NEXO:...` para ingreso mediante el teclado virtual del control remoto de Android TV.
   - Soporte para códigos QR (generación y lectura local desde imagen sin servicios externos).
6. **Privacidad y Seguridad**:
   - Sin servidores externos requeridos, sin cuentas de usuario, sin anuncios propios y sin telemetría.
   - Limpieza de caché, cookies de sesión y almacenamiento volátil de WebView al salir o desde Ajustes.
   - Conexiones HTTPS forzadas y errores TLS cancelados inmediatamente.

---

## 2. Mapa de Teclas del Control Remoto (Android TV)

La aplicación ha sido diseñada para no depender de teclas de colores, micrófonos ni menús especiales:

| Tecla / Acción | Función en Portada / Agenda | Función en Navegador Web |
| :--- | :--- | :--- |
| **Flechas (D-Pad)** | Mueve el foco visible con borde resaltado | Modo Foco (elementos) / Modo Cursor (mueve puntero) / Modo Scroll (desplaza página) |
| **Botón Central (OK)** | Abre tarjetas, pestañas o activa acciones | Pulsa botones nativos de la barra o despacha clic en posición del puntero |
| **Atrás (Back)** | Vuelve a la pestaña de Sitios | **Jerarquía estricta**:<br>1. Cierra diálogo de bloqueo si está abierto<br>2. Sale de video a pantalla completa<br>3. Muestra la barra de herramientas si estaba oculta<br>4. Vuelve al portal nativo |

---

## 3. Dispositivos Objetivo

- **Google TV Streamer**: Totalmente compatible con D-pad nativo, renderizado acelerado por hardware y gestión de pantalla completa.
- **FlowBox TV**: Compatible en modo Android TV. En caso de restricciones del proveedor para proveedores de documentos o SAF, se utiliza la **importación mediante código compacto por teclado**.

---

## 4. Compilación e Instalación

### Requisitos:
- Android Studio Ladybug / Meerkat (o Gradle 8+ con JDK 17/21).
- Android SDK con Build-Tools 36 y Android API 36 (`minSdk = 24`).

### Comandos de Compilación:
```bash
# Compilar APK de depuración
gradle :app:assembleDebug

# Ejecutar suite de pruebas unitarias y Robolectric
gradle :app:testDebugUnitTest
```

El APK resultante se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

### Instalación vía depuración inalámbrica (ADB):
```bash
adb connect <IP_DEL_TELEVISOR>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
