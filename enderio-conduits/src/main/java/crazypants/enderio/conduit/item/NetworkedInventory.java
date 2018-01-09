package crazypants.enderio.conduit.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.RoundRobinIterator;

import crazypants.enderio.base.Log;
import crazypants.enderio.base.capability.ItemTools;
import crazypants.enderio.base.conduit.ConnectionMode;
import crazypants.enderio.base.config.Config;
import crazypants.enderio.base.filter.IItemFilter;
import crazypants.enderio.base.filter.ILimitedItemFilter;
import crazypants.enderio.base.filter.INetworkedInventory;
import crazypants.enderio.util.Prep;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public class NetworkedInventory implements INetworkedInventory {

  private static final boolean SIMULATE = true;
  private static final boolean EXECUTE = false;

  IItemConduit con;
  EnumFacing conDir;
  BlockPos location;
  EnumFacing inventorySide;

  List<Target> sendPriority = new ArrayList<Target>();
  RoundRobinIterator<Target> rrIter = new RoundRobinIterator<Target>(sendPriority);

  private int extractFromSlot = -1;

  int tickDeficit;

  // TODO Inventory
//  boolean inventoryPanel = false;

  World world;
  ItemConduitNetwork network;
  String invName;

  NetworkedInventory(@Nonnull ItemConduitNetwork network, @Nonnull IItemConduit con, @Nonnull EnumFacing conDir, @Nonnull IItemHandler inv, @Nonnull BlockPos location) {
    this.network = network;
    inventorySide = conDir.getOpposite();
    this.con = con;
    this.conDir = conDir;
    this.location = location;
    world = con.getBundle().getBundleworld();

    IBlockState bs = world.getBlockState(location);
    invName = bs.getBlock().getLocalizedName();
    
//    TileEntity te = world.getTileEntity(location);
//    if(te instanceof TileInventoryPanel) {
//      inventoryPanel = true;
//    }
  }

  public boolean hasTarget(@Nonnull IItemConduit conduit, @Nonnull EnumFacing dir) {
    for (Target t : sendPriority) {
      if(t.inv.con == conduit && t.inv.conDir == dir) {
        return true;
      }
    }
    return false;
  }

  boolean canExtract() {
    ConnectionMode mode = con.getConnectionMode(conDir);
    return mode == ConnectionMode.INPUT || mode == ConnectionMode.IN_OUT;
  }

  boolean canInsert() {
//    if(inventoryPanel) {
//      return false;
//    }
    ConnectionMode mode = con.getConnectionMode(conDir);
    return mode == ConnectionMode.OUTPUT || mode == ConnectionMode.IN_OUT;
  }

//  boolean isInventoryPanel() {
//    return inventoryPanel;
//  }

  boolean isSticky() {
    return con.getOutputFilter(conDir) != null && con.getOutputFilter(conDir).isValid() && con.getOutputFilter(conDir).isSticky();
  }

  int getPriority() {
    return con.getOutputPriority(conDir);
  }

  public void onTick() {
    if(tickDeficit > 0 || !canExtract() || !con.isExtractionRedstoneConditionMet(conDir)) {
      //do nothing     
    } else {
      transferItems();
    }

    tickDeficit--;
    if(tickDeficit < -1) {
      //Sleep for a second before checking again.
      tickDeficit = 20;
    }
  }

  private int nextSlot(int numSlots) {
    ++extractFromSlot;
    if(extractFromSlot >= numSlots || extractFromSlot < 0) {
      extractFromSlot = 0;
    }
    return extractFromSlot;
  }

  private void setNextStartingSlot(int slot) {
    extractFromSlot = slot;
    extractFromSlot--;
  }

  private boolean transferItems() {
    final IItemHandler inventory = getInventory();
    if (inventory == null) {
      return false;
    }
    final int numSlots = inventory.getSlots();
    if (numSlots < 1) {
      return false;
    }
    
    final int maxExtracted = con.getMaximumExtracted(conDir);
    final IItemFilter filter = con.getInputFilter(conDir);

    int slot = -1;
    final int slotChecksPerTick = Math.min(numSlots, ItemConduitNetwork.MAX_SLOT_CHECK_PER_TICK);
    for (int i = 0; i < slotChecksPerTick; i++) {
      slot = nextSlot(numSlots);
      ItemStack item = inventory.extractItem(slot, maxExtracted, SIMULATE);
      if (Prep.isValid(item)) {

        if (filter instanceof ILimitedItemFilter && ((ILimitedItemFilter) filter).isLimited()) {
          final int count = ((ILimitedItemFilter) filter).getMaxCountThatPassesFilter(this, item);
          if (count <= 0) { // doesn't pass filter
            item = Prep.getEmpty();
          } else if (count < Integer.MAX_VALUE) { // some limit
            final ItemStack stackInSlot = inventory.getStackInSlot(slot);
            if (stackInSlot.getCount() <= count) { // there's less than the limit in there
              item = Prep.getEmpty();
            } else if (stackInSlot.getCount() - item.getCount() < count) { // we are trying to extract more than allowed
              item = inventory.extractItem(slot, stackInSlot.getCount() - count, SIMULATE);
            }
          }
        } else if (filter != null && !filter.doesItemPassFilter(this, item)) {
          item = Prep.getEmpty();
        }

        if (Prep.isValid(item) && doTransfer(inventory, item, slot)) {
          setNextStartingSlot(slot);
          return true;
        }
      }
    }
    return false;
  }

  private boolean doTransfer(@Nonnull IItemHandler inventory, @Nonnull ItemStack extractedItem, int slot) {
    int numInserted = insertIntoTargets(extractedItem.copy());
    if(numInserted <= 0) {
      return false;
    }
    ItemStack extracted = inventory.extractItem(slot, numInserted, EXECUTE);
    if (Prep.isInvalid(extracted) || extracted.getCount() != numInserted || extracted.getItem() != extractedItem.getItem()) {
      Log.warn("NetworkedInventory.itemExtracted: Inserted " + numInserted + " " + extractedItem.getDisplayName() + " but only removed "
          + (Prep.isInvalid(extracted) ? "null" : extracted.getCount() + " " + extracted.getDisplayName()) + " from " + inventory + " at " + location);
    }
    onItemExtracted(slot, numInserted);
    return true;
  }

  public void onItemExtracted(int slot, int numInserted) {
    con.itemsExtracted(numInserted, slot);
    tickDeficit = Math.round(numInserted * con.getTickTimePerItem(conDir));
  }

  int insertIntoTargets(@Nonnull ItemStack toExtract) {
    if (Prep.isInvalid(toExtract)) {
      return 0;
    }

    int totalToInsert = toExtract.getCount();
    int leftToInsert = totalToInsert;
    boolean matchedStickyInput = false;

    Iterable<Target> targets = getTargetIterator();

    //for (Target target : sendPriority) {
    for (Target target : targets) {
      if(target.stickyInput && !matchedStickyInput) {
        IItemFilter of = target.inv.con.getOutputFilter(target.inv.conDir);
        matchedStickyInput = of != null && of.isValid() && of.doesItemPassFilter(this, toExtract);
      }
      if(target.stickyInput || !matchedStickyInput) {        
        int inserted = target.inv.insertItem(toExtract);
        if(inserted > 0) {
          toExtract.shrink(inserted);
          leftToInsert -= inserted;
        }
        if(leftToInsert <= 0) {
          return totalToInsert;
        }
      }
    }
    return totalToInsert - leftToInsert;
  }

  private Iterable<Target> getTargetIterator() {
    if(con.isRoundRobinEnabled(conDir)) {
      return rrIter;
    }
    return sendPriority;
  }

  private int insertItem(@Nonnull ItemStack item) {
    if (!canInsert() || Prep.isInvalid(item)) {
      return 0;
    }
    IItemFilter filter = con.getOutputFilter(conDir);
    if (filter instanceof ILimitedItemFilter && ((ILimitedItemFilter) filter).isLimited()) {
      final int count = ((ILimitedItemFilter) filter).getMaxCountThatPassesFilter(this, item);
      if (count <= 0) {
        return 0;
      } else {
        final int maxInsert = ItemTools.getInsertLimit(getInventory(), item, count);
        if (maxInsert <= 0) {
          return 0;
        } else if (maxInsert < item.getCount()) {
          item = item.copy();
          item.setCount(maxInsert);
        }
      }
    } else if (filter != null && !filter.doesItemPassFilter(this, item)) {
      return 0;
    }
    return ItemTools.doInsertItem(getInventory(), item);
  }

  void updateInsertOrder() {
    sendPriority.clear();
    if(!canExtract()) {
      return;
    }
    List<Target> result = new ArrayList<NetworkedInventory.Target>();

    for (NetworkedInventory other : network.inventories) {
      if((con.isSelfFeedEnabled(conDir) || (other != this))
          && other.canInsert()
          && con.getInputColor(conDir) == other.con.getOutputColor(other.conDir)) {

        if(Config.itemConduitUsePhyscialDistance) {
          sendPriority.add(new Target(other, distanceTo(other), other.isSticky(), other.getPriority()));
        } else {
          result.add(new Target(other, 9999999, other.isSticky(), other.getPriority()));
        }
      }
    }

    if(Config.itemConduitUsePhyscialDistance) {
      Collections.sort(sendPriority);
    } else {
      if(!result.isEmpty()) {
        Map<BlockPos, Integer> visited = new HashMap<BlockPos, Integer>();
        List<BlockPos> steps = new ArrayList<BlockPos>();
        steps.add(con.getBundle().getLocation());
        calculateDistances(result, visited, steps, 0);

        sendPriority.addAll(result);

        Collections.sort(sendPriority);
      }
    }

  }

  private void calculateDistances(@Nonnull List<Target> targets, @Nonnull Map<BlockPos, Integer> visited, @Nonnull List<BlockPos> steps, int distance) {
    if(steps == null || steps.isEmpty()) {
      return;
    }

    ArrayList<BlockPos> nextSteps = new ArrayList<BlockPos>();
    for (BlockPos pos : steps) {
      IItemConduit con1 = network.conMap.get(pos);
      if (con1 != null) {
        for (EnumFacing dir : con1.getExternalConnections()) {
          Target target = getTarget(targets, con1, dir);
          if(target != null && target.distance > distance) {
            target.distance = distance;
          }
        }

        if(!visited.containsKey(pos)) {
          visited.put(pos, distance);
        } else {
          int prevDist = visited.get(pos);
          if(prevDist <= distance) {
            continue;
          }
          visited.put(pos, distance);
        }

        for (EnumFacing dir : con1.getConduitConnections()) {
          nextSteps.add(pos.offset(dir));
        }
      }
    }
    calculateDistances(targets, visited, nextSteps, distance + 1);
  }

  private Target getTarget(@Nonnull List<Target> targets, @Nonnull IItemConduit con1, @Nonnull EnumFacing dir) {
    if (targets == null || con1 == null || con1.getBundle().getLocation() == null) {
      return null;
    }
    for (Target target : targets) {
      BlockPos targetConLoc = null;
      if(target != null && target.inv != null && target.inv.con != null) {
        targetConLoc = target.inv.con.getBundle().getLocation();
        if (targetConLoc != null && target.inv.conDir == dir && targetConLoc.equals(con1.getBundle().getLocation())) {
          return target;
        }
      }
    }
    return null;
  }

  private int distanceTo(NetworkedInventory other) {
    // TODO Check if this should be a double or int
    return (int) con.getBundle().getLocation().distanceSq(other.con.getBundle().getLocation());
  }

  public @Nullable IItemHandler getInventory() {
    return ItemTools.getExternalInventory(world, location, inventorySide);
  }

  public EnumFacing getInventorySide() {
    return inventorySide;
  }

  public void setInventorySide(EnumFacing inventorySide) {
    this.inventorySide = inventorySide;
  }

  static class Target implements Comparable<Target> {
    NetworkedInventory inv;
    int distance;
    boolean stickyInput;
    int priority;

    Target(@Nonnull NetworkedInventory inv, int distance, boolean stickyInput, int priority) {
      this.inv = inv;
      this.distance = distance;
      this.stickyInput = stickyInput;
      this.priority = priority;
    }

    @Override
    public int compareTo(Target o) {
      if(stickyInput && !o.stickyInput) {
        return -1;
      }
      if(!stickyInput && o.stickyInput) {
        return 1;
      }
      if(priority != o.priority) {
        return ItemConduitNetwork.compare(o.priority, priority);
      }
      return ItemConduitNetwork.compare(distance, o.distance);
    }

  }

  public String getLocalizedInventoryName() {
    return invName;
  }

  public boolean isAt(BlockPos pos) {
    return location != null && pos != null && location.equals(pos);
  }
}
