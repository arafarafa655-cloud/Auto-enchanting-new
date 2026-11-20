package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;

import java.util.concurrent.ThreadLocalRandom;

public class ExampleModClient implements ClientModInitializer {
    // Delay antar langkah (dalam tick, 20 tick = 1 detik)
    private static final int BASE_DELAY = 6;
    private static final int RANDOM_DELAY = 5;

    private static boolean running = false;
    private static int wait = 0;
    private static int stuck = 0;
    private static Button toggleButton;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof EnchantmentScreen) {
                running = false;
                toggleButton = Button.builder(Component.literal("Auto: OFF"), b -> toggle())
                        .bounds(w / 2 + 95, h / 2 - 83, 60, 20)
                        .build();
                Screens.getButtons(screen).add(toggleButton);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(ExampleModClient::tick);
    }

    private static void toggle() {
        running = !running;
        wait = 0;
        stuck = 0;
        updateLabel();
    }

    private static void stop() {
        running = false;
        updateLabel();
    }

    private static void updateLabel() {
        if (toggleButton != null) {
            toggleButton.setMessage(Component.literal(running ? "Auto: ON" : "Auto: OFF"));
        }
    }

    private static void delay() {
        wait = BASE_DELAY + ThreadLocalRandom.current().nextInt(RANDOM_DELAY + 1);
    }

    private static int find(EnchantmentMenu menu, Item item) {
        for (int i = 2; i < menu.slots.size(); i++) {
            if (menu.getSlot(i).getItem().is(item)) return i;
        }
        return -1;
    }

    private static void shiftClick(Minecraft mc, EnchantmentMenu menu, int slot) {
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }

    private static void tick(Minecraft mc) {
        if (!running) return;
        if (mc.player == null || mc.gameMode == null
                || !(mc.screen instanceof EnchantmentScreen)
                || !(mc.player.containerMenu instanceof EnchantmentMenu menu)) {
            stop();
            return;
        }
        if (wait > 0) { wait--; return; }

        ItemStack target = menu.getSlot(0).getItem();
        ItemStack lapis = menu.getSlot(1).getItem();

        // 1. Ambil buku yang sudah ter-enchant
        if (target.is(Items.ENCHANTED_BOOK)) {
            shiftClick(mc, menu, 0);
            delay();
            return;
        }
        // 2. Masukkan buku
        if (target.isEmpty()) {
            int b = find(menu, Items.BOOK);
            if (b < 0) { stop(); return; }
            shiftClick(mc, menu, b);
            delay();
            return;
        }
        if (!target.is(Items.BOOK)) { stop(); return; }
        // 3. Masukkan lapis (minimal 3)
        if (lapis.getCount() < 3) {
            int l = find(menu, Items.LAPIS_LAZULI);
            if (l < 0) { stop(); return; }
            shiftClick(mc, menu, l);
            delay();
            return;
        }
        // 4. Klik opsi 3 (index 2)
        int cost = menu.costs[2];
        if (cost <= 0) {
            if (++stuck > 40) stop();
            return;
        }
        stuck = 0;
        if (mc.player.experienceLevel < cost) { stop(); return; }
        mc.gameMode.handleInventoryButtonClick(menu.containerId, 2);
        delay();
    }
}
