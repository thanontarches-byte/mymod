package com.example.donutsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.inventory.SimpleInventory;
import java.util.HashMap;
import java.util.UUID;
import java.util.Random;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public class DonutSMP implements ModInitializer {
    public static final String MOD_ID = "donutsmp";
    public static final Identifier MONEY_SYNC_PACKET = new Identifier(MOD_ID, "money_sync");
    
    // In-memory economy (For production, save to NBT/JSON)
    public static final HashMap<UUID, Integer> economy = new HashMap<>();
    public static final HashMap<UUID, Long> rtpCooldown = new HashMap<>();

    @Override
    public void onInitialize() {
        // Sync money when player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID uuid = handler.player.getUuid();
            if (!economy.containsKey(uuid)) economy.put(uuid, 0);
            syncMoney(handler.player);
        });

        // Register Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            
            // /sell command
            dispatcher.register(literal("sell").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if(player == null) return 0;
                
                ItemStack hand = player.getMainHandStack();
                if (hand.isEmpty()) {
                    player.sendMessage(Text.literal("§cYou are not holding any item!"), false);
                    return 0;
                }
                
                int amount = hand.getCount();
                int price = 0;
                
                if (hand.isOf(Items.DIAMOND)) price = 500 * amount;
                else if (hand.isOf(Items.NETHERITE_INGOT)) price = 5000 * amount;
                else if (hand.isOf(Items.SUGAR_CANE)) price = 70 * amount;
                else if (hand.isOf(Items.ELYTRA)) price = 100000 * amount;
                else if (amount == 64) price = 50; // Generic full stack
                else price = 1 * amount; // Anything else
                
                hand.setCount(0); // Remove item
                addMoney(player, price);
                player.sendMessage(Text.literal("§aYou sold items for $" + price), false);
                return 1;
            }));

            // /shop command
            dispatcher.register(literal("shop").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if(player == null) return 0;
                
                SimpleInventory shopInventory = new SimpleInventory(27);
                // Setup shop items with names and prices in lore (simplified display)
                shopInventory.setStack(0, new ItemStack(Items.TOTEM_OF_UNDYING)); // $1000
                shopInventory.setStack(1, new ItemStack(Items.OAK_LOG)); // $20
                shopInventory.setStack(2, new ItemStack(Items.END_CRYSTAL)); // $200
                shopInventory.setStack(3, new ItemStack(Items.OBSIDIAN)); // $20
                shopInventory.setStack(4, new ItemStack(Items.GOLDEN_APPLE)); // $20
                shopInventory.setStack(5, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)); // $3500
                shopInventory.setStack(6, new ItemStack(Items.GOLDEN_CARROT)); // $30
                
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> 
                    GenericContainerScreenHandler.createGeneric9x3(syncId, inv, shopInventory), 
                    Text.literal("Server Shop")
                ));
                return 1;
            }));

            // /rtp command
            dispatcher.register(literal("rtp").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if(player == null) return 0;
                
                long currentTime = System.currentTimeMillis();
                if (rtpCooldown.containsKey(player.getUuid()) && currentTime - rtpCooldown.get(player.getUuid()) < 10000) {
                    player.sendMessage(Text.literal("§cCooldown! Wait 10 seconds before RTP again."), false);
                    return 0;
                }
                rtpCooldown.put(player.getUuid(), currentTime);
                
                Random random = new Random();
                int x = random.nextInt(10000) - 5000;
                int z = random.nextInt(10000) - 5000;
                World world = player.getWorld();
                int y = 100; // Simplified safe Y check should be implemented
                
                player.teleport(x, y, z);
                player.sendMessage(Text.literal("§aTeleported to random coordinates!"), false);
                return 1;
            }));

            // /givemoney command (OP only)
            dispatcher.register(literal("givemonney")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> {
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if(player != null) {
                        addMoney(player, amount);
                        player.sendMessage(Text.literal("§aGave yourself $" + amount), false);
                    }
                    return 1;
            })));
        });
    }

    public static int getMoney(UUID uuid) {
        return economy.getOrDefault(uuid, 0);
    }

    public static void addMoney(ServerPlayerEntity player, int amount) {
        UUID uuid = player.getUuid();
        economy.put(uuid, getMoney(uuid) + amount);
        syncMoney(player);
    }

    public static void syncMoney(ServerPlayerEntity player) {
        // Pseudo-code for networking buffer to send to client
        // PacketByteBuf buf = PacketByteBufs.create();
        // buf.writeInt(getMoney(player.getUuid()));
        // ServerPlayNetworking.send(player, MONEY_SYNC_PACKET, buf);
    }
}
