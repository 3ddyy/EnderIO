package crazypants.enderio.base.recipe;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.stackable.Things;

import crazypants.enderio.util.Prep;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class ThingsRecipeInput implements IRecipeInput {

  private final @Nonnull Things things;
  /**
   * A stack to represent this input in situations where a single stack is needed. It also holds the stackSize for the input. Callers may modify this stack's
   * size to keep track of things (obviously they need to copy this object first).
   */
  private final @Nonnull ItemStack leadStack;
  private final int slot;
  private final float multiplier;

  public ThingsRecipeInput(@Nonnull Things things) {
    this(things, -1);
  }

  public ThingsRecipeInput(@Nonnull Things things, int slot) {
    this(things, slot, 1f);
  }

  public ThingsRecipeInput(@Nonnull Things things, @Nonnull ItemStack leadStack, int slot) {
    this(things, leadStack, slot, 1f);
  }

  public ThingsRecipeInput(@Nonnull Things things, int slot, float multiplier) {
    this(things, things.getItemStacks().isEmpty() ? Prep.getEmpty() : things.getItemStacks().get(0).copy(), slot, multiplier);
  }

  public ThingsRecipeInput(@Nonnull Things things, @Nonnull ItemStack leadStack, int slot, float multiplier) {
    this.things = things;
    this.leadStack = leadStack;
    this.slot = slot;
    this.multiplier = multiplier;
  }

  public @Nonnull ThingsRecipeInput setCount(int count) {
    leadStack.setCount(count);
    return this;
  }

  @Override
  public @Nonnull ThingsRecipeInput copy() {
    return new ThingsRecipeInput(things, leadStack.copy(), slot, multiplier);
  }

  @Override
  public boolean isFluid() {
    return false;
  }

  @Override
  public @Nonnull ItemStack getInput() {
    return leadStack;
  }

  @Override
  public FluidStack getFluidInput() {
    return null;
  }

  @Override
  public float getMulitplier() {
    return multiplier; // used by Vat recipes only
  }

  @Override
  public int getSlotNumber() {
    return slot;
  }

  @Override
  public boolean isInput(@Nonnull ItemStack test) {
    return things.contains(test) && (!leadStack.hasTagCompound() || ItemStack.areItemStackTagsEqual(leadStack, test));
  }

  @Override
  public boolean isInput(FluidStack test) {
    return false;
  }

  @Override
  public ItemStack[] getEquivelentInputs() {
    final ItemStack[] result = things.getItemStacksRaw().toArray(new ItemStack[0]);
    for (int i = 0; i < result.length; i++) {
      result[i] = result[i].copy();
      result[i].setCount(leadStack.getCount());
      if (leadStack.hasTagCompound()) {
        result[i].setTagCompound(leadStack.getTagCompound());
      }
    }
    return result;
  }

  @Override
  public boolean isValid() {
    return Prep.isValid(leadStack);
  }

}
