// 新規ファイル
// src/main/java/me/earth/earthhack/impl/modules/combat/autocrystal/AttackRTTTracker.java
package me.earth.earthhack.impl.modules.combat.autocrystal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * クリスタル攻撃の往復時間（RTT）を実測して動的 attackDelay を計算するトラッカー。
 *
 * Cosmos の attackTimes[10] + getAverageWaitTime() + lastAttackTime を
 * 1.12.2 向けに再実装。
 *
 * 仕組み:
 *  1. attack送信時に onAttackSent(entityId) → 送信時刻を記録
 *  2. SPacketDestroyEntities 受信時に onCrystalDestroyed(entityId) → RTT 計算
 *  3. getAverageRTT() で直近 HISTORY_SIZE 回の平均を返す
 *  4. AbstractCalculation.breakCheck() で breakTimer の判定に使う
 *
 * 使い方例 (AbstractCalculation内):
 *   long dynamicDelay = module.await.getValue()
 *       ? (long)(module.rttTracker.getAverageRTT()
 *              + module.yieldProtection.getValue() * 50L)
 *       : module.breakDelay.getValue();
 *   if (module.breakTimer.passed(dynamicDelay)) { ... }
 */
public final class AttackRTTTracker {

    /** 保持するRTTサンプル数 — Cosmos の attackTimes[10] に対応 */
    private static final int  HISTORY_SIZE  = 10;
    /** 送信記録のタイムアウト — これ以上経ってもDestroyが来なければ破棄 */
    private static final long SENT_TIMEOUT  = 5000L;

    /** entityId → 攻撃送信時刻 */
    private final ConcurrentHashMap<Integer, Long> sentTimes   = new ConcurrentHashMap<>();

    /** 直近 HISTORY_SIZE 件の RTT (ms) を循環バッファに保存 */
    private final long[] rttBuffer    = new long[HISTORY_SIZE];
    private       int    writeIndex   = 0;
    private       int    sampleCount  = 0;

    /** 最後の攻撃送信時刻（Cosmos の lastAttackTime 相当）*/
    private volatile long lastSentTime  = 0L;
    /** 最後の確認時刻（Cosmos の lastConfirmTime 相当）*/
    private volatile long lastConfirmTime = 0L;

    // ================================================================
    // Public API
    // ================================================================

    /**
     * CPacketUseEntity（クリスタル攻撃）送信直後に呼ぶ。
     * @param entityId 攻撃したクリスタルのエンティティID
     */
    public void onAttackSent(int entityId) {
        long now = System.currentTimeMillis();
        sentTimes.put(entityId, now);
        lastSentTime = now;

        // タイムアウト済みの古いエントリを掃除
        evictSentTimes(now);
    }

    /**
     * SPacketDestroyEntities でクリスタルの消滅を受信した際に呼ぶ。
     * RTT を計算してバッファに追記する。
     *
     * @param entityId 消滅したエンティティID
     */
    public void onCrystalDestroyed(int entityId) {
        Long sentAt = sentTimes.remove(entityId);
        if (sentAt == null) return; // 自分が攻撃したものでなければ無視

        long rtt = System.currentTimeMillis() - sentAt;
        // 異常値（サーバー再接続等）を除外
        if (rtt < 0 || rtt > 3000L) return;

        lastConfirmTime = System.currentTimeMillis();
        appendRTT(rtt);
    }

    /**
     * 直近サンプルの平均RTT (ms) を返す。
     * サンプルが1件もない場合は 0 を返す（呼び出し元でデフォルト値を使うこと）。
     */
    public long getAverageRTT() {
        if (sampleCount == 0) return 0L;

        int count = Math.min(sampleCount, HISTORY_SIZE);
        long sum  = 0L;
        for (int i = 0; i < count; i++) {
            sum += rttBuffer[i];
        }
        return sum / count;
    }

    /**
     * 最後に攻撃が確認された時刻 (ms) を返す。
     * Cosmos の lastConfirmTime 相当。
     */
    public long getLastConfirmTime() {
        return lastConfirmTime;
    }

    /**
     * サンプルがあるかどうか。
     * ない場合はデフォルトdelayにフォールバックすべき。
     */
    public boolean hasSamples() {
        return sampleCount > 0;
    }

    /**
     * すべての状態をリセット（onDisable 時など）。
     */
    public void clear() {
        sentTimes.clear();
        java.util.Arrays.fill(rttBuffer, 0L);
        writeIndex   = 0;
        sampleCount  = 0;
        lastSentTime = 0L;
        lastConfirmTime = 0L;
    }

    // ================================================================
    // Internal
    // ================================================================

    /** 循環バッファにRTTを1件追記 */
    private void appendRTT(long rtt) {
        rttBuffer[writeIndex] = rtt;
        writeIndex = (writeIndex + 1) % HISTORY_SIZE;
        if (sampleCount < HISTORY_SIZE) sampleCount++;
    }

    /** SENT_TIMEOUT を超えた送信記録を破棄 */
    private void evictSentTimes(long now) {
        for (Iterator<Map.Entry<Integer, Long>> it =
                 sentTimes.entrySet().iterator(); it.hasNext();) {
            if (now - it.next().getValue() > SENT_TIMEOUT) {
                it.remove();
            }
        }
    }
}
