package me.earth.earthhack.impl.managers.client;



import me.earth.earthhack.impl.modules.combat.autocrystal.AutoCrystal;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.register.IterationRegister;
import me.earth.earthhack.api.register.Registrable;
import me.earth.earthhack.api.register.exception.CantUnregisterException;
import me.earth.earthhack.impl.Earthhack;
import me.earth.earthhack.impl.event.events.client.PostInitEvent;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.accountspoof.AccountSpoof;
import me.earth.earthhack.impl.modules.client.anticheat.AntiCheat;
import me.earth.earthhack.impl.modules.client.autoconfig.AutoConfig;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.modules.client.colors.Colors;
import me.earth.earthhack.impl.modules.client.commands.Commands;
import me.earth.earthhack.impl.modules.client.configs.ConfigModule;
import me.earth.earthhack.impl.modules.client.customfont.FontMod;
import me.earth.earthhack.impl.modules.client.debug.Debug;
import me.earth.earthhack.impl.modules.client.hud.HUD;
import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.modules.client.management.Management;
import me.earth.earthhack.impl.modules.client.media.Media;
import me.earth.earthhack.impl.modules.client.nospoof.NoSpoof;
import me.earth.earthhack.impl.modules.client.notifications.Notifications;
import me.earth.earthhack.impl.modules.client.pbgui.PbGui;
import me.earth.earthhack.impl.modules.client.pbteleport.PbTeleport;
import me.earth.earthhack.impl.modules.client.pingbypass.PingBypassModule;
import me.earth.earthhack.impl.modules.client.rotationbypass.Compatibility;
import me.earth.earthhack.impl.modules.client.safety.Safety;
import me.earth.earthhack.impl.modules.client.server.ServerModule;
import me.earth.earthhack.impl.modules.client.settings.SettingsModule;
import me.earth.earthhack.impl.modules.client.tab.TabModule;

import me.earth.earthhack.impl.modules.combat.autofeettrap.AutoFeetTrap;
import me.earth.earthhack.impl.modules.combat.holefiller.HoleFiller;
import me.earth.earthhack.impl.modules.combat.killaura.KillAura;
import me.earth.earthhack.impl.modules.combat.offhand.Offhand;
import me.earth.earthhack.impl.modules.combat.surround.Surround;
import me.earth.earthhack.impl.modules.misc.announcer.Announcer;
import me.earth.earthhack.impl.modules.misc.buildheight.BuildHeight;
import me.earth.earthhack.impl.modules.misc.chat.Chat;
import me.earth.earthhack.impl.modules.misc.mcf.MCF;
import me.earth.earthhack.impl.modules.misc.tpssync.TpsSync;
import me.earth.earthhack.impl.modules.movement.blocklag.BlockLag;
import me.earth.earthhack.impl.modules.movement.tickshift.TickShift;
import me.earth.earthhack.impl.modules.movement.velocity.Velocity;
import me.earth.earthhack.impl.modules.player.fakeplayer.FakePlayer;
import me.earth.earthhack.impl.modules.player.ncptweaks.NCPTweaks;
import me.earth.earthhack.impl.modules.render.crystalparticles.Crystalparticles;
import me.earth.earthhack.impl.modules.render.notification.Notification;
import me.earth.earthhack.impl.modules.render.offhandshake.OffhandShake;
import me.earth.earthhack.impl.modules.render.oldanimation.OldAnimation;
import me.earth.earthhack.vanilla.Environment;

import java.util.ArrayList;

public class ModuleManager extends IterationRegister<Module>
{
    public void init()
    {
        Earthhack.getLogger().info("Initializing Modules.");
        this.forceRegister(new AccountSpoof());
        this.forceRegister(new AntiCheat());
        this.forceRegister(new AutoConfig());
        this.forceRegister(new ClickGui());
        this.forceRegister(new Colors());
        this.forceRegister(new Commands());
        this.forceRegister(new ConfigModule());
        this.forceRegister(new Debug());
        this.forceRegister(new FontMod());
        this.forceRegister(new HUD());
        this.forceRegister(new KonasHUD());
        this.forceRegister(new Management());
        this.forceRegister(new NoSpoof());
        this.forceRegister(new Notifications());
        this.forceRegister(new Compatibility());
        this.forceRegister(new Safety());
        this.forceRegister(new ServerModule());
        this.forceRegister(new PbGui());
        this.forceRegister(new PbTeleport());
        this.forceRegister(new SettingsModule());
        this.forceRegister(new TabModule());
        this.forceRegister(new Media());


        this.forceRegister(new AutoCrystal());
        this.forceRegister(new AutoFeetTrap());
        this.forceRegister(new HoleFiller());
        this.forceRegister(new KillAura());
        this.forceRegister(new Offhand());
        this.forceRegister(new Surround());

        this.forceRegister(new Announcer());
        this.forceRegister(new BuildHeight());
        this.forceRegister(new Chat());
        this.forceRegister(new MCF());
        this.forceRegister(new TpsSync());

        this.forceRegister(new BlockLag());
        this.forceRegister(new TickShift());
        this.forceRegister(new Velocity());

        this.forceRegister(new FakePlayer());
        this.forceRegister(new NCPTweaks());

        this.forceRegister(new Crystalparticles());
        this.forceRegister(new OffhandShake());
        this.forceRegister(new OldAnimation());
        this.forceRegister(new Notification());

        this.forceRegister(new PingBypassModule());

        Bus.EVENT_BUS.post(new PostInitEvent());
    }

    public void load()
    {
        Caches.setManager(this);
        for (Module module : getRegistered())
        {
            module.load();
        }
    }

    @Override
    public void unregister(Module module) throws CantUnregisterException
    {
        super.unregister(module);
        Bus.EVENT_BUS.unsubscribe(module);
    }

    protected void forceRegister(Module module)
    {
        registered.add(module);
        if (module instanceof Registrable)
        {
            ((Registrable) module).onRegister();
        }
    }

    public ArrayList<Module> getModulesFromCategory(Category moduleCategory) {
        final ArrayList<Module> iModules = new ArrayList<>();
        for (Module iModule : getRegistered()) {
            if (iModule.getCategory() == moduleCategory && iModule.isHidden() != me.earth.earthhack.api.module.util.Hidden.Hidden) {
                iModules.add(iModule);
            }
        }
        return iModules;
    }
}

