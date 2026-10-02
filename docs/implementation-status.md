# Estado de implementación frente a `PLAN_MOD_SECUREMOD.md`

Leyenda: ✅ hecho y probado · 🟡 hecho con una decisión distinta (explicada) · ⏳ pendiente (fuera del código o de la v1)

## Secciones 1–11 (v1)
| Sección | Estado | Notas |
|---|---|---|
| 1. Visión v1 | ✅ | Puertas, trampillas, puertas de valla, cofres, barriles, shulkers y cualquier contenedor (candado) protegidos frente a abrir, romper, tolvas y explosiones. |
| 2. Stack | ✅ / 🟡 | MC 26.1.2, 26.2 y 26.3 · Loader 0.19.5 · Fabric API por versión · nombres Mojang · Java 25 · Loom 1.18 + Stonecutter 0.9 · config JSON propia. 🟡 Permisos: se usa la **Fabric Permission API** que ya trae Fabric API 26.x (LuckPerms la implementa) en lugar de `fabric-permissions-api` de lucko: mismo resultado, una dependencia menos. 🟡 Claims: OPAC y Flan; FTB Chunks no tiene versión Fabric para 26.x. |
| 3.1 Bloques | ✅ / 🟡 | Todos. 🟡 El escáner de retina se activa con clic derecho ("mirar y pulsar") para no tener ticks. |
| 3.2 Ítems | ✅ | Codificador universal, keycards 1–5 + Card Writer, Padlock (dato adjunto, no reemplaza el bloque), Admin Tool, Universal Block Remover. |
| 3.3 Propiedad | 🟡 | Propietario, nombre, hash+salt, permitidos, **bloqueados**, modo, nivel de tarjeta, fecha y `dataVersion`. 🟡 Se guardan en el chunk (Data Attachment API, como pide la sección 20.1) también para los bloques propios, así hay una sola fuente de datos y los bloques propios usan los BlockEntity vanilla. Equipos: scoreboard en modo *compartido*; parties de OPAC vía integración de claims. Bloques dobles: ✅. |
| 4. Seguridad multijugador | ✅ | Todo en servidor (distancia, dimensión, bloque, permiso) · SHA-256(salt+código), nunca sincronizado · anti fuerza bruta · romper, explosiones (`ServerExplosion`), pistones, tolvas/minecart-tolva/droppers, wither, ender dragon, zombis, fuego, comparadores, `UseBlockCallback` (agachado, con ítem, espectador), redstone externa, soporte de puertas · nodos admin · rate limiting · log de auditoría. 🟡 Endermen: no necesitan mixin (los bloques bloqueables no son `enderman_holdable`). Lava/fluidos: no destruyen puertas ni contenedores en vanilla. |
| 5. Red | ✅ | `SubmitCodeC2S`, `SetCodeC2S`, `UpdateAllowListC2S`, `CodeResultS2C`, `OwnerInfoS2C` + `SetModeC2S`, `OpenKeypadS2C`, `OpenConfigS2C`. Longitudes validadas. |
| 6. GUIs | ✅ / ⏳ | Teclado, panel de configuración, HUD "Propiedad de X", plugin de Jade. ⏳ WTHIT: no tiene build para 26.3. |
| 7. Comandos | ✅ | Todos los del plan + `trusted`, `inspect`, `debug`, `purgeinactive`, `unlockall`, `export`, `rollback`. |
| 8. Configuración | ✅ | Todos los campos + perfiles, raid windows, `compat.disable`, fake players, `claimsGrantAccess`. |
| 9. Persistencia | ✅ | Adjuntos del chunk + `SavedData` global por jugador + `dataVersion`. Probado cerrando y reabriendo el mundo. |
| 10. Estructura | 🟡 | Paquete `com.takumistudios.securemod`; `access/`, `lock/`, `compat/` y `api/` añadidos. No hacen falta BlockEntities propios. |
| 11. Recetas y balance | ✅ | Recetas caras (obsidiana + hierro). Los bloques propios solo caen con el Remover (son irrompibles en survival). |

## Sección 12 – Fases
| Fase | Estado |
|---|---|
| 0 Proyecto base multiversión + runServer/runClient + CI | ✅ |
| 1 Propiedad + puerta reforzada + romper/explosiones/pistones | ✅ |
| 2 Cofre/barril con contraseña + red + teclado + hash + anti fuerza bruta | ✅ |
| 3 Tolvas, mobs, comparadores, Padlock | ✅ |
| 4 Universal Modifier, whitelist, equipos, comandos, permisos | ✅ |
| 5 Keycards + lector, escáner | ✅ |
| 6 Config, auditoría, SavedData, límites y purga | ✅ |
| 7 Texturas, idiomas, recetas, Jade/claims | ✅ (texturas originales generadas por `tools/TextureGen.java`) |
| 8 Pruebas en servidor dedicado por versión + publicación | ✅ automáticas en las 3 versiones · ⏳ prueba manual con 2 clientes y publicación (necesita nombre definitivo y cuentas de Modrinth/CurseForge) |

## Sección 13 – Pruebas
✅ JUnit (20 tests) · ✅ 20 gametests por versión (incluido el modpack de compatibilidad) · ✅ client gametest con capturas y reinicio del mundo · ⏳ checklist manual con dos clientes (`docs/testing.md`) · ⏳ medición con Spark de 10 000 bloques (hay un gametest de 1 000 bloques / 100 000 consultas).

## Secciones 16–20
| Punto | Estado |
|---|---|
| 16 Multiversión (Stonecutter, rangos en `fabric.mod.json`, nombre de jar, CI) | ✅ · ⏳ 1.21.x (opcional, fase posterior) |
| 17.2-1 Seguridad sin trucos, `SECURITY.md`, bypass tests en CI | ✅ |
| 17.2-2 Modo solo servidor (Polymer) | ⏳ v1.x (Polymer existe para 26.x) |
| 17.2-3 Admin: LuckPerms, auditoría, inspect, rollback, purga, límites por rango, export JSON | ✅ |
| 17.2-4 Rendimiento: sin ticks, caché de permisos | ✅ · ⏳ medir con Spark |
| 17.2-5 GUIs, sonidos, partículas, barra de acción, tooltip, traducciones (en, es, pt, fr, de) | ✅ · ⏳ guía dentro del juego (v1.x) |
| 17.2-6 Claims, Jade, LuckPerms, Transfer API | ✅ · REI/EMI muestran las recetas sin plugin |
| 17.2-7 API pública | ✅ `SecureModApi`, `SecureModEvents` |
| 17.2-8 Perfiles y raid windows | ✅ |
| 18 v1.x / v2 / v3 (Polymer, cámaras, láseres, bloques reforzados, torretas, panel web…) | ⏳ hoja de ruta, no forman parte de la v1 |
| 19 Repo: MIT, plantillas, CONTRIBUTING, SECURITY, CHANGELOG, semver, publicación automática, wiki (`docs/`) | ✅ · ⏳ crear el repo en GitHub, secretos de publicación, página del mod con GIFs, Discord, beta abierta |
| 20.1 No reemplazar ni modificar | ✅ adjuntos, clases vanilla, tags `c:`, tags `lockable`/`never_lock` |
| 20.2 Reglas de mixins | ✅ MixinExtras, sin `@Overwrite`/`@Redirect`, `require = 0`, verificación al cargar, mixins condicionales, prefijo `securemod$`, `MIXINS.md` |
| 20.3 Robustez | ✅ try/catch y fallo seguro, datos corruptos/futuros cerrados, nunca cargar chunks, sin ticks, FakePlayer |
| 20.4 Matriz de compatibilidad | ✅ automática con Lithium, FerriteCore, Jade, OPAC, Flan · ⏳ manual con Create, Carpet, Sodium/Iris y un modpack grande |
| 20.5 Dependencias y metadatos | ✅ solo Fabric API obligatoria |
| 20.6 Desinstalar sin perder el mundo | ✅ `export`, `unlockall` |
| 20.7 Proceso de incompatibilidades | ✅ plantilla `compat`, `compat.disable` |
