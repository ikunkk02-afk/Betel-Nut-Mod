package betel.nut.client;

import betel.nut.BetelNutMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Static, standard baked quads: folded leaves without a block entity or per-frame work. */
public final class BetelPalmMeshModels {
    private record Face(float[][] vertices, float[][] uv, int material, boolean doubleSided) { }
    private record Geometry(List<ResourceLocation> textures, List<Face> faces) { }

    public static void register() {
        PreparableModelLoadingPlugin.register((resources, executor) -> CompletableFuture.supplyAsync(() -> {
            Map<String, Geometry> models = new HashMap<>();
            resources.listResources("palm_mesh", id -> id.getNamespace().equals("betel-nut-mod")
                    && id.getPath().endsWith(".json")).forEach((id, resource) -> {
                try (var reader = resource.openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported palm mesh format");
                    List<ResourceLocation> textures = new ArrayList<>();
                    for (var t : json.getAsJsonArray("textures")) textures.add(ResourceLocation.parse(t.getAsString()));
                    List<Face> faces = new ArrayList<>();
                    for (var element : json.getAsJsonArray("quads")) {
                        var q = element.getAsJsonObject();
                        int material = q.get("material").getAsInt();
                        if (material < 0 || material >= textures.size()) throw new IllegalArgumentException("Invalid texture index");
                        faces.add(new Face(readVectors(q.getAsJsonArray("vertices"), 3),
                                readVectors(q.getAsJsonArray("uv"), 2), material, q.get("doubleSided").getAsBoolean()));
                    }
                    models.put(id.getPath().substring("palm_mesh/".length(), id.getPath().length() - 5),
                            new Geometry(List.copyOf(textures), List.copyOf(faces)));
                } catch (Exception e) {
                    BetelNutMod.LOGGER.error("Unable to read palm mesh {}; using the Java cube model", id, e);
                }
            });
            BetelNutMod.LOGGER.info("Loaded {} folded palm mesh parts", models.size());
            return Map.copyOf(models);
        }, executor), (models, context) -> context.modifyModelAfterBake().register((original, bake) -> {
            var id = bake.resourceId();
            // Inventory models are baked as top-level models, without a resourceId.
            // Do not substitute block-state top levels: they include distinct crown parts/styles.
            if (id == null && bake.topLevelId() != null && "inventory".equals(bake.topLevelId().variant())) {
                id = bake.topLevelId().id();
            }
            if (original == null || id == null || !id.getNamespace().equals("betel-nut-mod")) return original;
            String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
            Geometry geometry = models.get(name);
            if (geometry == null) return original;
            List<TextureAtlasSprite> sprites = geometry.textures().stream().map(texture ->
                    bake.textureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture))).toList();
            List<BakedQuad> quads = new ArrayList<>();
            Matrix4f rotation = bake.settings().getRotation().getMatrix();
            for (Face face : geometry.faces()) {
                append(quads, face, sprites.get(face.material()), rotation, false);
                if (face.doubleSided()) append(quads, face, sprites.get(face.material()), rotation, true);
            }
            return new PalmModel(original, List.copyOf(quads));
        }));
    }

    private static float[][] readVectors(JsonArray array, int dimensions) {
        if (array.size() != 4) throw new IllegalArgumentException("A quad must have four vertices");
        float[][] result = new float[4][dimensions];
        for (int i = 0; i < 4; i++) {
            JsonArray vector = array.get(i).getAsJsonArray();
            if (vector.size() != dimensions) throw new IllegalArgumentException("Invalid vertex dimensions");
            for (int j = 0; j < dimensions; j++) {
                float value = vector.get(j).getAsFloat();
                if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite coordinate");
                result[i][j] = value;
            }
        }
        return result;
    }

    private static void append(List<BakedQuad> result, Face face, TextureAtlasSprite sprite,
            Matrix4f rotation, boolean reverse) {
        int[] order = reverse ? new int[]{3, 2, 1, 0} : new int[]{0, 1, 2, 3};
        Vector3f[] points = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            float[] v = face.vertices()[order[i]];
            points[i] = new Vector3f(v[0] / 16 - .5f, v[1] / 16 - .5f, v[2] / 16 - .5f);
            rotation.transformPosition(points[i]);
            points[i].add(.5f, .5f, .5f);
        }
        // A pointed blade ends in a triangle encoded by a repeated quad vertex.
        Vector3f normal = new Vector3f();
        for (int i = 1; i < 3 && normal.lengthSquared() < 1.0e-12f; i++) {
            new Vector3f(points[i]).sub(points[0]).cross(new Vector3f(points[i + 1]).sub(points[0]), normal);
        }
        if (normal.lengthSquared() < 1.0e-12f) return;
        normal.normalize();
        int packedNormal = ((int)(normal.x * 127) & 255) | (((int)(normal.y * 127) & 255) << 8)
                | (((int)(normal.z * 127) & 255) << 16);
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            int k = i * 8;
            vertices[k] = Float.floatToRawIntBits(points[i].x);
            vertices[k + 1] = Float.floatToRawIntBits(points[i].y);
            vertices[k + 2] = Float.floatToRawIntBits(points[i].z);
            vertices[k + 3] = -1;
            vertices[k + 4] = Float.floatToRawIntBits(sprite.getU(face.uv()[order[i]][0]));
            vertices[k + 5] = Float.floatToRawIntBits(sprite.getV(face.uv()[order[i]][1]));
            vertices[k + 7] = packedNormal;
        }
        result.add(new BakedQuad(vertices, -1, Direction.getNearest(normal.x, normal.y, normal.z), sprite, true));
    }

    private record PalmModel(BakedModel original, List<BakedQuad> quads) implements BakedModel {
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return side == null ? quads : List.of();
        }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return original.isGui3d(); }
        @Override public boolean usesBlockLight() { return original.usesBlockLight(); }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return original.getOverrides(); }
    }

    private BetelPalmMeshModels() { }
}
