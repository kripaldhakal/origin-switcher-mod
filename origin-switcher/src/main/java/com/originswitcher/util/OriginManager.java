package com.originswitcher.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class OriginManager {

    private static Class<?> originRegistryClass;
    private static Class<?> originLayersClass;
    private static Class<?> originClass;
    private static Class<?> originLayerClass;
    private static Class<?> modComponentsClass;
    private static Class<?> originComponentClass;
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;

    private static void initReflection() {
        if (reflectionInitialized || reflectionFailed) return;
        try {
            originRegistryClass  = Class.forName("io.github.apace100.origins.origin.OriginRegistry");
            originClass          = Class.forName("io.github.apace100.origins.origin.Origin");
            originLayerClass     = Class.forName("io.github.apace100.origins.origin.OriginLayer");
            modComponentsClass   = Class.forName("io.github.apace100.origins.registry.ModComponents");
            originComponentClass = Class.forName("io.github.apace100.origins.component.OriginComponent");

            // Try different class names for OriginLayers across versions
            for (String candidate : new String[]{
                    "io.github.apace100.origins.origin.OriginLayers",
                    "io.github.apace100.origins.registry.OriginLayers",
                    "io.github.apace100.origins.Origins"
            }) {
                try {
                    originLayersClass = Class.forName(candidate);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            reflectionInitialized = true;
        } catch (ClassNotFoundException e) {
            reflectionFailed = true;
        }
    }

    public static boolean isOriginsLoaded() {
        initReflection();
        return reflectionInitialized;
    }

    public static List<Identifier> getAllOriginIds() {
        initReflection();
        if (reflectionFailed) return Collections.emptyList();
        try {
            Method valuesMethod = originRegistryClass.getMethod("values");
            Collection<?> origins = (Collection<?>) valuesMethod.invoke(null);
            List<Identifier> ids = new ArrayList<>();
            Method getIdMethod = originClass.getMethod("getIdentifier");
            for (Object origin : origins) {
                Identifier id = (Identifier) getIdMethod.invoke(origin);
                if (!id.getPath().equals("empty")) ids.add(id);
            }
            ids.sort(Comparator.comparing(Identifier::toString));
            return ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<String> getAllLayerIds() {
        initReflection();
        if (reflectionFailed || originLayersClass == null) return Collections.emptyList();

        // Try getLayers() method
        for (String methodName : new String[]{"getLayers", "getAll", "values", "entries"}) {
            try {
                Method m = originLayersClass.getMethod(methodName);
                Object result = m.invoke(null);
                if (result instanceof Collection) {
                    List<String> ids = new ArrayList<>();
                    Method getIdMethod = originLayerClass.getMethod("getIdentifier");
                    for (Object layer : (Collection<?>) result) {
                        Identifier id = (Identifier) getIdMethod.invoke(layer);
                        ids.add(id.toString());
                    }
                    if (!ids.isEmpty()) return ids;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: return default layer
        return Collections.singletonList("origins:origin");
    }

    private static Object getLayerObject(String layerIdStr) {
        initReflection();
        if (reflectionFailed || originLayersClass == null) return null;

        Identifier layerId = new Identifier(layerIdStr);

        // Try getLayer(Identifier)
        for (String methodName : new String[]{"getLayer", "get", "fromId"}) {
            try {
                Method m = originLayersClass.getMethod(methodName, Identifier.class);
                Object result = m.invoke(null, layerId);
                if (result != null) return result;
            } catch (Exception ignored) {}
        }

        // Try iterating all layers and matching by identifier
        try {
            List<String> layerIds = getAllLayerIds();
            if (layerIds.contains(layerIdStr)) {
                for (String methodName : new String[]{"getLayers", "getAll", "values"}) {
                    try {
                        Method m = originLayersClass.getMethod(methodName);
                        Object result = m.invoke(null);
                        if (result instanceof Collection) {
                            Method getIdMethod = originLayerClass.getMethod("getIdentifier");
                            for (Object layer : (Collection<?>) result) {
                                Identifier id = (Identifier) getIdMethod.invoke(layer);
                                if (id.toString().equals(layerIdStr)) return layer;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static Optional<String> getCurrentOriginId(PlayerEntity player, String layerIdStr) {
        initReflection();
        if (reflectionFailed) return Optional.empty();
        try {
            Object componentKey = modComponentsClass.getField("ORIGIN").get(null);
            Method getCompMethod = componentKey.getClass().getMethod("get", Object.class);
            Object component = getCompMethod.invoke(componentKey, player);
            if (component == null) return Optional.empty();

            Object layer = getLayerObject(layerIdStr);
            if (layer == null) return Optional.empty();

            Method getOriginMethod = originComponentClass.getMethod("getOrigin", originLayerClass);
            Object origin = getOriginMethod.invoke(component, layer);
            if (origin == null) return Optional.empty();

            Method getIdMethod = originClass.getMethod("getIdentifier");
            Identifier id = (Identifier) getIdMethod.invoke(origin);
            return Optional.of(id.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean applyOriginLocally(PlayerEntity player, Identifier layerId, Identifier originId) {
        initReflection();
        if (reflectionFailed) return false;
        try {
            // Get component
            Object componentKey = modComponentsClass.getField("ORIGIN").get(null);
            Method getCompMethod = componentKey.getClass().getMethod("get", Object.class);
            Object component = getCompMethod.invoke(componentKey, player);
            if (component == null) return false;

            // Get layer
            Object layer = getLayerObject(layerId.toString());
            if (layer == null) return false;

            // Get origin
            Method getOriginMethod = originRegistryClass.getMethod("get", Identifier.class);
            Object origin = getOriginMethod.invoke(null, originId);
            if (origin == null) return false;

            // Check not empty
            try {
                Object emptyOrigin = originClass.getField("EMPTY").get(null);
                if (origin.equals(emptyOrigin)) return false;
            } catch (Exception ignored) {}

            // Set origin
            Method setOriginMethod = originComponentClass.getMethod("setOrigin", originLayerClass, originClass);
            setOriginMethod.invoke(component, layer, origin);

            // Sync
            try {
                Method syncMethod = originComponentClass.getMethod("sync");
                syncMethod.invoke(component);
            } catch (Exception ignored) {}

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String buildDataCommand(Identifier layerId, Identifier originId) {
        return String.format(
                "data merge entity @s {origins:{origins:[{Origin:\"%s\",Layer:\"%s\"}]}}",
                originId.toString(), layerId.toString()
        );
    }

    public static void sendServerCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    public static List<Identifier> searchOrigins(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        return getAllOriginIds().stream()
                .filter(id -> id.toString().toLowerCase(Locale.ROOT).contains(lower))
                .collect(Collectors.toList());
    }
}