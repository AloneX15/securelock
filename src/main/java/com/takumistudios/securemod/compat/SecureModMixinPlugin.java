package com.takumistudios.securemod.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin de mixins:
 * <ul>
 *   <li>Mixins condicionales: los de integración solo se aplican si el mod correspondiente está cargado.</li>
 *   <li>Comprobación al cargar cada clase: si una inyección no se conectó (porque otro mod cambió ese código),
 *   se avisa en el log y se muestra en {@code /securemod debug}. El juego no crashea.</li>
 * </ul>
 * MixinExtras aplica {@code @WrapOperation}, {@code @ModifyExpressionValue} y {@code @WrapWithCondition} en una fase
 * tardía (en una extensión de Mixin), así que la comprobación final se hace en una extensión propia que corre después.
 */
public final class SecureModMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("securemod-mixins");
    private static final String PACKAGE = "com.takumistudios.securemod.mixin.";
    private static final String PREFIX = "securemod$";

    /** Mixin -> función que protege. */
    private static final Map<String, String> FEATURES = Map.of(
            "ServerExplosionMixin", "explosion_protection",
            "HopperBlockEntityMixin", "hopper_protection",
            "PistonStructureResolverMixin", "piston_protection",
            "FireBlockMixin", "fire_protection",
            "WitherBossMixin", "mob_protection",
            "EnderDragonMixin", "mob_protection",
            "ComparatorBlockMixin", "comparator_protection",
            "TransportItemsBetweenContainersMixin", "golem_protection",
            "BlockApiLookupImplMixin", "transfer_protection",
            "DoorBlockMixin", "mob_protection");

    /** Mixins que solo se aplican si un mod está cargado. */
    private static final Map<String, String> REQUIRED_MODS = Map.of(
            "BlockApiLookupImplMixin", "fabric-transfer-api-v1");

    /** Estado por mixin: true = todas sus inyecciones conectadas. */
    private static final Map<String, Boolean> STATUS = new ConcurrentHashMap<>();
    /** Clase destino (formato interno) -> mixins aplicados con sus handlers, pendientes de la comprobación final. */
    private static final Map<String, List<Pending>> PENDING = new ConcurrentHashMap<>();
    private static final Set<String> FAILED_FEATURES = ConcurrentHashMap.newKeySet();

    private record Pending(String mixin, List<String> handlers) {
    }

    private static final VerificationExtension EXTENSION = new VerificationExtension();
    private static boolean verificationUnavailable;

    @Override
    public void onLoad(String mixinPackage) {
    }

    /**
     * Coloca nuestra extensión al final de la lista, después de las de MixinExtras (que se registran más tarde
     * que nuestro plugin). Se llama antes de que Mixin ejecute las extensiones de la clase actual.
     */
    private static void ensureVerificationLast() {
        if (verificationUnavailable) {
            return;
        }
        try {
            List<IExtension> active = com.llamalad7.mixinextras.utils.MixinInternals.getExtensions().getActiveExtensions();
            if (!active.isEmpty() && active.get(active.size() - 1) == EXTENSION) {
                return;
            }
            com.llamalad7.mixinextras.utils.MixinInternals.unregisterExtension(EXTENSION);
            com.llamalad7.mixinextras.utils.MixinInternals.registerExtension(EXTENSION);
        } catch (Throwable t) {
            verificationUnavailable = true;
            LOGGER.info("[Secure Mod] No se pueden verificar las inyecciones ({}); /securemod debug no mostrará su estado", t.toString());
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = simpleName(mixinClassName);
        String requiredMod = REQUIRED_MODS.get(simple);
        if (requiredMod != null && !FabricLoader.getInstance().isModLoaded(requiredMod)) {
            STATUS.put(simple, false);
            LOGGER.info("[Secure Mod] {} no se aplica: falta el mod {}", simple, requiredMod);
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        ensureVerificationLast();
        try {
            List<String> handlers = new ArrayList<>();
            for (MethodNode method : mixinInfo.getClassNode(0).methods) {
                if (method.name.startsWith(PREFIX)) {
                    handlers.add(method.name);
                }
            }
            PENDING.computeIfAbsent(targetClass.name, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new Pending(simpleName(mixinClassName), handlers));
        } catch (RuntimeException e) {
            LOGGER.debug("[Secure Mod] No se pudo preparar la verificación de {}", mixinClassName, e);
        }
    }

    /** Se ejecuta después de que MixinExtras aplique sus inyecciones tardías. */
    private static final class VerificationExtension implements IExtension {
        @Override
        public boolean checkActive(MixinEnvironment environment) {
            return true;
        }

        @Override
        public void preApply(ITargetClassContext context) {
        }

        @Override
        public void postApply(ITargetClassContext context) {
            ClassNode node = context.getClassNode();
            List<Pending> pending = PENDING.remove(node.name);
            if (pending == null) {
                return;
            }
            for (Pending entry : pending) {
                List<String> missing = new ArrayList<>();
                for (String handler : entry.handlers()) {
                    if (!isCalled(node, handler)) {
                        missing.add(handler);
                    }
                }
                boolean ok = missing.isEmpty();
                STATUS.merge(entry.mixin(), ok, Boolean::logicalAnd);
                if (!ok) {
                    String feature = FEATURES.getOrDefault(entry.mixin(), entry.mixin());
                    FAILED_FEATURES.add(feature);
                    LOGGER.warn("[Secure Mod] Inyección(es) {} de {} no conectada(s) en {} (probablemente otro mod cambió ese código). "
                            + "La función '{}' puede no funcionar. El juego sigue funcionando.", missing, entry.mixin(), node.name, feature);
                }
            }
        }

        @Override
        public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
        }
    }

    /** ¿Algún método de la clase (que no sea el propio handler) llama al handler? */
    private static boolean isCalled(ClassNode targetClass, String handler) {
        for (MethodNode method : targetClass.methods) {
            if (method.instructions == null || method.name.endsWith(handler)) {
                continue;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.name.endsWith(handler)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String simpleName(String mixinClassName) {
        return mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
    }

    /** Estado de cada mixin aplicado hasta ahora (true = conectado). Los no cargados aún no aparecen. */
    public static Map<String, Boolean> status() {
        return Collections.unmodifiableMap(new TreeMap<>(STATUS));
    }

    public static boolean isFeatureFailed(String feature) {
        return FAILED_FEATURES.contains(feature);
    }
}
