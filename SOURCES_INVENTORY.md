# Inventario por Fuente: Sitios Deportivos Iniciales - Nexo TV

Fecha de evaluación de base: 18 de octubre de 2026.

| Fuente | URL Inicial Preconfigurada | Acceso Inicial Observado | Adaptador de Agenda | Estado de Agenda | Reproducción Probada | Dispositivo Evaluado | Clasificación / Observaciones |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TV Libre** | `https://tvlibre.st/` | Accesible HTTP 200 en portadas | `TvLibreAdapter` (Jsoup) | Implementada y verificada con fixtures | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Implementada y verificada en parser; reproducción pendiente de dispositivo** |
| **Fútbol Libre Full HD** | `https://futbollibrefullhd.org/` | Accesible HTTP 200 | `FutbolLibreAdapter` (Jsoup) | Implementada y verificada con fixtures | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Implementada y verificada en parser; reproducción pendiente de dispositivo** |
| **Pelota Libre** | `https://pelotalibre.la/default.php` | Accesible HTTP 200 | `FutbolLibreAdapter` (Espejo compatible) | Implementada | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Implementada; reproducción pendiente de dispositivo** |
| **Roja Directa** | `https://rojadirectatvmas.net/` | Accesible HTTP 200 | Pendiente (Acceso por Favorito directo) | Pendiente de integración | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Pendiente de integración de parser; favorito funcional** |
| **Rústico TV** | `https://rusticotvplus.org/` | Accesible HTTP 200 | Pendiente (Acceso por Favorito directo) | Pendiente de integración | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Pendiente de integración de parser; favorito funcional** |
| **Fútbol para Todos** | `https://futbolparatodos.ad/home.php` | Inaccesible en revisión previa | Deshabilitado por defecto | Error de acceso reportado con transparencia | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Pendiente de conectividad; favorito editable si el operador cambia** |
| **Al Ángulo TV** | `https://alangulotv.sx/` | Inaccesible en revisión previa | Deshabilitado por defecto | Error de acceso reportado con transparencia | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Pendiente de conectividad; favorito editable si el operador cambia** |
| **Tu Deporte** | `https://tudeporte.lat/` | Accesible HTTP 200 | Pendiente (Acceso por Favorito directo) | Pendiente de integración | Pendiente de hardware | Google TV Streamer / FlowBox (Pendiente físico) | **Pendiente de integración de parser; favorito funcional** |

---

### Notas de Transparencia Técnica:
1. No se crearon resultados de muestra o partidos falsos para fuentes con errores o pendientes.
2. Cada enlace de la agenda se abre dentro del motor WebView protegido de Nexo TV sin invocar navegadores externos ni reproductores que rompan la contención.
3. El usuario puede agregar manualmente cualquier evento de estas u otras fuentes desde el botón *"Agregar Manual"* en la sección Agenda.
