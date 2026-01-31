package com.pvphelper;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArrowPrediction {
    private static final double AIR_DRAG = 0.99;
    private static final double WATER_DRAG = 0.6;
    private static final double GRAVITY = 0.05;
    private static final int MAX_STEPS = 200;
    private static final double PLAYER_HITBOX_EXPAND = 0.3;
    private static final double SEARCH_RADIUS = 128.0;

    private static final Map<Integer, PredictionResult> ARROW_PREDICTIONS = new HashMap<>();
    private static final Set<Integer> WARNED_ARROWS = new HashSet<>();
    private static PredictionResult aimPrediction;

    private ArrowPrediction() {
    }

    public static void update(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        World world = client.world;
        ARROW_PREDICTIONS.clear();
        Set<Integer> seenArrows = new HashSet<>();

        Box searchBox = client.player.getBoundingBox().expand(SEARCH_RADIUS);
        List<PersistentProjectileEntity> arrows = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                searchBox,
                entity -> entity.getType() == EntityType.ARROW || entity.getType() == EntityType.SPECTRAL_ARROW
        );

        for (PersistentProjectileEntity arrow : arrows) {
            if (arrow.isRemoved()) {
                continue;
            }

            PredictionResult result = simulate(world, arrow.getPos(), arrow.getVelocity(), arrow.getOwner(), arrow);
            if (result == null) {
                continue;
            }

            ARROW_PREDICTIONS.put(arrow.getId(), result);
            seenArrows.add(arrow.getId());

            if (result.hitPlayer() && WARNED_ARROWS.add(arrow.getId())) {
                sendWarning(client, result);
            }
        }

        WARNED_ARROWS.retainAll(seenArrows);
        aimPrediction = computeAimPrediction(client);
    }

    public static void render(WorldRenderContext context) {
        if (ARROW_PREDICTIONS.isEmpty() && aimPrediction == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumerProvider consumers = context.consumers();

        for (PredictionResult result : ARROW_PREDICTIONS.values()) {
            if (result == null) {
                continue;
            }
            if (result.hitPlayer()) {
                drawMarker(matrices, consumers, result.position(), 1.0f, 0.2f, 0.2f, 0.9f, 0.4);
            } else {
                drawMarker(matrices, consumers, result.position(), 1.0f, 0.85f, 0.2f, 0.7f, 0.35);
            }
        }

        if (aimPrediction != null) {
            drawMarker(matrices, consumers, aimPrediction.position(), 0.2f, 0.9f, 1.0f, 0.8f, 0.45);
        }

        matrices.pop();
    }

    private static PredictionResult computeAimPrediction(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || !player.isUsingItem()) {
            return null;
        }

        ItemStack active = player.getActiveItem();
        if (!(active.getItem() instanceof BowItem)) {
            return null;
        }

        int usedTicks = active.getMaxUseTime() - player.getItemUseTimeLeft();
        float pullProgress = BowItem.getPullProgress(usedTicks);
        if (pullProgress <= 0.05f) {
            return null;
        }

        float velocity = pullProgress * 3.0f;
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d start = player.getEyePos().add(direction.multiply(0.1));
        Vec3d initialVelocity = direction.multiply(velocity).add(player.getVelocity());

        return simulate(player.getWorld(), start, initialVelocity, player, player);
    }

    private static PredictionResult simulate(World world, Vec3d start, Vec3d velocity, Entity owner, Entity source) {
        Vec3d position = start;
        Vec3d motion = velocity;

        for (int step = 0; step < MAX_STEPS; step++) {
            Vec3d next = position.add(motion);

            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    position,
                    next,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    source
            ));

            double closestDistance = Double.POSITIVE_INFINITY;
            Vec3d closestHit = null;
            String hitName = null;

            for (PlayerEntity player : world.getPlayers()) {
                if (player == owner || !player.isAlive()) {
                    continue;
                }
                Box expanded = player.getBoundingBox().expand(PLAYER_HITBOX_EXPAND);
                var hit = expanded.raycast(position, next);
                if (hit.isPresent()) {
                    double distance = position.distanceTo(hit.get());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestHit = hit.get();
                        hitName = player.getName().getString();
                    }
                }
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                double blockDistance = position.distanceTo(blockHit.getPos());
                if (closestHit != null && closestDistance <= blockDistance) {
                    return new PredictionResult(closestHit, true, hitName);
                }
                return new PredictionResult(blockHit.getPos(), false, null);
            }

            if (closestHit != null) {
                return new PredictionResult(closestHit, true, hitName);
            }

            position = next;
            double drag = isInWater(world, position) ? WATER_DRAG : AIR_DRAG;
            motion = motion.multiply(drag).add(0.0, -GRAVITY, 0.0);

            if (position.y < world.getBottomY() - 10) {
                break;
            }
        }

        return new PredictionResult(position, false, null);
    }

    private static void sendWarning(MinecraftClient client, PredictionResult result) {
        if (client.player == null) {
            return;
        }

        Text message;
        if (result.playerName() != null) {
            message = Text.translatable("text.pvp_helper.arrow_warning_target", result.playerName())
                    .formatted(Formatting.RED);
        } else {
            message = Text.translatable("text.pvp_helper.arrow_warning").formatted(Formatting.RED);
        }

        client.player.sendMessage(message, true);
    }

    private static void drawMarker(MatrixStack matrices,
                                   VertexConsumerProvider consumers,
                                   Vec3d position,
                                   float red,
                                   float green,
                                   float blue,
                                   float alpha,
                                   double size) {
        double half = size / 2.0;
        Box box = new Box(
                position.x - half,
                position.y - half,
                position.z - half,
                position.x + half,
                position.y + half,
                position.z + half
        );

        WorldRenderer.drawBox(
                matrices,
                consumers.getBuffer(RenderLayer.getLines()),
                box,
                red,
                green,
                blue,
                alpha
        );
    }

    private record PredictionResult(Vec3d position, boolean hitPlayer, String playerName) {
    }

    private static boolean isInWater(World world, Vec3d position) {
        BlockPos pos = new BlockPos(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );
        return world.getFluidState(pos).isIn(FluidTags.WATER);
    }
}
