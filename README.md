# Claude Proto — versión offline con backend propio

App Android que corre un LLM **dentro del teléfono** (MediaPipe LLM Inference) y expone su propio backend HTTP local
(`http://127.0.0.1:8765`, NanoHTTPD) con `GET /api/status` y `POST /api/chat`. La interfaz de chat la sirve ese mismo backend.
Sin internet ni claves de API. (Claude no se puede ejecutar offline: el modelo local es un modelo abierto, p. ej. Gemma.)

## Compilar en GitHub
Sube el proyecto a un repo → **Actions → Build APK** → descarga el artefacto `claude-proto-apk`.

## Usar
1. Descarga un modelo `.task` (formato MediaPipe/LiteRT, p. ej. Gemma 3 1B IT int4, desde la comunidad `litert-community` en Hugging Face; algunos requieren aceptar su licencia) y guárdalo en el teléfono.
2. Abre la app → **Cargar modelo** → elige el archivo. Se copia al almacenamiento interno y queda cargado para siempre.
3. Chatea sin conexión. Requiere Android 7+ arm64 y ~3 GB de RAM libres.
