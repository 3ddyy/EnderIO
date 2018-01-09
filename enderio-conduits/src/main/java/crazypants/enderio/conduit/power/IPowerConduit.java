package crazypants.enderio.conduit.power;

import crazypants.enderio.base.conduit.IExtractor;
import crazypants.enderio.base.power.ILegacyPowerReceiver;
import crazypants.enderio.base.power.IPowerInterface;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

public interface IPowerConduit extends ILegacyPowerReceiver, IExtractor {

  // TODO Lang

  public static final String ICON_KEY = "enderio:blocks/powerConduit";
  public static final String ICON_KEY_INPUT = "enderio:blocks/powerConduitInput";
  public static final String ICON_KEY_OUTPUT = "enderio:blocks/powerConduitOutput";
  public static final String ICON_CORE_KEY = "enderio:blocks/powerConduitCore";
  public static final String ICON_TRANSMISSION_KEY = "enderio:blocks/powerConduitTransmission";

  public static final String COLOR_CONTROLLER_ID = "ColorController";

  IPowerInterface getExternalPowerReceptor(EnumFacing direction);

  int getMaxEnergyExtracted(EnumFacing dir);

  TextureAtlasSprite getTextureForInputMode();

  TextureAtlasSprite getTextureForOutputMode();

  //called from NetworkPowerManager
  void onTick();
  
  boolean getConnectionsDirty();


}
