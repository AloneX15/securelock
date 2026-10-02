plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
}

val modId = sc.properties.get<String>("mod.id")
val modVersion = sc.properties.get<String>("mod.version")
val mcVersion = sc.current.version

// Nombre del jar: securelock-<versión del mod>+mc<versión>.jar
version = "$modVersion+mc$mcVersion"
group = sc.properties.get<String>("mod.group")
base.archivesName = modId

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }

    runConfigs.all {
        runDirectory = rootProject.file("run")
        generateRunConfig = true
    }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "securelock-test"
        enableGameTests = true
        enableClientGameTests = true
        eula = true
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    // Integraciones opcionales: solo para compilar; en tiempo de ejecución se cargan con FabricLoader.isModLoaded
    compileOnly("maven.modrinth:jade:${sc.properties.get<String>("deps.jade")}")
    "clientCompileOnly"("maven.modrinth:jade:${sc.properties.get<String>("deps.jade")}")
    compileOnly("maven.modrinth:open-parties-and-claims:${sc.properties.get<String>("deps.opac")}")
    compileOnly("maven.modrinth:flan:${sc.properties.get<String>("deps.flan")}")


    // Modpack de pruebas de compatibilidad (PLAN 20.4): ./gradlew runGameTest -PcompatPack
    if (project.hasProperty("compatPack")) {
        localRuntime("maven.modrinth:jade:${sc.properties.get<String>("deps.jade")}")
        localRuntime("maven.modrinth:open-parties-and-claims:${sc.properties.get<String>("deps.opac")}")
        localRuntime("maven.modrinth:forge-config-api-port:${sc.properties.get<String>("compat.fcap")}")
        localRuntime("maven.modrinth:flan:${sc.properties.get<String>("deps.flan")}")
        localRuntime("maven.modrinth:lithium:${sc.properties.get<String>("compat.lithium")}")
        localRuntime("maven.modrinth:ferrite-core:${sc.properties.get<String>("compat.ferritecore")}")
    }

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks {
    withType<JavaCompile>().configureEach { options.release = 25; options.encoding = "UTF-8" }

    test { useJUnitPlatform() }

    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to sc.properties.get<String>("mod.name"),
            "version" to version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    jar {
        from(rootProject.file("LICENSE")) { rename { "${it}_$modId" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Compila y copia los jars a build/libs/<versión del mod>/"
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
        dependsOn("build")
    }
}
