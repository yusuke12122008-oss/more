// 新規ファイル
// src/main/java/me/earth/earthhack/impl/modules/combat/autocrystal/CrystalPositionCache.java
package me.earth.earthhack.impl.modules.combat.autocrystal;

import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 爆発・攻撃済みクリスタルのBlockPosをタイムスタンプ付きで保持し、
 * 一定時間後に自動クリーンアップするキャッシュ。
 *
 * Sn0w/CrystalManager の crystalBoxes(600ms) と
 * polloshook の pendingPlacePositions(pingTimeout=250ms) を
 * 1.12.2 ConcurrentHashMap で統合した実装。
 *
 * 使い方:
 *   - place() 後 → pendingPlace(crystalPos) を呼ぶ
 *   - SPacketSpawnObject 受信 → confirmPlace(crystalPos) を呼ぶ
 *   - SPacketDestroyEntities 受信 → markExploded(crystalPos) を呼ぶ
 *   - HelperPlace.selfCalc() 内 → isBlocked(pos) でチェック
 */
public final class CrystalPositionCache {

    // ---- 定数 ----
    /** 未確認Pending の最大保持時間 (ms) — polloshook の pingTimeout 相当 */
    private static final long PENDING_TIMEOUT_MS  = 250L;
    /** 爆発済みBlockPos の最大保持時間 (ms) — Sn0w の 600ms 相当 */
    private static final long EXPLODE_TIMEOUT_MS  = 600L;

    // ---- 内部状態 (BlockPos -> timestamp) ----

    /**
     * プレース送信済みだがスポーンパケット未受信のBlockPos。
     * キー: 設置先BlockPos（crystalPos = pos.up() のブロック位置）
     * 値:   送信したシステム時刻 (ms)
     */
    private final ConcurrentHashMap<BlockPos, Long> pending   = new ConcurrentHashMap<>();

    /**
     * スポーンパケット受信済みで攻撃対象として確認されたBlockPos。
     * キー: 上記と同じ基準位置
     * 値:   スポーン受信時刻 (ms)
     */
    private final ConcurrentHashMap<BlockPos, Long> confirmed = new ConcurrentHashMap<>();

    /**
     * 爆発済みBlockPos。この位置への二重配置を防ぐ。
     * キー: 爆発したクリスタルの床BlockPos
     * 値:   爆発検出時刻 (ms)
     */
    private final ConcurrentHashMap<BlockPos, Long> exploded  = new ConcurrentHashMap<>();

    // ================================================================
    // Public API
    // ================================================================

    /**
     * プレースパケット送信直後に呼ぶ。
     * @param crystalBlockPos クリスタルが立つ BlockPos（obsidianの1段上）
     */
    public void pendingPlace(BlockPos crystalBlockPos) {
        pending.put(crystalBlockPos, System.currentTimeMillis());
    }

    /**
     * SPacketSpawnObject でクリスタルのスポーンを確認した際に呼ぶ。
     * pending から confirmed へ昇格させる。
     * @param crystalBlockPos 確認したBlockPos
     */
    public void confirmPlace(BlockPos crystalBlockPos) {
        Long t = pending.remove(crystalBlockPos);
        confirmed.put(crystalBlockPos, t != null ? t : System.currentTimeMillis());
    }

    /**
     * SPacketDestroyEntities / SPacketExplosion でクリスタル消滅を検知した際に呼ぶ。
     * @param crystalBlockPos 消えたクリスタルが立っていたBlockPos
     */
    public void markExploded(BlockPos crystalBlockPos) {
        pending.remove(crystalBlockPos);
        confirmed.remove(crystalBlockPos);
        exploded.put(crystalBlockPos, System.currentTimeMillis());
    }

    /**
     * すべての状態をリセット（onDisable 時など）。
     */
    public void clear() {
        pending.clear();
        confirmed.clear();
        exploded.clear();
    }

    /**
     * 指定BlockPosがブロックされているかを返す。
     * HelperPlace の selfCalc 先頭で blackList チェックと同列に使う。
     *
     * @param pos チェック対象の床BlockPos
     * @return true = この位置には現在配置すべきでない
     */
    public boolean isBlocked(BlockPos pos) {
        expire(); // 呼び出し毎に期限切れを掃除
        return exploded.containsKey(pos)
            || pending.containsKey(pos)
            || confirmed.containsKey(pos);
    }

    /**
     * pending に登録済みか（SPacketSpawnObject 受信判定で使う）。
     */
    public boolean isPending(BlockPos pos) {
        return pending.containsKey(pos);
    }

    /**
     * confirmed に登録済みか（攻撃済み判定で使う）。
     */
    public boolean isConfirmed(BlockPos pos) {
        return confirmed.containsKey(pos);
    }

    // ================================================================
    // Internal: expire 古いエントリを自動削除
    // ================================================================

    /**
     * タイムアウト超過エントリをすべて削除。
     * isBlocked() から毎回呼ばれるため ConcurrentHashMap で安全。
     */
    private void expire() {
        long now = System.currentTimeMillis();

        for (Iterator<Map.Entry<BlockPos, Long>> it =
                 pending.entrySet().iterator(); it.hasNext();) {
            if (now - it.next().getValue() > PENDING_TIMEOUT_MS) {
                it.remove();
            }
        }

        for (Iterator<Map.Entry<BlockPos, Long>> it =
                 confirmed.entrySet().iterator(); it.hasNext();) {
            // confirmed は攻撃が通れば markExploded で消えるので少し長めに保持
            if (now - it.next().getValue() > PENDING_TIMEOUT_MS * 2) {
                it.remove();
            }
        }

        for (Iterator<Map.Entry<BlockPos, Long>> it =
                 exploded.entrySet().iterator(); it.hasNext();) {
            if (now - it.next().getValue() > EXPLODE_TIMEOUT_MS) {
                it.remove();
            }
        }
    }
}
