package crazypants.enderio.base.config.config;

import crazypants.enderio.base.config.Config.Section;
import crazypants.enderio.base.config.SectionedValueFactory;
import crazypants.enderio.base.config.ValueFactory.IValue;

public final class InfinityConfig {

  public static final SectionedValueFactory F = new SectionedValueFactory(BaseConfig.F, new Section("", "items.infinityPowder"));

  public static final IValue<Float> dropChance = F.make("dropChance", .5f, //
      "Chance that Infinity Powder will drop from fire on bedrock.").setRange(0, 1).sync();
  public static final IValue<Integer> dropStackSize = F.make("dropStackSize", 1, //
      "Stack size when dropped from fire.").setRange(1, 64).sync();
  public static final IValue<Boolean> makesSound = F.make("makesSound", true, //
      "Should it make a sound when Infinity Powder drops from fire?");
  public static final IValue<Integer> fireMinAge = F.make("fireMinAge", 260, //
      "How old (in ticks) does a dying fire have to be to spawn Infinity Powder? (average fire age at death is 11.5s, default is 13s").setRange(1, 1000).sync();
  public static final IValue<Boolean> enableInAllDimensions = F.make("enableInAllDimensions", false, //
      "Should making Infinity Powder be allowed in all dimensions? If not, it'll only work in the overworld.").sync();
  public static final IValue<Boolean> inWorldCraftingEnabled = F.make("inWorldCraftingEnabled", true, //
      "Should making Infinity Powder by lighting bedrock on fire be enabled? Please note that you need to provide an alternative way of crafting it if you disabled this.")
      .sync();

}
