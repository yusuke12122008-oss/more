// 新規ファイル
// src/main/java/me/earth/earthhack/impl/modules/combat/autocrystal/ListenerPacketSendSwap.java
package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.AntiWeakness;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.potion.PotionEffect;

/**
 * CPacketHeldItemChange の送信を監視し、SwapStateTracker に記録する。
 *
 * Sn0w の onPacket(PacketEvent.Send) / onPacket で
 *   UpdateSelectedSlotC2SPacket を監視して swapTimer.resetDelay() する処理を
 *   1.12.2 向けに再実装。
 *
 * 重要な点:
 *  - serverSlot と同じスロットへのパケットは無視する（重複排除）
 *  - antiWeakness 用スイッチは swapDelay をリセットしない
 *    （weakness 解除が最優先であるため）
 */
final class ListenerPacketSendSwap
        extends ModuleListener<AutoCrystal,
                               PacketEvent.Send<CPacketHeldItemChange>>
{
    @SuppressWarnings("unchecked")
    public ListenerPacketSendSwap(AutoCrystal module) {
        super(module,
              PacketEvent.Send.class,
              Integer.MIN_VALUE + 1, // 最高優先度より1段下
              CPacketHeldItemChange.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketHeldItemChange> event) {
        CPacketHeldItemChange packet = event.getPacket();
        int newSlot = packet.getSlotId();

        // ---- 重複パケットのキャンセル ----
        // サーバーがすでに認識しているスロットへの再送は不要
        int serverSlot = module.swapStateTracker.getServerSlot();
        if (serverSlot == newSlot) {
            event.cancel(); // 同スロットへの CPacketHeldItemChange をキャンセル
            return;
        }

        // ---- antiWeakness 時はswapDelayをリセットしない ----
        // Sn0w の antiWeakness チェック相当:
        // Weakness 効果が有効かつ AntiWeakness 設定が有効な場合は
        // スワップをweakness解除目的と判断してdelayを掛けない
        if (module.antiWeakness.getValue() != AntiWeakness.None) {
            PotionEffect weakness =
                mc.player.getActivePotionEffect(MobEffects.WEAKNESS);
            PotionEffect strength =
                mc.player.getActivePotionEffect(MobEffects.STRENGTH);
            boolean isWeaknessed = weakness != null
                && (strength == null
                    || strength.getAmplifier() <= weakness.getAmplifier());
            if (isWeaknessed) {
                // weakness解除目的のスワップなのでdelayを記録しない
                // ただし serverSlot は更新する
                module.swapStateTracker.onSwap(
                    mc.player.inventory.currentItem, newSlot, null);
                return;
            }
        }

        // ---- 通常スワップの記録 ----
        // ホットバーのコピーを取ってからトラッカーに渡す
        net.minecraft.item.ItemStack[] copy =
            new net.minecraft.item.ItemStack[9];
        for (int i = 0; i < 9; i++) {
            copy[i] = mc.player.inventory.getStackInSlot(i);
        }
        module.swapStateTracker.onSwap(
            mc.player.inventory.currentItem, newSlot, copy);
    }
}
