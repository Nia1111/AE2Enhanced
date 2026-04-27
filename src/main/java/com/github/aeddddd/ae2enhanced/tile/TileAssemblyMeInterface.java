package com.github.aeddddd.ae2enhanced.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class TileAssemblyMeInterface extends TileEntity implements IGridProxyable, ICraftingProvider {

    private BlockPos controllerPos;

    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        markDirty();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public AENetworkProxy getProxy() {
        TileAssemblyController controller = getController();
        if (controller != null) {
            return controller.getProxy();
        }
        return null;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {
    }

    // IGridHost
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        if (controllerPos == null || world == null) return null;
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof TileAssemblyController && ((TileAssemblyController) te).isFormed()) {
            TileAssemblyController controller = (TileAssemblyController) te;
            AENetworkProxy proxy = controller.getProxy();
            if (proxy != null) {
                return proxy.getNode();
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        if (controllerPos == null || world == null) return AECableType.NONE;
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof TileAssemblyController) {
            TileAssemblyController controller = (TileAssemblyController) te;
            if (controller.isFormed()) {
                return AECableType.SMART;
            }
        }
        return AECableType.NONE;
    }

    @Override
    public void securityBreak() {
        if (controllerPos != null && world != null) {
            TileEntity te = world.getTileEntity(controllerPos);
            if (te instanceof TileAssemblyController) {
                ((TileAssemblyController) te).disassemble();
            }
        }
    }

    // ICraftingMedium
    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        TileAssemblyController controller = getController();
        if (controller != null) {
            return controller.pushPattern(patternDetails, table);
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        TileAssemblyController controller = getController();
        if (controller != null) {
            return controller.isBusy();
        }
        return false;
    }

    // ICraftingProvider
    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        TileAssemblyController controller = getController();
        if (controller != null && controller.isMeInterfaceActive(pos)) {
            controller.provideCrafting(craftingTracker);
        }
    }

    public TileAssemblyController getController() {
        if (controllerPos == null || world == null) return null;
        TileEntity te = world.getTileEntity(controllerPos);
        return te instanceof TileAssemblyController ? (TileAssemblyController) te : null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("controllerX")) {
            controllerPos = new BlockPos(
                compound.getInteger("controllerX"),
                compound.getInteger("controllerY"),
                compound.getInteger("controllerZ")
            );
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (controllerPos != null) {
            compound.setInteger("controllerX", controllerPos.getX());
            compound.setInteger("controllerY", controllerPos.getY());
            compound.setInteger("controllerZ", controllerPos.getZ());
        }
        return compound;
    }
}
