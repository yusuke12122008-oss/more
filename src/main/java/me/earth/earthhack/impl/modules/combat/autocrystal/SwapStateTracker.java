// 新規ファイル
// src/main/java/me/earth/earthhack/impl/modules/combat/autocrystal/SwapStateTracker.java
package me.earth.earthhack.impl.modules.combat.autocrystal;

import net.minecraft.item.ItemStack;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * スロット変更の履歴をタイムスタンプ付きで管理するトラッカー。
 *
 * Sn0w の PreSwapData + RotationManager.swapData リストを
 * 1.12.2 向けに再実装。
 *
 * 役割:
 *  1. スワップが"いつ行われたか"を ms 単位で記録する
 *  2. swapDelay 設定値と比較して、次のplace/breakをブロックするか判定する
 *  3. 古いエントリを自動削除（clearTimeout ms 後）
 *
 * 使い方:
 *   - CPacketHeldItemChange / 直接スロット変更後 → onSwap(fromSlot, toSlot, hotbar)
 *   - AbstractCalculation.breakCheck() / placeCheck() → hasPassedDelay(ms) で確認
 *   - ListenerPacketSend で CPacketHeldItemChange を監視して onSwap() を呼ぶ
 */
public final class SwapStateTracker {

    /** 履歴エントリの最大保持時間(ms) — 古いスワップ記録を自動削除 */
    private static final long CLEAR_TIMEOUT_MS = 1000L;

    /** スワップ履歴リスト。CopyOnWriteArrayList でスレッドセーフ */
    private final CopyOnWriteArrayList<SwapEntry> history = new CopyOnWriteArrayList<>();

    /** サーバー側で認識している現在のスロット番号（serverSlot 相当）*/
    private volatile int serverSlot = -1;

    // ================================================================
    // Public API
    // ================================================================

    /**
     * スロット変更が発生したときに呼ぶ。
     * CPacketHeldItemChange の送信直後にフックする。
     *
     * @param fromSlot    変更前スロット
     * @param toSlot      変更後スロット
     * @param hotbarCopy  変更前のホットバーコピー（null可）
     */
    public void onSwap(int fromSlot, int toSlot, ItemStack[] hotbarCopy) {
        // 同じスロットへの重複スワップは記録しない（Sn0w の serverSlot チェック相当）
        if (fromSlot == toSlot) return;

        evict(); // 古いエントリを先に削除
        history.add(new SwapEntry(fromSlot, toSlot, hotbarCopy, System.currentTimeMillis()));
        serverSlot = toSlot;
    }

    /**
     * 最後のスワップから delayMs ミリ秒経過しているかを返す。
     *
     * @param delayMs 必要な待機時間（autocrystal の swapDelay 設定値 * 50L など）
     * @return true = 十分待った → place/break 続行可
     *         false = まだ待つべき → place/break をブロック
     */
    public boolean hasPassedDelay(long delayMs) {
        if (history.isEmpty()) return true;

        SwapEntry last = history.get(history.size() - 1);
        return System.currentTimeMillis() - last.timestamp >= delayMs;
    }

    /**
     * サーバー側スロット番号を返す（UpdateSelectedSlot の重複送信防止に使う）。
     */
    public int getServerSlot() {
        return serverSlot;
    }

    /**
     * 最新スワップエントリを返す（null の場合はスワップ履歴なし）。
     */
    public SwapEntry getLastEntry() {
        evict();
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }

    /**
     * すべての履歴をクリア（onDisable など）。
     */
    public void clear() {
        history.clear();
        serverSlot = -1;
    }

    // ================================================================
    // Internal
    // ================================================================

    /** CLEAR_TIMEOUT_MS を超えた古いエントリを削除 */
    private void evict() {
        long now = System.currentTimeMillis();
        for (Iterator<SwapEntry> it = history.iterator(); it.hasNext();) {
            SwapEntry e = it.next();
            if (now - e.timestamp > CLEAR_TIMEOUT_MS) {
                history.remove(e); // CopyOnWriteArrayList は remove(Object) が安全
            }
        }
    }

    // ================================================================
    // Inner class
    // ================================================================

    /**
     * 1回のスワップ操作を表すイミュータブルな記録。
     */
    public static final class SwapEntry {
        public final int        fromSlot;
        public final int        toSlot;
        public final ItemStack[] preHotbar; // null の場合はコピーなし
        public final long       timestamp;

        SwapEntry(int fromSlot, int toSlot, ItemStack[] preHotbar, long ts) {
            this.fromSlot  = fromSlot;
            this.toSlot    = toSlot;
            this.preHotbar = preHotbar;
            this.timestamp = ts;
        }
    }
}
