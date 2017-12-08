package crazypants.enderio.machines.machine.wireless;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.NBTAction;

import crazypants.enderio.base.TileEntityEio;
import crazypants.enderio.base.network.PacketHandler;
import crazypants.enderio.base.paint.IPaintable;
import crazypants.enderio.base.paint.YetaUtil;
import crazypants.enderio.base.power.ILegacyPowerReceiver;
import crazypants.enderio.base.power.PowerHandlerUtil;
import crazypants.enderio.base.power.wireless.IWirelessCharger;
import crazypants.enderio.base.power.wireless.WirelessChargerController;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;

@Storable
public class TileWirelessCharger extends TileEntityEio
    implements ILegacyPowerReceiver, IWirelessCharger, IPaintable.IPaintableTileEntity {

  public static final int MAX_ENERGY_STORED = 200000;
  public static final int MAX_ENERGY_IN = 10000;
  public static final int MAX_ENERGY_OUT = 10000;

  @Store
  int storedEnergyRF;

  private double lastPowerUpdate = -1;

  private boolean registered = false;

  public TileWirelessCharger() {
  }

  @Override
  public void invalidate() {
    super.invalidate();
    WirelessChargerController.instance.deregisterCharger(this);
    registered = false;
  }

  @Override
  public void doUpdate() {
    if (world.isRemote) {
      YetaUtil.refresh(this);
      return;
    }

    if (!registered) {
      WirelessChargerController.instance.registerCharger(this);
      registered = true;
    }

    if ((lastPowerUpdate == -1) || (lastPowerUpdate == 0 && storedEnergyRF > 0) || (lastPowerUpdate > 0 && storedEnergyRF == 0)
        || (lastPowerUpdate != storedEnergyRF && shouldDoWorkThisTick(20))) {
      lastPowerUpdate = storedEnergyRF;
      PacketHandler.sendToAllAround(new PacketStoredEnergy(this), this);
    }

  }

  @Override
  public boolean chargeItems(NonNullList<ItemStack> items) {
    boolean chargedItem = false;
    int available = Math.min(MAX_ENERGY_OUT, storedEnergyRF);
    for (int i = 0, end = items.size(); i < end && available > 0; i++) {
      ItemStack item = items.get(i);
      if (!item.isEmpty()) {
        IEnergyStorage chargable = PowerHandlerUtil.getCapability(item, null);
        if (chargable != null && item.getCount() == 1) {
          int max = chargable.getMaxEnergyStored();
          int cur = chargable.getEnergyStored();
          int canUse = Math.min(available, max - cur);
          if (cur < max) {
            int used = chargable.receiveEnergy(canUse, false);
            if (used > 0) {
              storedEnergyRF = storedEnergyRF - used;
              chargedItem = true;
              available -= used;
            }
          }
        }
      }
    }
    return chargedItem;
  }

  @Override
  public int getMaxEnergyRecieved(EnumFacing dir) {
    return MAX_ENERGY_IN;
  }

  @Override
  public int getEnergyStored() {
    return storedEnergyRF;
  }

  @Override
  public int getMaxEnergyStored() {
    return MAX_ENERGY_STORED;
  }

  @Override
  public void setEnergyStored(int stored) {
    storedEnergyRF = stored;
  }

  @Override
  public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
    return PowerHandlerUtil.recieveInternal(this, maxReceive, from, simulate);
  }

  @Override
  public int takeEnergy(int max) {
    if (isActive()) {
      int prev = storedEnergyRF;
      storedEnergyRF = Math.max(0, storedEnergyRF - max);
      return prev - storedEnergyRF;
    }
    return 0;
  }

  @Override
  public boolean canConnectEnergy(@Nonnull EnumFacing from) {
    return true;
  }

  @Override
  public World getworld() {
    return getWorld();
  }

  @Override
  public boolean displayPower() {
    return true;
  }

  @Override
  public boolean isActive() {
    return getEnergyStored() > 0 && !isPoweredRedstone();
  }

  @Nonnull
  @Override
  public BlockPos getLocation() {
    return pos;
  }

  @Override
  protected void onAfterDataPacket() {
    updateBlock();
  }

  @Store({ NBTAction.SYNC, NBTAction.SAVE })
  protected IBlockState sourceBlock;

  @Override
  public IBlockState getPaintSource() {
    return sourceBlock;
  }

  @Override
  public void setPaintSource(@Nullable IBlockState sourceBlock) {
    this.sourceBlock = sourceBlock;
    markDirty();
    updateBlock();
  }

}