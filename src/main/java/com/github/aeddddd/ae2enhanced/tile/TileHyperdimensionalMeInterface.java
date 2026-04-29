package com.github.aeddddd.ae2enhanced.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileHyperdimensionalMeInterface extends TileEntity {

    private BlockPos controllerPos = null;

    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        markDirty();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("controllerX")) {
            controllerPos = new BlockPos(compound.getInteger("controllerX"), compound.getInteger("controllerY"), compound.getInteger("controllerZ"));
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
