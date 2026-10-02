# Secure Mod

*Diseñado por TakumiStudios.*

**Seguridad para servidores Fabric.** Puertas reforzadas, cofres y barriles con contraseña, teclados numéricos, tarjetas de acceso, escáneres de retina, candados para *cualquier* contenedor (vanilla o de otros mods) y herramientas de administración de primera. Toda la autorización ocurre en el servidor.

| Minecraft | Jar |
|---|---|
| 26.3.x | `securemod-<versión>+mc26.3.jar` |
| 26.2.x | `securemod-<versión>+mc26.2.jar` |
| 26.1.x | `securemod-<versión>+mc26.1.2.jar` |

Requiere Fabric Loader ≥ 0.19.5, **Fabric API** y Java 25. Hay que instalarlo **en cliente y servidor**.

## Qué incluye
- **Bloques:** puerta de hierro y de roble reforzadas, trampilla de hierro reforzada, puerta de valla de roble reforzada, cofre con contraseña (simple y doble), barril con contraseña, teclado numérico, lector de tarjetas y escáner de retina.
- **Ítems:** candado (bloquea cualquier contenedor o puerta existente sin reemplazar el bloque), codificador universal, tarjetas de nivel 1–5, grabador de tarjetas, extractor universal de bloques y herramienta de administrador.
- **Seguridad:** autorización 100 % en servidor, contraseñas con hash y salt, anti fuerza bruta, rate limiting de paquetes, registro de auditoría y protección contra romper, explosiones, pistones, tolvas, gólems de cobre, mobs, fuego, comparadores, Transfer API, pick block en creativo y espectadores.
- **Administración:** comandos `/securemod`, permisos compatibles con LuckPerms, historial por bloque, purga de inactivos, límites por jugador, exportación a JSON y "raid windows" para servidores de facciones.

La documentación completa está en [`docs/`](docs/) (en inglés) y el estado de cada punto del plan en [docs/implementation-status.md](docs/implementation-status.md).

## Compilar
```
./gradlew buildAndCollect        # jars de todas las versiones en build/libs/<versión>/
./gradlew :26.3:runGameTest      # batería de pruebas de bypass en un mundo real
```

## Licencia
[MIT](LICENSE) © TakumiStudios
