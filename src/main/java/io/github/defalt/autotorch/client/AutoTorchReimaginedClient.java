//Autotorch: A fabric mod to automatically place torches in offhand
//Copyright (C) 2021 Shamil K

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU Lesser General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Lesser General Public License for more details.

//You should have received a copy of the GNU Lesser General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.defalt.autotorch.client;

import com.google.common.collect.ImmutableSet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.Block;
import net.minecraft.world.LightType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutoTorchReimaginedClient implements ClientModInitializer {

    private MinecraftClient minecraftClient;
    public ConfigHolder<ModConfig> configHolder;
    private ModConfig modConfig;
    private static final ImmutableSet<Item> TorchSet = ImmutableSet.of(Items.TORCH, Items.SOUL_TORCH);

    private static final KeyBinding AutoPlaceBinding = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "autotorch-reimagined.autotorch-reimagined.toggle",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    KeyBinding.Category.MISC
            )
    );

    @Override
    public void onInitializeClient() {
        this.minecraftClient = MinecraftClient.getInstance();
        configHolder = AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        modConfig = configHolder.getConfig();
        configHolder.registerLoadListener((manager, data) -> {
            modConfig = data;
            return ActionResult.SUCCESS;
        });
    }

    public void tick(MinecraftClient client) {
        if (client.player != null && client.world != null) {
            if (AutoPlaceBinding.wasPressed()) {
                modConfig.enabled = !modConfig.enabled;
                var message = modConfig.enabled ? Text.translatable("autotorch-reimagined.message.enabled") : Text.translatable("autotorch-reimagined.message.disabled");
                client.player.sendMessage(message, false);
            }
            if (!modConfig.enabled) {
                return;
            }
            if (!TorchSet.contains(client.player.getOffHandStack().getItem())) {
                return;
            }
            if (!modConfig.ignoreSculkSensors && isSculkSensorsNearby(client.player.getBlockPos(), 7)) {
                return;
            }
            BlockPos blockPos = client.player.getBlockPos();
            if (client.world.getLightLevel(LightType.BLOCK, blockPos) < modConfig.lightLevel && canPlaceTorch(blockPos)) {
                offHandRightClickBlock(blockPos);
            }
        }
    }

    private void offHandRightClickBlock(BlockPos pos) {
        Vec3d vec3d = Vec3d.ofBottomCenter(pos);
        if (modConfig.accuratePlacement) {
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(
                    minecraftClient.player.getYaw(),
                    90F,
                    true,
                    minecraftClient.player.isOnGround()
            );
            minecraftClient.player.networkHandler.sendPacket(packet);
        }
        ActionResult one = minecraftClient.interactionManager.interactBlock(minecraftClient.player, Hand.OFF_HAND,
                new BlockHitResult(vec3d, Direction.DOWN, pos, false));
        ActionResult two = minecraftClient.interactionManager.interactItem(minecraftClient.player, Hand.OFF_HAND);
    }

    public boolean canPlaceTorch(BlockPos blockPos) {
        return (minecraftClient.world.getBlockState(blockPos).getFluidState().isEmpty() && Block.sideCoversSmallSquare(minecraftClient.world, blockPos.down(), Direction.UP));
    }

    public boolean isSculkSensorsNearby(BlockPos blockPos, int radius) {
        Iterable<BlockPos> positions = BlockPos.iterate(
                blockPos.add(-radius, -radius, -radius),
                blockPos.add(radius, radius, radius)
        );
        for (BlockPos pos : positions) {
            Block block = minecraftClient.world.getBlockState(pos).getBlock();
            if (block == Blocks.SCULK_SENSOR) {
                return true;
            }
        }
        return false;
    }


}