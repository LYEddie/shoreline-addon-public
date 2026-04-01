package me.lyeddie.addon.api;

import com.mojang.blaze3d.systems.RenderSystem;
import me.lyeddie.addon.mixin.accessor.AccessorTextRenderer;
import me.lyeddie.addon.mixin.accessor.AccessorWorldRenderer;
import me.lyeddie.addon.util.Globals;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import org.lwjgl.opengl.GL11;

import static me.lyeddie.addon.api.RenderBuffers.LINES;
import static me.lyeddie.addon.api.RenderBuffers.QUADS;

public class RenderManager implements Globals {

    public static void post(Runnable callback) {
        RenderBuffers.post(callback);
    }

    public static void renderBox(MatrixStack matrices, BlockPos p, int color) {
        renderBox(matrices, new Box(p), color);
    }

    public static void renderBox(MatrixStack matrices, Box box, int color) {
        if (!isFrustumVisible(box)) {
            return;
        }
        matrices.push();
        drawBox(matrices, box, color);
        matrices.pop();
    }

    public static void drawBox(MatrixStack matrices, Box box, int color) {
        drawBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
    }

    public static void drawBox(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        QUADS.begin(matrices);
        QUADS.color(color);
        QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
        QUADS.vertex(x1, y2, z1).vertex(x1, y2, z2).vertex(x2, y2, z2).vertex(x2, y2, z1);
        QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
        QUADS.vertex(x2, y1, z1).vertex(x2, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
        QUADS.vertex(x1, y1, z2).vertex(x2, y1, z2).vertex(x2, y2, z2).vertex(x1, y2, z2);
        QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);
        QUADS.end();
    }

    public static void renderPlane(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        matrices.push();
        drawPlane(matrices, x1, y1, z1, x2, y2, z2, color);
        matrices.pop();
    }

    public static void drawPlane(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        QUADS.begin(matrices);
        QUADS.color(color);
        QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
        QUADS.end();
    }

    public static void renderBoundingCross(MatrixStack matrices, Box box, float width, int color) {
        if (!isFrustumVisible(box)) {
            return;
        }
        matrices.push();
        RenderSystem.lineWidth(width);
        drawBoundingCross(matrices, box, color);
        matrices.pop();
    }

    public static void drawBoundingCross(MatrixStack matrices, Box box, int color) {
        drawBoundingCross(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
    }

    public static void drawBoundingCross(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        LINES.begin(matrices);
        LINES.color(color);
        LINES.vertexLine(x1, y1, z1, x2, y1, z2);
        LINES.vertexLine(x2, y1, z1, x1, y1, z2);
        LINES.end();
    }

    public static void renderBoundingBox(MatrixStack matrices, BlockPos p, float width, int color) {
        renderBoundingBox(matrices, new Box(p), width, color);
    }

    public static void renderBoundingBox(MatrixStack matrices, Box box, float width, int color) {
        if (!isFrustumVisible(box)) {
            return;
        }
        matrices.push();
        RenderSystem.lineWidth(width);
        drawBoundingBox(matrices, box, color);
        matrices.pop();
    }

    public static void drawBoundingBox(MatrixStack matrices, Box box, int color) {
        drawBoundingBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
    }

    public static void drawBoundingBox(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        LINES.begin(matrices);
        LINES.color(color);
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        if (dy == 0.0) {
            LINES.vertexLine(x1, y1, z1, x2, y1, z1);
            LINES.vertexLine(x1, y1, z1, x1, y1, z2);
            LINES.vertexLine(x2, y1, z2, x1, y1, z2);
            LINES.vertexLine(x2, y1, z2, x2, y1, z1);
            LINES.end();
            return;
        }
        VoxelShapes.cuboid(0.0, 0.0, 0.0, dx, dy, dz).forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) ->
            LINES.vertexLine(minX + x1, minY + y1, minZ + z1, maxX + x1, maxY + y1, maxZ + z1));
        LINES.end();
    }

    public static void renderSign(String text, Vec3d pos, int color) {
        renderSign(text, pos.getX(), pos.getY(), pos.getZ(), color);
    }

    public static void renderSign(String text, double x, double y, double z, int color) {
        Camera camera = mc.gameRenderer.getCamera();
        final Vec3d pos = camera.getPos();
        double dist = Math.sqrt(pos.squaredDistanceTo(x, y, z));
        float scaling = (float) (0.0018f + 0.003 * dist); // replace 0.003 with custom val
        if (dist <= 8.0) {
            scaling = 0.0245f;
        }
        renderSign(text, x, y, z, scaling, color);
    }

    public static void renderSign(String text, double x, double y, double z, float scaling, int color) {
        Camera camera = mc.gameRenderer.getCamera();
        final Vec3d pos = camera.getPos();
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - pos.getX(), y - pos.getY(), z - pos.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(-scaling, -scaling, -1.0f);
        float hwidth = mc.textRenderer.getWidth(text) / 2.0f;
        RenderManager.post(() -> {
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            ((AccessorTextRenderer) mc.textRenderer).hookDrawLayer(text, -hwidth, 0.0f, TextRenderer.tweakTransparency(color), true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.draw();
            ((AccessorTextRenderer) mc.textRenderer).hookDrawLayer(text, -hwidth, 0.0f, TextRenderer.tweakTransparency(color), false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.draw();
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        });
    }

    public static boolean isFrustumVisible(Box box) {
        return ((AccessorWorldRenderer) mc.worldRenderer).getFrustum().isVisible(box);
    }
}
