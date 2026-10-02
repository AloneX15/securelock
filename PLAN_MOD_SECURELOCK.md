# SecureLock — Plan de desarrollo (mod Fabric)

> ⚠️ **Nombre provisional.** Ya existe un mod llamado *SecureLocks* en Modrinth. Antes de publicar hay que elegir un nombre único y comprobar que no esté ocupado en Modrinth ni en CurseForge.

## 1. Visión y alcance
- **Meta:** ser **el mod de seguridad de referencia en Fabric**, el equivalente a SecurityCraft para Fabric, pero pensado desde el principio para servidores multijugador.
- **v1 (lanzamiento):** proteger por completo **puertas, trampillas, puertas de valla, cofres, barriles, shulkers y el resto de contenedores**. Nadie puede abrirlos, romperlos, vaciarlos con tolvas ni explotarlos si no tiene permiso.
- **v2 y v3:** ampliar hasta igualar o superar a SecurityCraft en funciones (cámaras, láseres, alarmas, bloques reforzados, minas, torretas). Ver la sección 18.
- Los pilares de calidad que lo diferencian están en la sección 17.

## 2. Stack técnico
| Elemento | Elección |
|---|---|
| Minecraft | **26.1.x, 26.2.x y 26.3.x** (versiones actuales; 26.3 es la última estable). Ver sección 16 |
| Loader | Fabric Loader (última versión, válida para todas las versiones objetivo) |
| API | Fabric API (una build por cada versión de Minecraft) |
| Mappings | **Nombres oficiales de Mojang** (Yarn no se mantiene desde 26.1; no hace falta línea `mappings` en `build.gradle`) |
| Java | **25** (IntelliJ IDEA ≥ 2025.3) |
| Build | Gradle + Loom (id de plugin `net.fabricmc.fabric-loom`) + **Stonecutter** para compilar varias versiones desde el mismo código |
| Config | JSON propio en `config/securelock.json` (sin dependencias externas, para no bloquear ninguna versión) |
| Permisos | `fabric-permissions-api` (compatible con LuckPerms) como dependencia opcional; si no está, fallback a nivel de OP |
| Protección de terceros (opcional) | Integración con FTB Chunks / Open Parties and Claims |

**Lado:** el mod debe ser **requerido en cliente y servidor** (añade bloques, ítems y GUIs). Toda la lógica de autorización vive **solo en el servidor**.

## 3. Funcionalidades
### 3.1 Bloques
| Bloque | Descripción |
|---|---|
| Puerta reforzada (hierro/madera variantes) | Solo abre propietario/permitidos. Irrompible para otros. |
| Trampilla y puerta de valla reforzadas | Igual que la puerta. |
| Cofre con contraseña (simple y doble) | Pide código al abrir; recuerda sesión unos segundos. |
| Barril con contraseña | Igual. |
| Teclado numérico (Keypad) | Se coloca junto a puertas; al introducir el código emite redstone N ticks. |
| Lector de tarjetas | Abre con una Keycard nivel 1–5. |
| Escáner biométrico (retina) | Abre solo a jugadores en whitelist al mirarlo. |

### 3.2 Ítems
- **Codificador universal (Universal Block Modifier):** gestiona whitelist/blacklist, cambiar código, ver propietario.
- **Keycards nivel 1–5** + **Card Writer** para vincularlas a un lector.
- **Llave de candado (Padlock):** ítem para bloquear **cualquier** cofre, puerta o contenedor existente, ya sea vanilla o **de otro mod**. **No reemplaza el bloque:** guarda el candado como dato adjunto (Fabric Data Attachment API, ver sección 20), así el bloque sigue siendo el mismo para el juego y para los demás mods.
- **Herramienta de administrador (Admin Tool):** solo OPs; ver info y forzar desbloqueo.
- **Universal Block Remover:** el propietario recupera su bloque protegido.

### 3.3 Sistema de propiedad
- Cada bloque protegido guarda en su `BlockEntity`: `ownerUUID`, `ownerName` (cache), `passcodeHash`, `allowList<UUID>`, `mode` (privado/compartido/público), `cardLevel`, `createdAt`.
- Soporte de **equipos**: permitir a miembros del scoreboard team o de la party de FTB/OPAC.
- Bloques dobles (cofre doble, puertas de 2 bloques): la mitad secundaria delega en la principal.

## 4. Seguridad multijugador (lo crítico)
1. **Autorización 100 % en servidor.** El cliente solo envía intenciones (`OpenRequest`, `SubmitCode`); el servidor valida distancia (≤ 8 bloques), dimensión, que el bloque exista y el permiso.
2. **Contraseñas:** nunca en texto plano. Guardar `hash = SHA-256(salt + code)` con salt aleatoria por bloque. Nunca sincronizar el hash al cliente (excluir de `getUpdatePacket`/`getUpdateTag`).
3. **Anti-fuerza bruta:** tras 3 intentos fallidos, bloqueo de 30 s por jugador+bloque (configurable) y log.
4. **Protecciones contra bypass.** Se implementan con eventos de Fabric API siempre que sea posible, y si no, con mixins que cumplan las reglas de la sección 20 (MixinExtras, nunca `@Overwrite`, fallo seguro). Cada protección solo actúa sobre **posiciones protegidas**; el resto del mundo no cambia nada.
   - Romper: `PlayerBlockBreakEvents.BEFORE` → cancelar si no es propietario/admin. Dureza `-1` para otros (`getExplosionResistance` alto, `getDestroyProgress` = 0).
   - Explosiones: resistencia 3 600 000; mixin en `ServerExplosion` para excluir bloques protegidos.
   - Pistones: `PushReaction.BLOCK` en las propiedades del bloque.
   - Tolvas/minecart-tolva/droppers: mixin en `HopperBlockEntity` y `Container` → no extraer/insertar en contenedores bloqueados (configurable para permitir solo si la tolva es del mismo dueño).
   - Endermen, withers, ender dragon: no pueden coger/destruir.
   - Fuego/lava/fluidos: no afectan.
   - `UseBlockCallback`: cancelar apertura de contenedores bloqueados aunque el jugador esté agachado o use otro ítem.
   - Comparadores: opcionalmente devolver 0 para no filtrar contenido.
   - Redstone externa: las puertas reforzadas **ignoran** redstone salvo de keypads/lectores del mismo dueño.
   - Rotura de bloque de soporte bajo la puerta: la puerta no cae.
5. **Permisos de admin:** nodos `securelock.admin.bypass`, `securelock.admin.inspect`, `securelock.admin.remove`, `securelock.command.*` (fallback a nivel OP 2).
6. **Rate limiting de paquetes** custom (máx. N por segundo por jugador) para evitar spam/crash.
7. **Logs de auditoría:** `logs/securelock-audit.log` con colocación, rotura, apertura denegada, cambio de código, bypass de admin.

## 5. Red (networking)
Usar la API de payloads de Fabric (`PayloadTypeRegistry` + `CustomPacketPayload` con `StreamCodec`, nombres Mojang), registrados con `ServerPlayNetworking` / `ClientPlayNetworking`.
| Payload | Dirección | Contenido |
|---|---|---|
| `SubmitCodeC2S` | C→S | `BlockPos`, `String code` (máx. 16 chars) |
| `SetCodeC2S` | C→S | `BlockPos`, código nuevo |
| `UpdateAllowListC2S` | C→S | `BlockPos`, acción add/remove, nombre de jugador |
| `CodeResultS2C` | S→C | ok/fallo/bloqueado + segundos restantes |
| `OwnerInfoS2C` | S→C | nombre de propietario, modo (para tooltip/HUD) |

Validar siempre longitudes y contenido en el servidor; ignorar paquetes malformados.

## 6. GUIs (cliente)
- Pantalla de teclado numérico (`Screen` propia, botones 0–9, borrar, OK).
- Pantalla de configuración del bloque (propietario, modo, lista de permitidos, cambiar código).
- Overlay/HUD opcional: al mirar un bloque protegido, mostrar "Propiedad de X".
- Integración Jade/WTHIT para tooltip (opcional, solo si tienen versión 26.1).

## 7. Comandos
```
/securelock info                     -> info del bloque que miras
/securelock trust <jugador>          -> añade a todos tus bloques
/securelock untrust <jugador>
/securelock list                     -> tus bloques protegidos (con coords)
/securelock admin unlock             -> fuerza desbloqueo (admin)
/securelock admin transfer <jugador> -> cambia propietario
/securelock admin purge <jugador>    -> quita protección de un jugador baneado/inactivo
/securelock reload                   -> recarga config
```

## 8. Configuración (`config/securelock.json`)
- `maxFailedAttempts`, `lockoutSeconds`, `codeMinLength`, `codeMaxLength`
- `allowHoppersFromSameOwner`, `comparatorOutputHidden`
- `maxProtectedBlocksPerPlayer` (0 = ilimitado)
- `inactiveOwnerDays` (para purga automática opcional)
- `explosionProof`, `allowTeamAccess`, `auditLog`
- Lista de bloques vanilla que el Padlock puede bloquear.

## 9. Persistencia
- Datos por bloque en NBT del `BlockEntity` (sobrevive a reinicios, se mueve con el chunk).
- Índice global por jugador (para `/securelock list`, límites y purga) en un `SavedData` del mundo: `Map<UUID, Set<GlobalPos>>`.
- Migración de versiones: campo `dataVersion` en NBT.

## 10. Estructura del proyecto
```
securelock/
├─ build.gradle, gradle.properties, settings.gradle
└─ src/
   ├─ main/java/com/tuusuario/securelock/
   │  ├─ SecureLock.java                 (ModInitializer: registros, eventos, comandos)
   │  ├─ registry/ ModBlocks, ModItems, ModBlockEntities, ModItemGroups
   │  ├─ block/  ReinforcedDoorBlock, ReinforcedTrapdoorBlock, LockedChestBlock, KeypadBlock, CardReaderBlock...
   │  ├─ blockentity/ OwnableBlockEntity (base), LockedChestBlockEntity, KeypadBlockEntity...
   │  ├─ item/ UniversalModifierItem, KeycardItem, PadlockItem, AdminToolItem
   │  ├─ security/ Ownership, PasscodeHasher, BruteForceTracker, PermissionHelper, AuditLogger
   │  ├─ network/ payloads + ServerPacketHandlers
   │  ├─ event/ BlockBreakHandler, UseBlockHandler, ExplosionHandler
   │  ├─ mixin/ HopperBlockEntityMixin, ServerExplosionMixin, EndermanMixin, PistonMixin
   │  ├─ command/ SecureLockCommand
   │  ├─ config/ SecureLockConfig
   │  └─ data/ ProtectedBlocksState
   ├─ client/java/com/tuusuario/securelock/client/
   │  ├─ SecureLockClient.java (ClientModInitializer)
   │  ├─ screen/ KeypadScreen, BlockConfigScreen
   │  └─ network/ ClientPacketHandlers
   └─ main/resources/
      ├─ fabric.mod.json, securelock.mixins.json
      ├─ assets/securelock/ (blockstates, models, textures, lang/es_es.json, en_us.json)
      └─ data/securelock/ (recipe, loot_table, tags — carpetas en singular desde 1.21)
```

## 11. Recetas y balance
- Recetas caras para evitar spam: puerta reforzada = puerta de hierro + 4 lingotes de hierro + bloque de obsidiana; keypad = redstone + botones de piedra + hierro.
- Loot table: el bloque protegido solo se dropea si lo rompe el propietario (con el Remover) o admin.

## 12. Hoja de ruta por fases
| Fase | Entregable |
|---|---|
| 0 | Proyecto base con Stonecutter compilando para 26.1, 26.2 y 26.3; `runServer` + `runClient` en cada una; CI con GitHub Actions |
| 1 | `OwnableBlockEntity` + Puerta reforzada + protección contra romper/explosiones/pistones |
| 2 | Cofre/Barril con contraseña + networking + KeypadScreen + hash + anti fuerza bruta |
| 3 | Mixins de tolvas, endermen, comparadores; Padlock para bloques vanilla |
| 4 | Universal Modifier, whitelist, equipos, comandos, permisos |
| 5 | Keycards + lector, escáner biométrico |
| 6 | Config, audit log, `SavedData`, límites y purga |
| 7 | Texturas, idiomas, recetas, integración Jade/claims |
| 8 | Pruebas en servidor dedicado en **cada** versión soportada, release en Modrinth/CurseForge (un jar por versión) |

## 13. Plan de pruebas
- Repetir todo el checklist en **cada versión soportada** (26.1, 26.2, 26.3).
- **Servidor dedicado** (`./gradlew runServer`) + 2 clientes (dos instancias `runClient` con nombres distintos, `online-mode=false` solo en local).
- Checklist por bloque protegido, probando como **jugador ajeno**:
  - [ ] Abrir con clic derecho (también agachado y con ítem en mano)
  - [ ] Romper en survival / creativo sin OP
  - [ ] TNT, creeper, cama en Nether, wither
  - [ ] Pistón empujando/tirando
  - [ ] Tolva debajo / al lado / minecart-tolva
  - [ ] Enderman, fuego, lava
  - [ ] Paquetes manipulados (enviar `SubmitCodeC2S` a coords lejanas o código de 10 000 chars)
  - [ ] Fuerza bruta → bloqueo temporal
  - [ ] Reinicio del servidor → datos persisten
  - [ ] Cofre doble: ambas mitades protegidas
- Tests unitarios (JUnit) para `PasscodeHasher` y `BruteForceTracker`; `fabric-gametest-api` para escenarios de mundo.
- Prueba de rendimiento con ~1000 bloques protegidos (sin ticking innecesario en BlockEntities).

## 14. Compatibilidad y riesgos
La compatibilidad es una **prioridad máxima**: el mod nunca debe ser la causa de un crash ni de que otro mod deje de funcionar. Todas las reglas están en la **sección 20**.
- Mods de almacenamiento (Create, Tom's Simple Storage, mods tipo AE2, Sophisticated...): acceden por la Transfer API. Para contenedores bloqueados se devuelve un `Storage` vacío o se deniega, sin romperlos.
- Mods de claims: no duplicar su función; integración opcional.
- Actualizaciones de Minecraft rompen mixins: mantenerlos al mínimo y preferir eventos de Fabric API.

## 15. Recursos
- Docs Fabric: https://docs.fabricmc.net
- Plantilla: https://fabricmc.net/develop/template/
- SecurityCraft: https://github.com/Geforce132/SecurityCraft (referencia de diseño; licencia MIT: si se reutiliza código, conservar el aviso de copyright y la licencia)
- fabric-permissions-api: https://github.com/lucko/fabric-permissions-api

## 16. Versiones soportadas y estrategia multiversión
### Versiones objetivo
| Versión | Estado | Soporte |
|---|---|---|
| **26.3.x** | Última estable (15/09/2026) | Principal: el desarrollo nuevo se hace aquí |
| **26.2.x** | Estable anterior | Soportada |
| **26.1.x** | Primera versión sin ofuscar | Soportada |
| 26.4 | En snapshots | Se porta cuando salga la versión estable |
| 1.21.11 / 1.21.1 | Antiguas (ofuscadas, Java 21) | Opcional, fase posterior; Loom con `officialMojangMappings()` para usar los mismos nombres |

SecurityCraft sigue el mismo modelo: tiene ramas activas para `26.3`, `26.2`, `26.1`, `1.21.11`, `1.21.1` y `1.20.1`, y marca con `eol/` las versiones abandonadas.

### Cómo compilar para varias versiones
- **Stonecutter** (plugin de Gradle): un único código fuente con comentarios condicionales donde la API cambie entre versiones:
  ```java
  //? if >=26.2 {
  /*metodoNuevo();*/
  //?} else
  metodoAntiguo();
  ```
- Un `gradle.properties` por versión (`versions/26.1.2/`, `versions/26.2.x/`, `versions/26.3.x/`) con `minecraft_version`, `fabric_api_version` y `loader_version`.
- `fabric.mod.json` con el rango de cada jar (por ejemplo `"minecraft": "~26.3"`) para que no cargue en una versión no probada.
- Nombre del jar: `securelock-<versión del mod>+mc<versión>.jar`.
- **CI (GitHub Actions):** compilar y ejecutar los gametests de todas las versiones en cada push.
- Política: cuando sale una versión estable nueva se añade, y la más antigua pasa a recibir solo arreglos críticos (`eol`).

### Reducir el trabajo de porteo
- Preferir **eventos de Fabric API** antes que mixins, porque cambian menos entre versiones.
- Aislar todo lo que usa clases de Minecraft en una capa `compat/` pequeña. La lógica de `security/` (hash, fuerza bruta, permisos) no importa clases de Minecraft y no cambia entre versiones.
- Mantener estables el formato NBT y los paquetes de red (campo `dataVersion`), para que un mundo que pasa de 26.1 a 26.3 conserve todas las protecciones.
- Recetas, loot tables, tags y modelos: Mojang cambia su formato con frecuencia. Generarlos por versión con Fabric Data Generation.

### Notas de la serie 26.x
- **Mappings Mojang obligatorios:** desde 26.1 el juego se publica sin ofuscar y Fabric dejó de mantener Yarn. Todos los nombres del plan siguen la nomenclatura de Mojang (`Level`, `BlockEntity`, `Container`, `ServerPlayer`, `SavedData`...).
- **Java 25:** configurar `toolchain` en Gradle y el JDK 25 en el IDE.
- **Dependencias de terceros** (fabric-permissions-api, Jade, OPAC/FTB Chunks...): comprobar en Modrinth para qué versiones existen. Siempre opcionales y cargadas con `FabricLoader.isModLoaded`, para que su ausencia no bloquee ninguna versión.
- Guía oficial de porteo: https://docs.fabricmc.net/develop/porting/

## 17. Cómo ser el mejor mod de seguridad en Fabric
### 17.1 Competencia actual en Fabric (26.x)
| Mod | Enfoque | Lo que le falta (oportunidad) |
|---|---|---|
| Lock and Key | Candados, llaves y lockers | Sin teclados, tarjetas, cámaras ni herramientas de administración avanzadas |
| Chest SignLock / SecureLocks | Protección con carteles, solo en el servidor | Sin bloques de seguridad ni redstone; poco "jugable" |
| Chest Locker | Bloqueo con ítem renombrado y comando | Muy básico |
| SecurityCraft | El más completo | **No tiene versión para Fabric** (comprobarlo antes de publicar) |

**Hueco en el mercado:** no hay un mod de Fabric que combine lo **jugable** de SecurityCraft (teclados, tarjetas, escáneres, cámaras) con las **herramientas de administración** de los plugins de protección de servidor.

### 17.2 Pilares que lo diferencian
1. **Seguridad real, sin trucos para saltársela.** Todo se valida en el servidor y hay una batería de pruebas de bypass (sección 13) que se ejecuta en la CI en cada cambio. Publicar `SECURITY.md` con un canal privado para reportar vulnerabilidades.
2. **Dos modos de instalación:**
   - **Completo** (cliente + servidor): bloques, GUIs y texturas propias.
   - **Solo servidor** (con la librería **Polymer**, si está disponible para 26.x): los jugadores entran con Minecraft vanilla, sin instalar nada. Los bloques se ven como los de vanilla y las GUIs se hacen con menús de cofre. **Gran ventaja para servidores públicos**, y además funciona con jugadores de Bedrock que entran por Geyser.
3. **Herramientas para administradores de primera clase:** permisos con LuckPerms, registro de auditoría, `/securelock inspect` (historial del bloque), rollback de protecciones, purga de jugadores inactivos, límites por rango y exportación del registro a JSON.
4. **Rendimiento:** ningún bloque hace trabajo en cada tick salvo que lo necesite (cámaras, láseres). Caché de permisos por jugador. Objetivo: menos de 0,1 ms por tick con 10 000 bloques protegidos. Medirlo con Spark.
5. **Experiencia de juego cuidada:** GUIs claras, sonidos y partículas al abrir o denegar, mensajes en la barra de acción en vez de spam en el chat, tooltip con el propietario, guía dentro del juego (libro tipo Patchouli o pantalla propia) y traducciones (español, inglés, portugués, francés, alemán...).
6. **Compatibilidad:** integración opcional con claims (OPAC, FTB Chunks, Flan), Jade/WTHIT, REI/EMI (recetas), LuckPerms y mods de almacenamiento mediante la Transfer API de Fabric.
7. **API pública para otros mods** (`securelock-api`): `isProtected(pos)`, `canAccess(player, pos)` y eventos `AccessDeniedEvent` / `BlockLockedEvent`, para que otros mods respeten las protecciones.
8. **Configurable para cualquier servidor:** desde un survival entre amigos (todo permisivo) hasta un servidor de facciones o raids (con "raid windows", horarios en los que se puede asaltar) mediante perfiles preconfigurados.

## 18. Hoja de ruta ampliada (después de v1)
| Versión | Contenido |
|---|---|
| **v1.0** | Fases 0–8 (sección 12): puertas, contenedores, teclado, tarjetas, escáner de retina, administración |
| **v1.x** | Modo solo servidor (Polymer), API pública, guía en el juego, más traducciones |
| **v2.0 "Vigilancia"** | Cámaras de seguridad + monitor para verlas, láseres (trampa o alarma), alarma sonora, detector de movimiento, escáner de inventario (prohíbe pasar con ciertos ítems) |
| **v2.x** | **Bloques reforzados** (piedra, cristal, madera...) que solo el propietario puede romper, para construir bases de verdad |
| **v3.0 "Defensa"** | Torretas (configurables: solo hostiles o también jugadores no autorizados), minas, trampas, "Block Pocket" (bunker sellado) |
| **v3.x** | Panel web opcional para administradores (consulta de auditoría), sincronización entre varios servidores (proxy Velocity) |

Regla: **nada pasa a la siguiente versión sin superar el checklist de bypass** de la sección 13.

## 19. Calidad, comunidad y publicación
- **Repositorio en GitHub** con licencia **MIT** (o LGPL si se quiere que las mejoras de otros vuelvan al proyecto), plantillas de issues, `CONTRIBUTING.md` y `SECURITY.md`.
- **Versionado semántico** y `CHANGELOG.md` en cada release, que avise claramente de los cambios que rompen compatibilidad con la configuración o los mundos.
- **Publicación automática** en Modrinth y CurseForge desde GitHub Actions (por ejemplo con `mc-publish`), con un jar por versión de Minecraft.
- **Página del mod:** GIFs de cada función, tabla de comparación con otros mods y una sección "para administradores".
- **Wiki** (GitHub Pages o la wiki de Modrinth): bloques, recetas, comandos, permisos, configuración y FAQ.
- **Discord** para soporte y para reportar bugs.
- **Beta abierta** en uno o dos servidores públicos antes de la v1.0, con un bug bounty simbólico (por ejemplo, rango o mención en los créditos) para quien encuentre formas de saltarse la protección.
- **Métricas de éxito:** descargas en Modrinth, número de servidores que lo usan, bugs de seguridad abiertos (objetivo: 0 críticos) y tiempo de porteo a cada versión nueva de Minecraft (objetivo: menos de 2 semanas).

## 20. Compatibilidad máxima: "nunca ser el mod que lo rompe"
**Regla de oro:** si algo falla dentro de SecureLock, se **desactiva esa función, se registra un aviso en el log y el juego sigue funcionando**. SecureLock nunca debe crashear el juego ni impedir que cargue otro mod.

### 20.1 No reemplazar ni modificar lo que no es nuestro
- **Proteger sin cambiar el bloque.** Los candados sobre bloques existentes (vanilla o de otros mods) se guardan con la **Fabric Data Attachment API**:
  - en el `BlockEntity`, si el bloque tiene uno (cofres, barriles, contenedores de mods);
  - en el **chunk**, como mapa `BlockPos → LockData`, para bloques sin `BlockEntity` (puertas, trampillas).

  Así el bloque sigue siendo un `ChestBlock`, un cofre de Expanded Storage, etc. Los mods de ordenar inventarios, almacenamiento, renderizado o recetas lo siguen reconociendo.
- **Bloques propios basados en los de vanilla:** `ReinforcedDoorBlock extends DoorBlock`, `LockedChestBlock extends ChestBlock`, etc. Las comprobaciones `instanceof` de otros mods siguen funcionando.
- **Tags convencionales** (`c:chests`, `c:barrels`, `c:wooden_chests`...) en los bloques propios, para que otros mods los reconozcan sin integración específica.
- **Nunca** modificar registros vanilla, cambiar propiedades globales de bloques vanilla ni sustituir clases de vanilla.
- **Qué se puede bloquear se define con tags de datapack:** `securelock:lockable`, con valores por defecto para cofres, puertas, contenedores y `c:chests`, y `securelock:never_lock` para excluir bloques. Los dueños de servidores y de modpacks pueden ajustarlo sin tocar código.

### 20.2 Reglas para los mixins
- **Orden de preferencia:** primero un evento de Fabric API; después `@WrapOperation` / `@ModifyExpressionValue` / `@WrapWithCondition` de **MixinExtras** (incluido en Fabric Loader), que permiten que varios mods modifiquen el mismo punto sin chocar; y como último recurso `@Inject(cancellable = true)`.
- **Prohibido:** `@Overwrite` y `@Redirect`. Son la causa más común de conflictos entre mods.
- **Inyecciones no obligatorias:** `require = 0` en los mixins de compatibilidad, y comprobación al arrancar de qué inyecciones se aplicaron. Si una no se aplicó, porque otro mod cambió ese código, la función correspondiente se desactiva, se avisa en el log y se muestra en `/securelock debug`. El juego no crashea.
- **Mixins condicionales** con un `IMixinConfigPlugin`: los de integración con otro mod solo se aplican si ese mod está cargado (`FabricLoader.isModLoaded`).
- **Todo lo que añadimos lleva prefijo:** campos y métodos `@Unique` con el prefijo `securelock$`, para no chocar con nombres de otros mods.
- **Mínimo número de mixins**, documentados en `MIXINS.md`: qué clase tocan, por qué y qué función se pierde si fallan.
- **Lógica dentro de los mixins:** solo una llamada a `SecureLockHooks.xxx(...)`. Así la lógica se prueba aparte y el mixin queda pequeño.

### 20.3 Robustez en tiempo de ejecución
- **Cada handler de evento y de mixin va dentro de `try/catch`.** Si hay una excepción, se registra una sola vez (sin llenar el log) y se aplica el comportamiento seguro: **un bloque protegido sigue cerrado**, pero el servidor no se cae.
- **Datos corruptos o de una versión futura:** se registra el problema, el bloque queda bloqueado para todos excepto los admins y el juego sigue funcionando.
- **Nunca cargar chunks** para comprobar una protección. Si el chunk no está cargado, no se hace nada.
- **Sin lógica pesada en cada tick** y sin hilos propios que accedan al mundo.
- **Jugadores falsos de otros mods** (`FakePlayer` de Fabric API, usados por mods como Create): se tratan según la config. Por defecto pueden interactuar si el mod indica un propietario autorizado y se deniega en caso contrario, sin lanzar errores. Se puede configurar con una lista de mods permitidos.

### 20.4 Compatibilidad con mods populares (matriz de pruebas)
| Mod | Riesgo | Cómo se evita |
|---|---|---|
| **Sodium / Iris** | Renderizado de bloques propios | Solo modelos JSON estándar y `BlockEntityRenderer` normales; ningún mixin de renderizado |
| **Lithium** | Reescribe la lógica de tolvas, así que un mixin en `HopperBlockEntity` podría no ejecutarse | Proteger también desde la Transfer API / `Container`. Probar la protección contra tolvas con Lithium instalado |
| **Create** | Deployers, brazos mecánicos y contraptions que mueven bloques | Jugador falso con dueño, `PushReaction.BLOCK` y comprobación de que una contraption no se lleve un bloque protegido |
| **Carpet** | Bots y reglas de tolvas o pistones | Los bots se tratan como jugadores; probar las reglas más usadas |
| **Inventory Profiles Next, Mouse Tweaks, mods de ordenar** | Acceso al inventario del contenedor | Si el jugador tiene permiso, el menú se abre de forma normal y todo funciona |
| **REI / EMI / JEI, Jade / WTHIT, ModMenu** | Integraciones de cliente | Plugins opcionales y condicionales |
| **Polymer, Geyser** | Modo solo servidor | Probar con un cliente vanilla y uno de Bedrock |
| **Claims (OPAC, FTB Chunks, Flan), LuckPerms** | Doble protección o permisos distintos | Integración opcional: si un jugador tiene acceso por el claim, decide la config |
| **Mods de almacenamiento** (Tom's, Expanded Storage, mods tipo AE2/RS) | Extracción remota | Transfer API: un `Storage` bloqueado no devuelve ítems a quien no tiene permiso |
| **Mods de rendimiento de servidor** (C2ME, ScalableLux, FerriteCore, ModernFix) | Carga de chunks multihilo, cambios en estados de bloques | No acceder al mundo desde otros hilos, no depender del orden de carga, `BlockState` estándar |

- **Modpack de pruebas en la CI:** un perfil de Gradle descarga estos mods (con Modrinth Maven) y ejecuta los gametests con todos cargados, en cada versión de Minecraft soportada.
- **Prueba manual antes de cada release** en un modpack grande y popular (más de 200 mods) para comprobar el arranque y el uso básico.

### 20.5 Dependencias y metadatos
- **Única dependencia obligatoria: Fabric API.** Todo lo demás va en `suggests` o `recommends` en `fabric.mod.json`, nunca en `depends`.
- **No incluir dentro del jar** (shade ni jar-in-jar) librerías que otros mods también usen en otra versión, salvo librerías propias con un paquete único.
- Usar `breaks` solo ante una incompatibilidad confirmada **sin solución**, y siempre con un mensaje claro.
- **Rangos de versión** correctos para no cargar en versiones no probadas, y avisar en el log en lugar de crashear si una dependencia opcional tiene una versión inesperada.

### 20.6 Desinstalar sin perder el mundo
- `/securelock admin export` y `/securelock admin unlockall`, que quitan todos los candados y convierten los bloques propios en sus equivalentes vanilla **conservando el contenido**.
- Si el mod se elimina sin ese paso:
  - los candados sobre bloques vanilla o de otros mods simplemente desaparecen, porque son datos adjuntos, y el bloque queda intacto;
  - los bloques propios desaparecen, como ocurre con cualquier mod.

  Avisarlo en la documentación.

### 20.7 Proceso cuando aparece una incompatibilidad
- Etiqueta `compat` en GitHub con una plantilla de issue (lista de mods, versión y log).
- Objetivo: **parche en menos de 72 horas** para crashes causados por SecureLock. Mientras tanto, la config permite desactivar la función afectada (`compat.disable = ["hopper_protection", ...]`).
- Cada incompatibilidad resuelta se convierte en una prueba en el modpack de la CI, para que no vuelva a pasar.
