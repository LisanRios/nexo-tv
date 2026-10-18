# Arquitectura del Sistema: Nexo TV

## 1. Estructura de Paquetes

```
com.example/
├── MainActivity.kt               # Entrada unificada; selección de interfaz según dispositivo (TV / Móvil)
├── NexoTvApplication.kt          # Inicialización de BD, dependencias y limpieza en arranque frío
├── domain/model/                 # Modelos limpios: Favorite, Category, SitePolicy, AgendaSource, SportEvent, EventLink
├── data/
│   ├── local/                    # Room Database (NexoTvDatabase), Entities, DAOs y TypeConverters
│   └── repository/               # FavoritesRepository y AgendaRepository
├── protection/                   # Motor de seguridad estricta:
│   ├── HostNormalizer.kt         # Normalización RFC, Punycode y verificación de límites de subdominio
│   ├── RequestFilter.kt          # Interfaz de filtrado y DomainListRequestFilter con reemplazo atómico
│   ├── DefaultBlockedDomains.kt  # Base local de redes publicitarias, popups y rastreadores
│   └── StrictProtectionEngine.kt # Evaluación de navegación, recursos, límites de origen y logs saneados
├── browser/                      # Componentes del navegador:
│   ├── NexoWebViewClient.kt      # Contención de navegación, intercepción de recursos y seguridad TLS
│   └── NexoWebChromeClient.kt    # Manejo de video CustomView a pantalla completa y rechazo de popups
├── agenda/                       # Fuentes de agenda y adaptadores:
│   └── AgendaSourceAdapter.kt    # Interfaz y adaptadores para TV Libre y Fútbol Libre con Jsoup
├── transfer/                     # Exportación e importación de datos:
│   └── TransferManager.kt        # JSON versionado, serializador compacto gzip-base64 y soporte QR local
└── ui/
    ├── common/                   # Modificador tvFocusable, monogramas y etiquetas deportivas
    ├── theme/                    # Paleta oscura de alto contraste (NexoTvTheme, Color, Type)
    ├── browser/                  # BrowserScreen con cursor virtual D-pad y herramientas nativas
    ├── forms/                    # Diálogos de edición de favoritos y eventos manuales
    ├── tv/                       # Vistas Android TV: TvMainScreen, TvSitesScreen, TvAgendaScreen, TvOtherScreens
    ├── mobile/                   # Interfaz táctil secundaria: MobileMainScreen
    └── NexoMainViewModel.kt      # ViewModel central con StateFlow y navegación explícita
```

## 2. Modelo de Datos y Persistencia

La persistencia estructurada utiliza **Room**:
- **FavoriteEntity**: `id`, `title`, `url`, `categoryId`, `sortOrder`, `pinned`, `createdAt`, `updatedAt`.
- **CategoryEntity**: `id`, `name`, `sortOrder`.
- **SitePolicyEntity**: `originHost`, `authorizedNavigationHostsJson`, `strictProtectionEnabled`.
- **AgendaSourceEntity**: `id`, `title`, `baseUrl`, `adapterId`, `enabled`, `lastAttemptAt`, `lastSuccessAt`, `status`, `technicalMessage`.
- **SportEventEntity**: `id`, `title`, `sport`, `competition`, `participants`, `startsAt`, `originalTimezone`, `originalDateText`, `sourceId`, `sourceEventId`, `fetchedAt`, `expiresAt`, `eventType`.
- **EventLinkEntity**: `id`, `eventId`, `sourceId`, `pageUrl`, `label`.

Los datos volátiles de navegación (caché web, sesiones de cookies, WebStorage) no se persisten en base de datos y se depuran al terminar la sesión.

## 3. Motor de Protección Estricta

1. **Rechazo de Ventanas (`WebChromeClient.onCreateWindow`)**:
   Devuelve `false` incondicionalmente, impidiendo que scripts ejecuten aperturas emergentes (`window.open`) o hipervínculos con `target="_blank"`.
2. **Contención de Navegación (`shouldOverrideUrlLoading`)**:
   - Comprueba si el esquema es `http` o `https`. Cualquier intento de abrir `intent:`, `market:`, `file:` o `javascript:` es cancelado de inmediato.
   - Comprueba si el destino pertenece al origen autorizado del favorito o evento.
   - Los escapes hacia redes publicitarias o dominios de aterrizaje son detenidos mostrando un aviso visible sin destruir el contenido web legítimo.
3. **Filtrado de Subrecursos (`shouldInterceptRequest`)**:
   - Bloquea consultas a redes de publicidad conocidas devolviendo un `WebResourceResponse` vacío.
   - La comprobación de subdominios exige un límite estricto por punto (`x.ads.example.test` coincide con `ads.example.test`, pero `notads.example.test` no coincide).
4. **Seguridad TLS Innegociable**:
   - En caso de `onReceivedSslError`, el handler ejecuta `handler.cancel()`.
   - `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW`.
