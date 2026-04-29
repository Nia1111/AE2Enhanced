package com.github.aeddddd.ae2enhanced.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileHyperdimensionalController extends TileEntity {

    private boolean formed = false;

    public boolean isFormed() {
        return formed;
    }

    public void assemble() {
        if (!formed) {
            formed = true;
            markDirty();
            // P0: 后续补充存储初始化逻辑
        }
    }

    public void disassemble() {
        if (formed) {
            formed = false;
            markDirty();
            // P0: 后续补充存储释放逻辑
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        formed = compound.getBoolean("formed");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("formed", formed);
        return compound;
    }
}
