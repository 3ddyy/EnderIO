package crazypants.enderio.machine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.BlockCoord;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public interface IIoConfigurable {

  public @Nonnull IoMode toggleIoModeForFace(@Nullable EnumFacing faceHit);

  public boolean supportsMode(@Nullable EnumFacing faceHit, @Nullable IoMode mode);

  public void setIoMode(@Nullable EnumFacing faceHit, @Nullable IoMode mode);

  public @Nonnull IoMode getIoMode(@Nullable EnumFacing face);

  public void clearAllIoModes();

  BlockPos getLocation();

}
