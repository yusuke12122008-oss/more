package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Complexity;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.*;
import me.earth.earthhack.api.util.bind.Bind;
import me.earth.earthhack.impl.gui.visibility.PageBuilder;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.pingbypass.PingBypassModule;
import me.earth.earthhack.impl.modules.client.settings.SettingsModule;
import me.earth.earthhack.impl.modules.combat.autocrystal.helpers.*;
import me.earth.earthhack.impl.modules.combat.autocrystal.helpers.FakeCrystalRender;
import me.earth.earthhack.impl.modules.combat.autocrystal.helpers.ServerTimeHelper;
import me.earth.earthhack.impl.modules.combat.autocrystal.helpers.ThreadHelper;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.*;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.CrystalTimeStamp;
import me.earth.earthhack.impl.modules.combat.autocrystal.util.RotationFunction;
import me.earth.earthhack.impl.util.helpers.blocks.modes.PlaceSwing;
import me.earth.earthhack.impl.util.helpers.blocks.modes.RayTraceMode;
import me.earth.earthhack.impl.util.helpers.blocks.modes.Rotate;
import me.earth.earthhack.impl.util.math.DiscreteTimer;
import me.earth.earthhack.impl.util.math.GuardTimer;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.StopWatch;
import me.earth.earthhack.impl.util.math.geocache.Sphere;
import me.earth.earthhack.impl.util.math.position.PositionUtil;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.MovementUtil;
import me.earth.earthhack.impl.util.minecraft.CooldownBypass;
import me.earth.earthhack.impl.util.minecraft.blocks.BlockUtil;
import me.earth.earthhack.impl.util.minecraft.blocks.mine.MineUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.misc.collections.CollectionUtil;
import me.earth.earthhack.impl.util.network.ServerUtil;
import me.earth.earthhack.impl.util.text.TextColor;
import me.earth.earthhack.impl.util.thread.SafeRunnable;
import me.earth.earthhack.impl.util.thread.ThreadUtil;
import me.earth.earthhack.pingbypass.PingBypass;
import me.earth.earthhack.pingbypass.input.Mouse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoCrystal extends Module
{
    public static final PositionHistoryHelper POSITION_HISTORY =
        new PositionHistoryHelper();

    static {
        Bus.EVENT_BUS.subscribe(POSITION_HISTORY);
    }

    private static final ScheduledExecutorService EXECUTOR =
            ThreadUtil.newDaemonScheduledExecutor("AutoCrystal");
    private static final ModuleCache<PingBypassModule> PINGBYPASS =
            Caches.getModule(PingBypassModule.class);

    private static final AtomicBoolean ATOMIC_STARTED = new AtomicBoolean();
    private static boolean started;

    // =====================================================================
    // [NEW] トラッカー / キャッシュ インスタンス
    // =====================================================================
    /** クリスタル配置位置の pending/confirmed/exploded キャッシュ */
    public final CrystalPositionCache  positionCache    = new CrystalPositionCache();
    /** スロット変更のタイムスタンプ履歴 */
    public final SwapStateTracker      swapStateTracker = new SwapStateTracker();
    /** 攻撃パケット〜破壊確認の往復時間トラッカー */
    public final AttackRTTTracker      rttTracker       = new AttackRTTTracker();

    /* ---------------- Page -------------- */
    protected final Setting<ACPages> pages =
            register(new EnumSetting<>("Page", ACPages.Place))
                .setComplexity(Complexity.Medium);

    /* ---------------- Place Settings -------------- */
    protected final Setting<Boolean> place =
            register(new BooleanSetting("Place", true));
    protected final Setting<Target> targetMode =
            register(new EnumSetting<>("Target", Target.Closest))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> placeRange =
            register(new NumberSetting<>("PlaceRange", 6.0f, 0.0f, 6.0f));
    protected final Setting<Float> placeTrace =
            register(new NumberSetting<>("PlaceTrace", 6.0f, 0.0f, 6.0f))
                .setComplexity(Complexity.Expert);
    public final Setting<Float> minDamage =
            register(new NumberSetting<>("MinDamage", 6.0f, 0.1f, 20.0f));
    protected final Setting<Integer> placeDelay =
            register(new NumberSetting<>("PlaceDelay", 25, 0, 500));
    protected final Setting<Float> maxSelfPlace =
            register(new NumberSetting<>("MaxSelfPlace", 9.0f, 0.0f, 20.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> multiPlace =
            register(new NumberSetting<>("MultiPlace", 1, 1, 5))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> slowPlaceDmg =
            register(new NumberSetting<>("SlowPlace", 4.0f, 0.1f, 20.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> slowPlaceDelay =
            register(new NumberSetting<>("SlowPlaceDelay", 500, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> override =
            register(new BooleanSetting("OverridePlace", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> newVer =
            register(new BooleanSetting("1.13+", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> newVerEntities =
            register(new BooleanSetting("1.13-Entities", false))
                .setComplexity(Complexity.Medium);
    public final Setting<SwingTime> placeSwing =
            register(new EnumSetting<>("PlaceSwing", SwingTime.Post))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> smartTrace =
            register(new BooleanSetting("Smart-Trace", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> placeRangeEyes =
            register(new BooleanSetting("PlaceRangeEyes", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> placeRangeCenter =
            register(new BooleanSetting("PlaceRangeCenter", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> traceWidth =
            register(new NumberSetting<>("TraceWidth", -1.0, -1.0, 1.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> fallbackTrace =
            register(new BooleanSetting("Fallback-Trace", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> rayTraceBypass =
            register(new BooleanSetting("RayTraceBypass", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> forceBypass =
            register(new BooleanSetting("ForceBypass", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> rayBypassFacePlace =
            register(new BooleanSetting("RayBypassFacePlace", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> rayBypassFallback =
            register(new BooleanSetting("RayBypassFallback", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> bypassTicks =
            register(new NumberSetting<>("BypassTicks", 10, 0, 20))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> rbYaw =
            register(new NumberSetting<>("RB-Yaw", 180.0f, 0.0f, 180.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> rbPitch =
            register(new NumberSetting<>("RB-Pitch", 90.0f, 0.0f, 90.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> bypassRotationTime =
            register(new NumberSetting<>("RayBypassRotationTime", 500, 0, 1000))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> ignoreNonFull =
            register(new BooleanSetting("IgnoreNonFull", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> efficientPlacements =
            register(new BooleanSetting("EfficientPlacements", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> simulatePlace =
            register(new NumberSetting<>("Simulate-Place", 0, 0, 10))
                .setComplexity(Complexity.Medium);

    /* ---------------- Break Settings -------------- */
    protected final Setting<Attack> attackMode =
            register(new EnumSetting<>("Attack", Attack.Crystal));
    protected final Setting<Boolean> attack =
            register(new BooleanSetting("Break", true));
    protected final Setting<Float> breakRange =
            register(new NumberSetting<>("BreakRange", 6.0f, 0.0f, 6.0f));
    protected final Setting<Integer> breakDelay =
            register(new NumberSetting<>("BreakDelay", 25, 0, 500));
    protected final Setting<Float> breakTrace =
            register(new NumberSetting<>("BreakTrace", 3.0f, 0.0f, 6.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> minBreakDamage =
            register(new NumberSetting<>("MinBreakDmg", 0.5f, 0.0f, 20.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> maxSelfBreak =
            register(new NumberSetting<>("MaxSelfBreak", 10.0f, 0.0f, 20.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> slowBreakDamage =
            register(new NumberSetting<>("SlowBreak", 3.0f, 0.1f, 20.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> slowBreakDelay =
            register(new NumberSetting<>("SlowBreakDelay", 500, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> instant =
            register(new BooleanSetting("Instant", false));
    protected final Setting<Boolean> asyncCalc =
            register(new BooleanSetting("Async-Calc", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> alwaysCalc =
            register(new BooleanSetting("Always-Calc", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> ncpRange =
            register(new BooleanSetting("NCP-Range", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<SmartRange> placeBreakRange =
            register(new EnumSetting<>("SmartRange", SmartRange.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> smartTicks =
            register(new NumberSetting<>("SmartRange-Ticks", 0, 0, 20))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> negativeTicks =
            register(new NumberSetting<>("Negative-Ticks", 0, 0, 20))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> smartBreakTrace =
            register(new BooleanSetting("SmartBreakTrace", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> negativeBreakTrace =
            register(new BooleanSetting("NegativeBreakTrace", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> packets =
            register(new NumberSetting<>("Packets", 1, 1, 5))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> overrideBreak =
            register(new BooleanSetting("OverrideBreak", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<AntiWeakness> antiWeakness =
            register(new EnumSetting<>("AntiWeakness", AntiWeakness.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> instantAntiWeak =
            register(new BooleanSetting("AW-Instant", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> efficient =
            register(new BooleanSetting("Efficient", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> manually =
            register(new BooleanSetting("Manually", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> manualDelay =
            register(new NumberSetting<>("ManualDelay", 500, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<SwingTime> breakSwing =
            register(new EnumSetting<>("BreakSwing", SwingTime.Post))
                .setComplexity(Complexity.Expert);

    /* --------------- Rotations -------------- */
    protected final Setting<ACRotate> rotate =
            register(new EnumSetting<>("Rotate", ACRotate.None));
    protected final Setting<RotateMode> rotateMode =
            register(new EnumSetting<>("Rotate-Mode", RotateMode.Normal))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> smoothSpeed =
            register(new NumberSetting<>("Smooth-Speed", 0.5f, 0.1f, 2.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> endRotations =
            register(new NumberSetting<>("End-Rotations", 250, 0, 1000))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> angle =
            register(new NumberSetting<>("Break-Angle", 180.0f, 0.1f, 180.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> placeAngle =
            register(new NumberSetting<>("Place-Angle", 180.0f, 0.1f, 180.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> height =
            register(new NumberSetting<>("Height", 0.05f, 0.0f, 1.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> placeHeight =
            register(new NumberSetting<>("Place-Height", 1.0, 0.0, 1.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> rotationTicks =
            register(new NumberSetting<>("Rotations-Existed", 0, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> focusRotations =
            register(new BooleanSetting("Focus-Rotations", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> focusAngleCalc =
            register(new BooleanSetting("FocusRotationCompare", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> focusExponent =
            register(new NumberSetting<>("FocusExponent", 0.0, 0.0, 10.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> focusDiff =
            register(new NumberSetting<>("FocusDiff", 0.0, 0.0, 180.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> rotationExponent =
            register(new NumberSetting<>("RotationExponent", 0.0, 0.0, 10.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> minRotDiff =
            register(new NumberSetting<>("MinRotationDiff", 0.0, 0.0, 180.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> existed =
            register(new NumberSetting<>("Existed", 0, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> pingExisted =
            register(new BooleanSetting("Ping-Existed", false))
                .setComplexity(Complexity.Medium);
    /* ---------------- Misc Settings -------------- */
    protected final Setting<Float> targetRange =
            register(new NumberSetting<>("TargetRange", 20.0f, 0.1f, 20.0f));
    protected final Setting<Float> pbTrace =
            register(new NumberSetting<>("CombinedTrace", 3.0f, 0.0f, 6.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> range =
            register(new NumberSetting<>("Range", 12.0f, 0.1f, 20.0f));
    protected final Setting<Boolean> suicide =
            register(new BooleanSetting("Suicide", false));
    protected final Setting<Boolean> shield =
            register(new BooleanSetting("Shield", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> shieldCount =
            register(new NumberSetting<>("ShieldCount", 1, 1, 5))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> shieldMinDamage =
            register(new NumberSetting<>("ShieldMinDamage", 6.0f, 0.0f, 20.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> shieldSelfDamage =
            register(new NumberSetting<>("ShieldSelfDamage", 2.0f, 0.0f, 20.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> shieldDelay =
            register(new NumberSetting<>("ShieldPlaceDelay", 50, 0, 5000))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> shieldRange =
            register(new NumberSetting<>("ShieldRange", 10.0f, 0.0f, 20.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> shieldPrioritizeHealth =
            register(new BooleanSetting("Shield-PrioritizeHealth", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> multiTask =
            register(new BooleanSetting("MultiTask", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> multiPlaceCalc =
            register(new BooleanSetting("MultiPlace-Calc", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> multiPlaceMinDmg =
            register(new BooleanSetting("MultiPlace-MinDmg", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> countDeadCrystals =
            register(new BooleanSetting("CountDeadCrystals", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> countDeathTime =
            register(new BooleanSetting("CountWithinDeathTime", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> yCalc =
            register(new BooleanSetting("Y-Calc", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> dangerSpeed =
            register(new BooleanSetting("Danger-Speed", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> dangerHealth =
            register(new NumberSetting<>("Danger-Health", 0.0f, 0.0f, 36.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> cooldown =
            register(new NumberSetting<>("CoolDown", 500, 0, 10_000));
    protected final Setting<Integer> placeCoolDown =
            register(new NumberSetting<>("PlaceCooldown", 0, 0, 10_000))
                .setComplexity(Complexity.Medium);
    protected final Setting<AntiFriendPop> antiFriendPop =
            register(new EnumSetting<>("AntiFriendPop", AntiFriendPop.None));
    protected final Setting<Boolean> antiFeetPlace =
            register(new BooleanSetting("AntiFeetPlace", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> feetBuffer =
            register(new NumberSetting<>("FeetBuffer", 5, 0, 50))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> stopWhenEating =
            register(new BooleanSetting("StopWhenEating", false));
    protected final Setting<Boolean> stopWhenMining =
            register(new BooleanSetting("StopWhenMining", false));
    protected final Setting<Boolean> dangerFacePlace =
            register(new BooleanSetting("Danger-FacePlace", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> motionCalc =
            register(new BooleanSetting("Motion-Calc", false))
                .setComplexity(Complexity.Expert);

    /* ---------------- FacePlace and ArmorPlace -------------- */
    protected final Setting<Boolean> holdFacePlace =
            register(new BooleanSetting("HoldFacePlace", false));
    protected final Setting<Float> facePlace =
            register(new NumberSetting<>("FacePlace", 10.0f, 0.0f, 36.0f));
    protected final Setting<Float> minFaceDmg =
            register(new NumberSetting<>("Min-FP", 2.0f, 0.0f, 5.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> armorPlace =
            register(new NumberSetting<>("ArmorPlace", 5.0f, 0.0f, 100.0f))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> pickAxeHold =
            register(new BooleanSetting("PickAxe-Hold", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> antiNaked =
            register(new BooleanSetting("AntiNaked", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> fallBack =
            register(new BooleanSetting("FallBack", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> fallBackDiff =
            register(new NumberSetting<>("Fallback-Difference", 10.0f, 0.0f, 16.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> fallBackDmg =
            register(new NumberSetting<>("FallBackDmg", 3.0f, 0.0f, 6.0f))
                .setComplexity(Complexity.Expert);

    /* ---------------- Switch, Swing and PingBypass -------------- */
    protected final Setting<AutoSwitch> autoSwitch =
            register(new EnumSetting<>("AutoSwitch", AutoSwitch.Bind));
    protected final Setting<Boolean> mainHand =
            register(new BooleanSetting("MainHand", false));
    protected final Setting<Bind> switchBind =
            register(new BindSetting("SwitchBind", Bind.none()));
    protected final Setting<Boolean> switchBack =
            register(new BooleanSetting("SwitchBack", true));
    protected final Setting<Boolean> useAsOffhand =
            register(new BooleanSetting("UseAsOffHandBind", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> instantOffhand =
            register(new BooleanSetting("Instant-Offhand", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> pingBypass =
            register(new BooleanSetting("Old-PingBypass", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> switchMessage =
            register(new BooleanSetting("Switch-Message", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<SwingType> swing =
            register(new EnumSetting<>("BreakHand", SwingType.MainHand))
                .setComplexity(Complexity.Expert);
    protected final Setting<SwingType> placeHand =
            register(new EnumSetting<>("PlaceHand", SwingType.MainHand))
                .setComplexity(Complexity.Expert);
    protected final Setting<CooldownBypass> cooldownBypass =
            register(new EnumSetting<>("CooldownBypass", CooldownBypass.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<CooldownBypass> obsidianBypass =
            register(new EnumSetting<>("ObsidianBypass", CooldownBypass.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<CooldownBypass> antiWeaknessBypass =
            register(new EnumSetting<>("AntiWeaknessBypass", CooldownBypass.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<CooldownBypass> mineBypass =
            register(new EnumSetting<>("MineBypass", CooldownBypass.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<SwingType> obbyHand =
            register(new EnumSetting<>("ObbyHand", SwingType.MainHand))
                .setComplexity(Complexity.Expert);

    /* ---------------- Render Settings -------------- */
    protected final Setting<Boolean> render =
            register(new BooleanSetting("Render", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> renderTime =
            register(new NumberSetting<>("Render-Time", 600, 0, 5000))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> box =
            register(new BooleanSetting("Draw-Box", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Color> boxColor =
            register(new ColorSetting("Box", new Color(255, 255, 255, 120)));
    protected final Setting<Color> outLine =
            register(new ColorSetting("Outline", new Color(255, 255, 255, 240)));
    protected final Setting<Color> indicatorColor =
            register(new ColorSetting("IndicatorColor", new Color(190, 5, 5, 255)))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> fade =
            register(new BooleanSetting("Fade", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> fadeComp =
            register(new BooleanSetting("Fade-Compatibility", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> fadeTime =
            register(new NumberSetting<>("Fade-Time", 1000, 0, 5000))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> realtime =
            register(new BooleanSetting("Realtime", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> slide =
            register(new BooleanSetting("Slide", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> smoothSlide =
            register(new BooleanSetting("SmoothenSlide", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Double> slideTime =
            register(new NumberSetting<>("Slide-Time", 250.0, 1.0, 1000.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> zoom =
            register(new BooleanSetting("Zoom", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Double> zoomTime =
            register(new NumberSetting<>("Zoom-Time", 100.0, 1.0, 1000.0))
                .setComplexity(Complexity.Medium);
    protected final Setting<Double> zoomOffset =
            register(new NumberSetting<>("Zoom-Offset", -0.5, -1.0, 1.0))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> multiZoom =
            register(new BooleanSetting("Multi-Zoom", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> renderExtrapolation =
            register(new BooleanSetting("RenderExtrapolation", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<RenderDamagePos> renderDamage =
            register(new EnumSetting<>("DamageRender", RenderDamagePos.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<RenderDamage> renderMode =
            register(new EnumSetting<>("DamageMode", RenderDamage.Normal))
                .setComplexity(Complexity.Medium);

    /* ---------------- ArrayList Formatting -------------- */
    protected final Setting<Boolean> arrayInfo =
            register(new BooleanSetting("ArrayInfo", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> showTarget =
            register(new BooleanSetting("ShowTarget", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> showDelay =
            register(new BooleanSetting("ShowDelay", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> showSpeed =
            register(new BooleanSetting("ShowSpeed", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> showCPS =
            register(new BooleanSetting("ShowCPS", true))
                .setComplexity(Complexity.Medium);

    /* ---------------- SetDead Settings -------------- */
    protected final Setting<Boolean> setDead =
            register(new BooleanSetting("SetDead", false));
    protected final Setting<Boolean> instantSetDead =
            register(new BooleanSetting("Instant-Dead", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> pseudoSetDead =
            register(new BooleanSetting("Pseudo-Dead", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> simulateExplosion =
            register(new BooleanSetting("SimulateExplosion", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> soundRemove =
            register(new BooleanSetting("SoundRemove", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> useSafeDeathTime =
            register(new BooleanSetting("UseSafeDeathTime", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> safeDeathTime =
            register(new NumberSetting<>("Safe-Death-Time", 0, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> deathTime =
            register(new NumberSetting<>("Death-Time", 0, 0, 500))
                .setComplexity(Complexity.Medium);

    /* ---------------- Obsidian Settings -------------- */
    protected final Setting<Boolean> obsidian =
            register(new BooleanSetting("Obsidian", false));
    protected final Setting<Boolean> basePlaceOnly =
            register(new BooleanSetting("BasePlaceOnly", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> obbySwitch =
            register(new BooleanSetting("Obby-Switch", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> obbyDelay =
            register(new NumberSetting<>("ObbyDelay", 500, 0, 5000))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> obbyCalc =
            register(new NumberSetting<>("ObbyCalc", 500, 0, 5000))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> helpingBlocks =
            register(new NumberSetting<>("HelpingBlocks", 1, 0, 5))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> obbyMinDmg =
            register(new NumberSetting<>("Obby-MinDamage", 7.0f, 0.1f, 36.0f));
    protected final Setting<Boolean> terrainCalc =
            register(new BooleanSetting("TerrainCalc", true));
    protected final Setting<Boolean> obbySafety =
            register(new BooleanSetting("ObbySafety", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<RayTraceMode> obbyTrace =
            register(new EnumSetting<>("Obby-Raytrace", RayTraceMode.Fast))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> obbyTerrain =
            register(new BooleanSetting("Obby-Terrain", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> obbyPreSelf =
            register(new BooleanSetting("Obby-PreSelf", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> fastObby =
            register(new NumberSetting<>("Fast-Obby", 0, 0, 3))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> maxDiff =
            register(new NumberSetting<>("Max-Difference", 1, 0, 5))
                .setComplexity(Complexity.Expert);
    protected final Setting<Double> maxDmgDiff =
            register(new NumberSetting<>("Max-DamageDiff", 0.0, 0.0, 10.0))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> setState =
            register(new BooleanSetting("Client-Blocks", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<PlaceSwing> obbySwing =
            register(new EnumSetting<>("Obby-Swing", PlaceSwing.Once))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> obbyFallback =
            register(new BooleanSetting("Obby-Fallback", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Rotate> obbyRotate =
            register(new EnumSetting<>("Obby-Rotate", Rotate.None));

    /* ---------------- Liquids Settings -------------- */
    protected final Setting<Boolean> interact =
            register(new BooleanSetting("Interact", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> inside =
            register(new BooleanSetting("Inside", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> lava =
            register(new BooleanSetting("Lava", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> water =
            register(new BooleanSetting("Water", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> liquidObby =
            register(new BooleanSetting("LiquidObby", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> liquidRayTrace =
            register(new BooleanSetting("LiquidRayTrace", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> liqDelay =
            register(new NumberSetting<>("LiquidDelay", 500, 0, 1000))
                .setComplexity(Complexity.Medium);
    protected final Setting<Rotate> liqRotate =
            register(new EnumSetting<>("LiquidRotate", Rotate.None))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> pickaxeOnly =
            register(new BooleanSetting("PickaxeOnly", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> interruptSpeedmine =
            register(new BooleanSetting("InterruptSpeedmine", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> setAir =
            register(new BooleanSetting("SetAir", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> absorb =
            register(new BooleanSetting("Absorb", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> requireOnGround =
            register(new BooleanSetting("RequireOnGround", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> ignoreLavaItems =
            register(new BooleanSetting("IgnoreLavaItems", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> sponges =
            register(new BooleanSetting("Sponges", false))
                .setComplexity(Complexity.Expert);

    /* ---------------- AntiTotem Settings -------------- */
    protected final Setting<Boolean> antiTotem =
            register(new BooleanSetting("AntiTotem", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Float> totemHealth =
            register(new NumberSetting<>("Totem-Health", 1.5f, 0.0f, 10.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> minTotemOffset =
            register(new NumberSetting<>("Min-Offset", 0.5f, 0.0f, 5.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> maxTotemOffset =
            register(new NumberSetting<>("Max-Offset", 2.0f, 0.0f, 5.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> popDamage =
            register(new NumberSetting<>("Pop-Damage", 12.0f, 10.0f, 20.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> totemSync =
            register(new BooleanSetting("TotemSync", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> forceAntiTotem =
            register(new BooleanSetting("Force-AntiTotem", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> forceSlow =
            register(new BooleanSetting("Force-Slow", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> syncForce =
            register(new BooleanSetting("Sync-Force", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> dangerForce =
            register(new BooleanSetting("Danger-Force", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> forcePlaceConfirm =
            register(new NumberSetting<>("Force-Place", 100, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> forceBreakConfirm =
            register(new NumberSetting<>("Force-Break", 100, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> attempts =
            register(new NumberSetting<>("Attempts", 500, 0, 10000))
                .setComplexity(Complexity.Expert);

    /* ---------------- Damage Sync -------------- */
    protected final Setting<Boolean> damageSync =
            register(new BooleanSetting("DamageSync", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> preSynCheck =
            register(new BooleanSetting("Pre-SyncCheck", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> discreteSync =
            register(new BooleanSetting("Discrete-Sync", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> dangerSync =
            register(new BooleanSetting("Danger-Sync", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> placeConfirm =
            register(new NumberSetting<>("Place-Confirm", 250, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> breakConfirm =
            register(new NumberSetting<>("Break-Confirm", 250, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> syncDelay =
            register(new NumberSetting<>("SyncDelay", 500, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> surroundSync =
            register(new BooleanSetting("SurroundSync", true))
                .setComplexity(Complexity.Expert);

    /* ---------------- Extrapolation Settings -------------- */
    public final Setting<Integer> extrapol =
            register(new NumberSetting<>("Extrapolation", 0, 0, 50))
                .setComplexity(Complexity.Medium);
    public final Setting<Integer> bExtrapol =
            register(new NumberSetting<>("Break-Extrapolation", 0, 0, 50))
                .setComplexity(Complexity.Medium);
    public final Setting<Integer> blockExtrapol =
            register(new NumberSetting<>("Block-Extrapolation", 0, 0, 50))
                .setComplexity(Complexity.Medium);
    public final Setting<BlockExtrapolationMode> blockExtraMode =
            register(new EnumSetting<>("BlockExtraMode",
                                       BlockExtrapolationMode.Pessimistic))
                .setComplexity(Complexity.Expert);
    public final Setting<Boolean> doubleExtraCheck =
            register(new BooleanSetting("DoubleExtraCheck", true))
                .setComplexity(Complexity.Expert);
    public final Setting<Boolean> avgPlaceDamage =
            register(new BooleanSetting("AvgPlaceExtra", false))
                .setComplexity(Complexity.Expert);
    public final Setting<Double> placeExtraWeight =
            register(new NumberSetting<>("P-Extra-Weight", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Medium);
    public final Setting<Double> placeNormalWeight =
            register(new NumberSetting<>("P-Norm-Weight", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Medium);
    public final Setting<Boolean> avgBreakExtra =
            register(new BooleanSetting("AvgBreakExtra", false))
                .setComplexity(Complexity.Expert);
    public final Setting<Double> breakExtraWeight =
            register(new NumberSetting<>("B-Extra-Weight", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Medium);
    public final Setting<Double> breakNormalWeight =
            register(new NumberSetting<>("B-Norm-Weight", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Medium);
    public final Setting<Boolean> gravityExtrapolation =
            register(new BooleanSetting("Extra-Gravity", true))
                .setComplexity(Complexity.Expert);
    public final Setting<Double> gravityFactor =
            register(new NumberSetting<>("Gravity-Factor", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Expert);
    public final Setting<Double> yPlusFactor =
            register(new NumberSetting<>("Y-Plus-Factor", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Expert);
    public final Setting<Double> yMinusFactor =
            register(new NumberSetting<>("Y-Minus-Factor", 1.0, 0.0, 5.0))
                .setComplexity(Complexity.Expert);
    public final Setting<Boolean> selfExtrapolation =
            register(new BooleanSetting("SelfExtrapolation", false))
                .setComplexity(Complexity.Medium);

    /* ---------------- Predict Settings -------------- */
    protected final Setting<Boolean> idPredict =
            register(new BooleanSetting("ID-Predict", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> idOffset =
            register(new NumberSetting<>("ID-Offset", 1, 1, 10))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> idDelay =
            register(new NumberSetting<>("ID-Delay", 0, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> idPackets =
            register(new NumberSetting<>("ID-Packets", 1, 1, 10))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> godAntiTotem =
            register(new BooleanSetting("God-AntiTotem", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> holdingCheck =
            register(new BooleanSetting("Holding-Check", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> toolCheck =
            register(new BooleanSetting("Tool-Check", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<PlaceSwing> godSwing =
            register(new EnumSetting<>("God-Swing", PlaceSwing.Once))
                .setComplexity(Complexity.Expert);

    /* ---------------- Efficiency / Pre-Calc -------------- */
    protected final Setting<PreCalc> preCalc =
            register(new EnumSetting<>("Pre-Calc", PreCalc.None))
                .setComplexity(Complexity.Expert);
    protected final Setting<ExtrapolationType> preCalcExtra =
            register(new EnumSetting<>("PreCalcExtra", ExtrapolationType.Place))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> preCalcDamage =
            register(new NumberSetting<>("Pre-CalcDamage", 15.0f, 0.0f, 36.0f))
                .setComplexity(Complexity.Expert);

    /* ---------------- MultiThread Settings -------------- */
    public final Setting<Boolean> multiThread =
            register(new BooleanSetting("MultiThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<RotationThread> rotationThread =
            register(new EnumSetting<>("RotationThread", RotationThread.Predict))
                .setComplexity(Complexity.Expert);
    protected final Setting<Float> partial =
            register(new NumberSetting<>("Partial", 0.0f, 0.0f, 1.0f))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> maxCancel =
            register(new NumberSetting<>("MaxCancel", 200, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> timeOut =
            register(new NumberSetting<>("TimeOut", 50, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> threadDelay =
            register(new NumberSetting<>("ThreadDelay", 0, 0, 500))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> pullBasedDelay =
            register(new NumberSetting<>("PullBasedDelay", 0, 0, 500))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> blockDestroyThread =
            register(new BooleanSetting("BlockDestroyThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> explosionThread =
            register(new BooleanSetting("ExplosionThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> soundThread =
            register(new BooleanSetting("SoundThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> entityThread =
            register(new BooleanSetting("EntityThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> gameloop =
            register(new BooleanSetting("GameLoop", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> spawnThread =
            register(new BooleanSetting("SpawnThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> destroyThread =
            register(new BooleanSetting("DestroyThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> serverThread =
            register(new BooleanSetting("ServerThread", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> asyncServerThread =
            register(new BooleanSetting("AsyncServerThread", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> earlyFeetThread =
            register(new BooleanSetting("EarlyFeetThread", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> lateBreakThread =
            register(new BooleanSetting("LateBreakThread", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> motionThread =
            register(new BooleanSetting("MotionThread", true))
                .setComplexity(Complexity.Medium);
    protected final Setting<Boolean> blockChangeThread =
            register(new BooleanSetting("BlockChangeThread", false))
                .setComplexity(Complexity.Medium);
    protected final Setting<Integer> priority =
            register(new NumberSetting<>("Priority", 0, -10, 10))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> smartPost =
            register(new BooleanSetting("SmartPost", true))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> clearPost =
            register(new BooleanSetting("ClearPost", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> spectator =
            register(new BooleanSetting("Spectator", false))
                .setComplexity(Complexity.Expert);

    /* ---------------- Server-Thread / ServerTime Settings -------------- */
    protected final Setting<Boolean> mainThreadThreads =
            register(new BooleanSetting("MainThreadThreads", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> tickThreshold =
            register(new NumberSetting<>("TickThreshold", 0, 0, 50))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> preSpawn =
            register(new NumberSetting<>("PreSpawn", 0, 0, 50))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> maxEarlyThread =
            register(new NumberSetting<>("MaxEarlyThread", 10, 0, 50))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> spawnThreadWhenAttacked =
            register(new BooleanSetting("SpawnThreadWhenAttacked", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Boolean> alwaysBomb =
            register(new BooleanSetting("AlwaysBomb", false))
                .setComplexity(Complexity.Expert);
    protected final Setting<Integer> removeTime =
            register(new NumberSetting<>("RemoveTime", 2000, 0, 10000))
                .setComplexity(Complexity.Expert);

    // =====================================================================
    // [NEW] スワップ / RTT 関連設定
    // =====================================================================
    /** 直近攻撃の平均 RTT に基づいて break ディレイを動的調整する (Cosmos の await 相当) */
    public final Setting<Boolean> await =
            register(new BooleanSetting("Await", false))
                .setComplexity(Complexity.Medium);

    /** RTT ベースディレイへの追加バッファ (ms) */
    public final Setting<Integer> yieldProtection =
            register(new NumberSetting<>("YieldProtection", 20, 0, 200))
                .setComplexity(Complexity.Expert);

    /** スロット変更後にこの ms が経過するまで break/place を抑制する (Sn0w の swapTimer 相当) */
    public final Setting<Integer> swapDelay =
            register(new NumberSetting<>("SwapDelay", 0, 0, 500))
                .setComplexity(Complexity.Medium);

    /** true のとき swapDelay は place もブロックする (Full モード) */
    public final Setting<Boolean> swapWaitFull =
            register(new BooleanSetting("SwapWait-Full", false))
                .setComplexity(Complexity.Medium);
    // =====================================================================
    // インスタンスフィールド (タイマー / ヘルパー / 状態変数)
    // =====================================================================

    /* --- タイマー --- */
    public  final StopWatch     breakTimer      = new StopWatch();
    public  final StopWatch     placeTimer      = new StopWatch();
    public  final StopWatch     obbyTimer       = new StopWatch();
    public  final StopWatch     obbyCalcTimer   = new StopWatch();
    public  final StopWatch     liquidTimer     = new StopWatch();
    public  final StopWatch     forceTimer      = new StopWatch();
    public  final StopWatch     shieldTimer     = new StopWatch();
    public  final DiscreteTimer feetTimer       = new GuardTimer();

    /* --- マップ / キュー --- */
    /** 配置済みクリスタルの座標→タイムスタンプ (SpawnThread などが参照) */
    public  final Map<BlockPos, CrystalTimeStamp> placed =
            new ConcurrentHashMap<>();
    /** post-action キュー (rotation 後に実行する Runnable) */
    public  final Queue<Runnable> post =
            new ConcurrentLinkedQueue<>();

    /* --- ヘルパー --- */
    public  final HelperPlace            placeHelper;
    public  final HelperBreak            breakHelper;
    public  final HelperBreakMotion      breakMotionHelper;
    public  final HelperObby             obbyHelper;
    public  final HelperLiquids          liquidHelper;
    public  final HelperSequential       sequentialHelper;
    public  final HelperRotation         rotationHelper;
    public  final HelperRange            rangeHelper;
    public  final HelperInstantAttack    idHelper;
    public  final WeaknessHelper         weaknessHelper;
    public  final AntiTotemHelper        antiTotemHelper;
    public  final DamageSyncHelper       damageSyncHelper;
    public  final ExtrapolationHelper    extrapolationHelper;
    public  final DamageHelper           damageHelper;
    public  final ForceHelper            forceHelper;
    public  final RotationCanceller      rotationCanceller;
    public  final ThreadHelper           threadHelper;
    public  final ServerTimeHelper       serverTimeHelper;
    public  final FakeCrystalRender      crystalRender;

    /* --- 状態変数 --- */
    public  volatile RotationFunction rotation;
    public  volatile Entity  focus;
    public  volatile boolean switching;
    public  volatile boolean noGod;
    public  volatile boolean isSpoofing;
    public  volatile BlockPos bombPos;
    public  volatile BlockPos slidePos;
    public  volatile String   damage = "";
    public  final AtomicInteger motionID = new AtomicInteger();
    public  final StopWatch     pullTimer  = new StopWatch();
    public  final StopWatch     slideTimer = new StopWatch();
    public  final StopWatch     zoomTimer  = new StopWatch();

    // =====================================================================
    // コンストラクタ
    // =====================================================================
    public AutoCrystal()
    {
        super("AutoCrystal", Category.Combat);

        this.placeHelper         = new HelperPlace(this);
        this.breakHelper         = new HelperBreak(this);
        this.breakMotionHelper   = new HelperBreakMotion(this);
        this.obbyHelper          = new HelperObby(this);
        this.liquidHelper        = new HelperLiquids(this);
        this.sequentialHelper    = new HelperSequential(this);
        this.rotationHelper      = new HelperRotation(this);
        this.rangeHelper         = new HelperRange(this);
        this.idHelper            = new HelperInstantAttack();
        this.weaknessHelper      = new WeaknessHelper(antiWeakness, cooldown);
        this.antiTotemHelper     = new AntiTotemHelper(totemHealth);
        this.damageSyncHelper    = new DamageSyncHelper(
            Bus.EVENT_BUS, discreteSync, syncDelay, dangerSync);
        this.extrapolationHelper = new ExtrapolationHelper(this);
        this.damageHelper        = new DamageHelper(
            this, extrapolationHelper, terrainCalc, extrapol,
            bExtrapol, selfExtrapolation, obbyTerrain);
        this.forceHelper         = new ForceHelper(this);
        this.rotationCanceller   = new RotationCanceller(this, maxCancel);
        this.threadHelper        = new ThreadHelper(
            this, multiThread, mainThreadThreads, threadDelay,
            rotationThread, rotate);
        this.serverTimeHelper    = new ServerTimeHelper(
            this, rotate, placeSwing, antiFeetPlace, newVer, feetBuffer);
        this.crystalRender       = new FakeCrystalRender(simulatePlace);

        // --- リスナー登録 ---
        this.listeners.add(new ListenerGameLoop(this));
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerEntity(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerDestroyEntities(this));
        this.listeners.add(new ListenerExplosion(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.listeners.add(new ListenerBlockMulti(this));
        this.listeners.add(new ListenerCPlayers(this));
        this.listeners.add(new ListenerDestroyBlock(this));
        this.listeners.add(new ListenerPacketSendSwap(this));
        this.listeners.add(new ListenerRender(this));
        this.listeners.add(new ListenerRenderEntities(this));
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerNoMotion(this));
        this.listeners.add(new ListenerKeyboard(this));
        this.listeners.add(new ListenerUseEntity(this));
        this.listeners.add(new ListenerPostPlace(this));
        this.listeners.add(new ListenerPosLook(this));
        this.listeners.add(new ListenerWorldClient(this));

        // --- GUI ページビルダー ---
        new PageBuilder<>(this, pages)
            .addPage(p -> p == ACPages.Place,
                 place, targetMode, placeRange, placeTrace, minDamage,
                 placeDelay, maxSelfPlace, multiPlace, slowPlaceDmg,
                 slowPlaceDelay, override, newVer, newVerEntities, placeSwing,
                 smartTrace, placeRangeEyes, placeRangeCenter, traceWidth,
                 fallbackTrace, rayTraceBypass, forceBypass,
                 rayBypassFacePlace, rayBypassFallback, bypassTicks,
                 rbYaw, rbPitch, bypassRotationTime, ignoreNonFull,
                 efficientPlacements, simulatePlace)
            .addPage(p -> p == ACPages.Break,
                 attackMode, attack, breakRange, breakDelay, breakTrace,
                 minBreakDamage, maxSelfBreak, slowBreakDamage, slowBreakDelay,
                 instant, asyncCalc, alwaysCalc, ncpRange, placeBreakRange,
                 smartTicks, negativeTicks, smartBreakTrace, negativeBreakTrace,
                 packets, overrideBreak, antiWeakness, instantAntiWeak,
                 efficient, manually, manualDelay, breakSwing)
            .addPage(p -> p == ACPages.Rotate,
                 rotate, rotateMode, smoothSpeed, endRotations, angle,
                 placeAngle, height, placeHeight, rotationTicks,
                 focusRotations, focusAngleCalc, focusExponent, focusDiff,
                 rotationExponent, minRotDiff, existed, pingExisted)
            .addPage(p -> p == ACPages.Misc,
                 targetRange, pbTrace, range, suicide, shield, shieldCount,
                 shieldMinDamage, shieldSelfDamage, shieldDelay, shieldRange,
                 shieldPrioritizeHealth, multiTask, multiPlaceCalc,
                 multiPlaceMinDmg, countDeadCrystals, countDeathTime, yCalc,
                 dangerSpeed, dangerHealth, cooldown, placeCoolDown,
                 antiFriendPop, antiFeetPlace, feetBuffer, stopWhenEating,
                 stopWhenMining, dangerFacePlace, motionCalc)
            .addPage(p -> p == ACPages.FacePlace,
                 holdFacePlace, facePlace, minFaceDmg, armorPlace, pickAxeHold,
                 antiNaked, fallBack, fallBackDiff, fallBackDmg)
            .addPage(p -> p == ACPages.Switch,
                 autoSwitch, mainHand, switchBind, switchBack, useAsOffhand,
                 instantOffhand, pingBypass, switchMessage, swing, placeHand,
                 cooldownBypass, obsidianBypass, antiWeaknessBypass, mineBypass,
                 obbyHand, await, yieldProtection, swapDelay, swapWaitFull)
            .addPage(p -> p == ACPages.Render,
                 render, renderTime, box, boxColor, outLine, indicatorColor,
                 fade, fadeComp, fadeTime, realtime, slide, smoothSlide,
                 slideTime, zoom, zoomTime, zoomOffset, multiZoom,
                 renderExtrapolation, renderDamage, renderMode,
                 arrayInfo, showTarget, showDelay, showSpeed, showCPS)
            .addPage(p -> p == ACPages.SetDead,
                 setDead, instantSetDead, pseudoSetDead, simulateExplosion,
                 soundRemove, useSafeDeathTime, safeDeathTime, deathTime)
            .addPage(p -> p == ACPages.Obsidian,
                 obsidian, basePlaceOnly, obbySwitch, obbyDelay, obbyCalc,
                 helpingBlocks, obbyMinDmg, terrainCalc, obbySafety, obbyTrace,
                 obbyTerrain, obbyPreSelf, fastObby, maxDiff, maxDmgDiff,
                 setState, obbySwing, obbyFallback, obbyRotate)
            .addPage(p -> p == ACPages.Liquids,
                 interact, inside, lava, water, liquidObby, liquidRayTrace,
                 liqDelay, liqRotate, pickaxeOnly, interruptSpeedmine, setAir,
                 absorb, requireOnGround, ignoreLavaItems, sponges)
            .addPage(p -> p == ACPages.AntiTotem,
                 antiTotem, totemHealth, minTotemOffset, maxTotemOffset,
                 popDamage, totemSync, forceAntiTotem, forceSlow, syncForce,
                 dangerForce, forcePlaceConfirm, forceBreakConfirm, attempts)
            .addPage(p -> p == ACPages.DamageSync,
                 damageSync, preSynCheck, discreteSync, dangerSync,
                 placeConfirm, breakConfirm, syncDelay, surroundSync)
            .addPage(p -> p == ACPages.Extrapolation,
                 extrapol, bExtrapol, blockExtrapol, blockExtraMode,
                 doubleExtraCheck, avgPlaceDamage, placeExtraWeight,
                 placeNormalWeight, avgBreakExtra, breakExtraWeight,
                 breakNormalWeight, gravityExtrapolation, gravityFactor,
                 yPlusFactor, yMinusFactor, selfExtrapolation)
            .addPage(p -> p == ACPages.GodModule,
                 idPredict, idOffset, idDelay, idPackets,
                 godAntiTotem, holdingCheck, toolCheck, godSwing)
            .addPage(p -> p == ACPages.Development,
                 preCalc, preCalcExtra, preCalcDamage)
            .addPage(p -> p == ACPages.MultiThread,
                 multiThread, rotationThread, partial, maxCancel, timeOut,
                 threadDelay, pullBasedDelay, blockDestroyThread,
                 explosionThread, soundThread, entityThread, gameloop,
                 spawnThread, destroyThread, serverThread, asyncServerThread,
                 earlyFeetThread, lateBreakThread, motionThread,
                 blockChangeThread, priority, smartPost, clearPost, spectator)
            .register(Visibilities.VISIBILITY_MANAGER);
    }

    // =====================================================================
    // onEnable / onDisable
    // =====================================================================

    @Override
    protected void onEnable()
    {
        started = false;
        ATOMIC_STARTED.set(false);
        breakTimer.reset();
        placeTimer.reset();
        obbyTimer.reset();
        liquidTimer.reset();
        forceTimer.reset();
        shieldTimer.reset();
        feetTimer.reset(0);
        placed.clear();
        post.clear();
        rotation       = null;
        focus          = null;
        switching      = false;
        noGod          = false;

        weaknessHelper.updateWeakness();
    }

    @Override
    protected void onDisable()
    {
        started = false;
        ATOMIC_STARTED.set(false);
        placed.clear();
        post.clear();
        rotation  = null;
        focus     = null;
        switching = false;
        noGod     = false;
        isSpoofing = false;
        bombPos    = null;
        slidePos   = null;
        damage     = "";

        // ヘルパーリセット
        threadHelper.reset();
        crystalRender.clear();

        // [NEW] トラッカーのリセット
        positionCache.clear();
        swapStateTracker.clear();
        rttTracker.clear();
    }
    // =====================================================================
    // ユーティリティ / ゲッター / 状態管理メソッド
    // =====================================================================

    /** post キューにある Runnable をすべて実行する */
    public void runPost()
    {
        Runnable r;
        while ((r = post.poll()) != null)
        {
            r.run();
        }
    }

    /** 自傷特化モード (Suicide) かどうか */
    public boolean isSuicideModule()
    {
        return suicide.getValue()
            && !isSuicideModule(RotationUtil.getRotationPlayer());
    }

    private boolean isSuicideModule(EntityPlayer player)
    {
        return player == null || EntityUtil.isDead(player);
    }

    /** 危険判定 (DangerSpeed が有効かつ体力が dangerHealth 以下) */
    public boolean shouldDanger()
    {
        if (!dangerSpeed.getValue())
        {
            return false;
        }

        EntityPlayer p = mc.player;
        return p != null && EntityUtil.getHealth(p) <= dangerHealth.getValue();
    }

    /** MinDamage の実効値を返す (DangerFacePlace 考慮) */
    public float getMinDamage()
    {
        if (dangerFacePlace.getValue() && shouldDanger())
        {
            return 0.0f;
        }

        return minDamage.getValue();
    }

    /** クリスタルを ESP ターゲットとしてセット */
    private volatile Entity crystal;

    public void setCrystal(Entity entity)
    {
        this.crystal = entity;
    }

    /** 現在のクリスタルターゲットを取得 */
    public Entity getCrystal()
    {
        return crystal;
    }

    /** 描画座標とダメージ値をセット */
    public void setRenderPos(BlockPos pos, float dmg)
    {
        if (slide.getValue())
        {
            BlockPos prev = this.renderPos;
            if (prev != null && !prev.equals(pos))
            {
                slidePos = prev;
                slideTimer.reset();
            }
        }

        if (zoom.getValue())
        {
            BlockPos prev = this.renderPos;
            if (prev == null || !prev.equals(pos))
            {
                zoomTimer.reset();
            }
        }

        this.renderPos = pos;
        this.renderDamageValue = dmg;
        this.damage = String.format("%.1f", dmg);
    }

    /** RayTrace バイパス座標をセット */
    public void setBypassPos(BlockPos pos)
    {
        this.bypassPos = pos;
    }

    /** ターゲットプレイヤーをセット */
    public void setTarget(EntityPlayer player)
    {
        antiTotemHelper.setTarget(player);
    }

    /** PingBypass が有効かどうか */
    public boolean isPingBypass()
    {
        if (!pingBypass.getValue())
        {
            return false;
        }

        return PINGBYPASS.computeIfPresent(PingBypassModule::isEnabled);
    }

    /** プレイヤーが何かを食べているか */
    public boolean isEating()
    {
        return mc.player != null
            && mc.player.isHandActive()
            && mc.player.getActiveItemStack().getItemUseAction()
                    == EnumAction.EAT;
    }

    /** プレイヤーがブロックを掘っているか */
    public boolean isMining()
    {
        return mc.playerController != null
            && mc.playerController.getIsHittingBlock();
    }

    /** ローテーションチェックを省略するか判定 */
    public boolean isNotCheckingRotations()
    {
        return rotate.getValue() == ACRotate.None
            || rotate.getValue() == ACRotate.Break;
    }

    /** placeRange の外か判定 */
    public boolean isOutsidePlaceRange(BlockPos pos)
    {
        return placeRangeCenter.getValue()
            ? pos.distanceSqToCenter(
                Managers.POSITION.getX(),
                Managers.POSITION.getY(),
                Managers.POSITION.getZ())
                    >= MathUtil.square(placeRange.getValue())
            : BlockUtil.getDistanceSq(pos)
                    >= MathUtil.square(placeRange.getValue());
    }

    /** deathTime を取得 (useSafeDeathTime 考慮) */
    public int getDeathTime()
    {
        if (useSafeDeathTime.getValue())
        {
            return safeDeathTime.getValue();
        }

        return deathTime.getValue();
    }

    /** HoldFacePlace / PickAxeHold の判定 */
    public boolean isHoldFacePlacing()
    {
        if (!holdFacePlace.getValue())
        {
            return false;
        }

        ItemStack held = mc.player.getHeldItemMainhand();
        if (pickAxeHold.getValue() && held.getItem() instanceof ItemPickaxe)
        {
            return Mouse.isButtonDown(0);
        }

        return !(held.getItem() instanceof ItemPickaxe)
            && Mouse.isButtonDown(0);
    }

    // =====================================================================
    // ArrayInfo (HUD 表示用)
    // =====================================================================

    public String getArrayInfo()
    {
        if (!arrayInfo.getValue())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        if (showTarget.getValue())
        {
            EntityPlayer target = antiTotemHelper.getTarget();
            if (target != null)
            {
                sb.append(TextColor.GRAY)
                  .append(target.getName())
                  .append(' ');
            }
        }

        if (showDelay.getValue())
        {
            sb.append(TextColor.GRAY)
              .append('B').append(breakDelay.getValue())
              .append('/')
              .append('P').append(placeDelay.getValue())
              .append(' ');
        }

        if (showCPS.getValue())
        {
            // 既存実装: breakHelper の CPS 計測値を利用
        }

        return sb.length() == 0 ? null : sb.toString().trim();
    }

    // =====================================================================
    // スレッド起動ヘルパー (既存コードそのまま)
    // =====================================================================

    /**
     * MultiThread 設定に応じて計算スレッドを起動する。
     * 既存のスレッド管理ロジックはそのまま維持。
     */
    public void startThread(SafeRunnable runnable)
    {
        if (!multiThread.getValue())
        {
            return;
        }

        int delay = threadDelay.getValue();
        if (delay <= 0)
        {
            EXECUTOR.submit(runnable);
        }
        else
        {
            EXECUTOR.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * ATOMIC_STARTED フラグを使って重複スレッドを防ぐ。
     */
    public boolean tryStartThread(SafeRunnable runnable)
    {
        if (ATOMIC_STARTED.compareAndSet(false, true))
        {
            startThread(() ->
            {
                try
                {
                    runnable.run();
                }
                finally
                {
                    ATOMIC_STARTED.set(false);
                }
            });
            return true;
        }

        return false;
    }

    // =====================================================================
    // 不足していたメソッド (リスナーから参照)
    // =====================================================================

    /** レンダリング用に保存された BlockPos */
    private volatile BlockPos renderPos;
    private volatile float    renderDamageValue;
    private volatile BlockPos bypassPos;

    /** レンダリング対象座標を取得 */
    public BlockPos getRenderPos()
    {
        return renderPos;
    }

    /** Bypass座標を取得 */
    public BlockPos getBypassPos()
    {
        return bypassPos;
    }

    /** ターゲットを取得 (HelperRotation が参照) */
    public EntityPlayer getTarget()
    {
        return antiTotemHelper.getTarget();
    }

    /** ワールド変更時にリセット (ListenerWorldClient が呼ぶ) */
    public void reset()
    {
        placed.clear();
        post.clear();
        rotation   = null;
        focus      = null;
        switching  = false;
        noGod      = false;
        isSpoofing = false;
        bombPos    = null;
        slidePos   = null;
        damage     = "";
        renderPos  = null;
        bypassPos  = null;
        threadHelper.reset();
        crystalRender.clear();
        positionCache.clear();
        swapStateTracker.clear();
        rttTracker.clear();
    }

    /** Executor のチェック (ListenerTick が呼ぶ) */
    public void checkExecutor()
    {
        // Executor の状態チェック用スタブ
    }

} // end class AutoCrystal
