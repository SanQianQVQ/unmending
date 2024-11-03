package dev.txuritan.unmending;

import dev.txuritan.unmending.api.events.AnvilUpdateEvent;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayerEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;

import java.util.Map;

public class Unmending implements ModInitializer {
    @Override
    public void onInitialize() {
        // 注册铁砧更新事件
        AnvilUpdateEvent.EVENT.register(event -> {
            ItemStack left = event.getLeft();
            ItemStack right = event.getRight();
            ItemStack out = event.getOutput();

            if (out.isEmpty() && (left.isEmpty() || right.isEmpty())) {
                return ActionResult.PASS;
            }

            boolean isMended = false;

            Map<Enchantment, Integer> enchantmentsLeft = EnchantmentHelper.get(left);
            Map<Enchantment, Integer> enchantmentsRight = EnchantmentHelper.get(right);

            if (enchantmentsLeft.containsKey(Enchantments.MENDING) || enchantmentsRight.containsKey(Enchantments.MENDING)) {
                if (left.getItem() == right.getItem()) {
                    isMended = true;
                }

                if (right.getItem() == Items.ENCHANTED_BOOK) {
                    isMended = true;
                }
            }

            if (isMended) {
                if (out.isEmpty()) {
                    out = left.copy();
                }

                if (!out.hasNbt()) {
                    out.setNbt(new NbtCompound());
                }

                Map<Enchantment, Integer> enchantmentsOutput = EnchantmentHelper.get(out);
                enchantmentsOutput.putAll(enchantmentsRight);
                enchantmentsOutput.remove(Enchantments.MENDING);
                EnchantmentHelper.set(enchantmentsOutput, out);

                out.setRepairCost(0);
                if (out.isDamageable()) {
                    out.setDamage(0);
                }

                event.setOutput(out);
                if (event.getCost() == 0) {
                    event.setCost(1);
                }

                return ActionResult.CONSUME;
            }

            return ActionResult.PASS;
        });

        // 监听玩家加入服务器事件，移除装备上的经验修补附魔
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            removeMendingFromEquippedItems(newPlayer);
        });

        // 监听玩家每次加入游戏或重生时移除经验修补附魔
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            removeMendingFromEquippedItems(newPlayer);
        });
    }

    /**
     * 移除玩家装备上的 Mending 附魔
     */
    private void removeMendingFromEquippedItems(PlayerEntity player) {
        // 处理玩家身上的所有装备（包括头部、胸部、腿部、脚部）
        for (ItemStack itemStack : player.getArmorItems()) {
            removeMendingEnchantment(itemStack);
        }

        // 处理玩家主手和副手的物品
        ItemStack mainHandItem = player.getMainHandStack();
        ItemStack offHandItem = player.getOffHandStack();

        removeMendingEnchantment(mainHandItem);
        removeMendingEnchantment(offHandItem);
    }

    /**
     * 移除指定物品上的 Mending 附魔
     */
    private void removeMendingEnchantment(ItemStack itemStack) {
        if (itemStack != null && EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) > 0) {
            // 获取物品的附魔数据
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
            enchantments.remove(Enchantments.MENDING);
            EnchantmentHelper.set(enchantments, itemStack);
        }
    }
}
