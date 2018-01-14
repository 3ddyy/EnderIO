package crazypants.enderio.machines;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.Lang;

import crazypants.enderio.api.addon.IEnderIOAddon;
import crazypants.enderio.machines.config.Config;
import crazypants.enderio.machines.config.ConfigHandler;
import crazypants.enderio.machines.config.RecipeLoaderMachines;
import crazypants.enderio.machines.machine.obelisk.render.ObeliskRenderManager;
import crazypants.enderio.machines.network.PacketHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = EnderIOMachines.MODID, name = EnderIOMachines.MOD_NAME, version = EnderIOMachines.VERSION, dependencies = EnderIOMachines.DEPENDENCIES)
@EventBusSubscriber(Side.CLIENT)
public class EnderIOMachines implements IEnderIOAddon {

  public static final @Nonnull String MODID = "enderiomachines";
  public static final @Nonnull String DOMAIN = "enderio";
  public static final @Nonnull String MOD_NAME = "Ender IO Machines";
  public static final @Nonnull String VERSION = "@VERSION@";

  private static final @Nonnull String DEFAULT_DEPENDENCIES = "after:" + crazypants.enderio.base.EnderIO.MODID;
  public static final @Nonnull String DEPENDENCIES = DEFAULT_DEPENDENCIES;

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  public static void onModelRegister(ModelRegistryEvent event) {
    ObeliskRenderManager.INSTANCE.registerRenderers();
  }

  @EventHandler
  public static void init(FMLPreInitializationEvent event) {
    ConfigHandler.init(event);
  }

  @EventHandler
  public static void init(FMLInitializationEvent event) {
    ConfigHandler.init(event);
    PacketHandler.init(event);

    if (Config.registerRecipes.get()) {
      RecipeLoaderMachines.addRecipes();
    }
  }

  @EventHandler
  public static void init(FMLPostInitializationEvent event) {
    ConfigHandler.init(event);
  }

  public static final @Nonnull Lang lang = new Lang(DOMAIN);

  @Override
  @Nullable
  public Configuration getConfiguration() {
    return ConfigHandler.config;
  }

}
