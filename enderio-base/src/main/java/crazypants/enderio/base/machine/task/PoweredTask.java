package crazypants.enderio.base.machine.task;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.NNList;

import crazypants.enderio.base.machine.interfaces.IPoweredTask;
import crazypants.enderio.base.recipe.IMachineRecipe;
import crazypants.enderio.base.recipe.MachineRecipeInput;
import crazypants.enderio.base.recipe.MachineRecipeRegistry;
import crazypants.enderio.base.recipe.RecipeBonusType;
import crazypants.enderio.base.recipe.IMachineRecipe.ResultStack;
import crazypants.enderio.util.Prep;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;

public class PoweredTask implements IPoweredTask {

  public static final @Nonnull String KEY_INPUT_STACKS = "inputsStacks";

  public static final @Nonnull String KEY_RECIPE = "recipeUid";
  public static final @Nonnull String KEY_USED_ENERGY = "usedEnergy";
  private static final @Nonnull String KEY_CHANCE = "chance";

  private float usedEnergy = 0;

  private @Nonnull NNList<MachineRecipeInput> inputs;

  private float requiredEnergy;

  private @Nonnull RecipeBonusType bonusType;

  private @Nonnull IMachineRecipe recipe;

  private float chance;

  public PoweredTask(@Nonnull IMachineRecipe recipe, float chance, @Nonnull NNList<MachineRecipeInput> inputs) {
    this(recipe, 0, chance, inputs);
  }

  protected PoweredTask(@Nonnull IMachineRecipe recipe, float usedEnergy, float chance, @Nonnull NNList<MachineRecipeInput> inputsIn) {
    inputs = new NNList<>();
    for (int i = 0; i < inputsIn.size(); i++) {
      if (Prep.isValid(inputsIn.get(i).item)) {
        inputs.add(new MachineRecipeInput(inputsIn.get(i).slotNumber, inputsIn.get(i).item.copy()));
      } else if (inputsIn.get(i).fluid != null) {
        inputs.add(new MachineRecipeInput(inputsIn.get(i).slotNumber, inputsIn.get(i).fluid.copy()));
      }
    }

    this.recipe = recipe;
    this.usedEnergy = usedEnergy;
    this.chance = MathHelper.clamp(chance, 0, 1);
    requiredEnergy = recipe.getEnergyRequired(inputsIn);
    bonusType = recipe.getBonusType(inputsIn);
  }

  @Override
  public void update(float availableEnergy) {
    usedEnergy += availableEnergy;
  }

  @Override
  public boolean isComplete() {
    return usedEnergy >= requiredEnergy;
  }

  @Override
  public float getProgress() {
    return MathHelper.clamp(usedEnergy / requiredEnergy, 0, 1);
  }

  @Override
  public @Nonnull ResultStack[] getCompletedResult() {
    return recipe.getCompletedResult(chance, inputs);
  }

  @Override
  public @Nonnull NNList<MachineRecipeInput> getInputs() {
    return inputs;
  }

  public void setInputs(@Nonnull NNList<MachineRecipeInput> inputs) {
    this.inputs = inputs;
  }

  @Override
  public float getRequiredEnergy() {
    return requiredEnergy;
  }

  public void setRequiredEnergy(float requiredEnergy) {
    this.requiredEnergy = requiredEnergy;
  }

  @Override
  public float getChance() {
    return chance;
  }

  public void setChance(float chance) {
    this.chance = chance;
  }

  @Override
  public @Nonnull RecipeBonusType getBonusType() {
    return bonusType;
  }

  @Override
  public void writeToNBT(@Nonnull NBTTagCompound nbtRoot) {
    NBTTagList inputItems = new NBTTagList();
    for (MachineRecipeInput ri : inputs) {
      NBTTagCompound stackRoot = new NBTTagCompound();
      ri.writeToNbt(stackRoot);
      inputItems.appendTag(stackRoot);
    }

    nbtRoot.setTag(KEY_INPUT_STACKS, inputItems);

    nbtRoot.setString(KEY_RECIPE, recipe.getUid());
    nbtRoot.setFloat(KEY_USED_ENERGY, usedEnergy);

    nbtRoot.setFloat(KEY_CHANCE, chance);
  }

  public static @Nullable IPoweredTask readFromNBT(@Nonnull NBTTagCompound nbtRoot) {
    IMachineRecipe recipe;

    float usedEnergy = nbtRoot.getFloat(KEY_USED_ENERGY);
    float chance = nbtRoot.getFloat(KEY_CHANCE);

    NBTTagList inputItems = (NBTTagList) nbtRoot.getTag(KEY_INPUT_STACKS);

    NNList<MachineRecipeInput> ins = new NNList<MachineRecipeInput>();
    for (int i = 0; i < inputItems.tagCount(); i++) {
      NBTTagCompound stackTag = inputItems.getCompoundTagAt(i);
      MachineRecipeInput mi = MachineRecipeInput.readFromNBT(stackTag);
      ins.add(mi);
    }

    String uid = nbtRoot.getString(KEY_RECIPE);
    recipe = MachineRecipeRegistry.instance.getRecipeForUid(uid);
    if (recipe != null) {
      return new PoweredTask(recipe, usedEnergy, chance, ins);
    }
    return null;

  }

  @Override
  public IMachineRecipe getRecipe() {
    return recipe;
  }

}
