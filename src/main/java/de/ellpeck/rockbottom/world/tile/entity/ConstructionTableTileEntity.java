package de.ellpeck.rockbottom.world.tile.entity;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.data.set.DataSet;
import de.ellpeck.rockbottom.api.inventory.CombinedInventory;
import de.ellpeck.rockbottom.api.item.Item;
import de.ellpeck.rockbottom.api.tile.entity.IFilteredInventory;
import de.ellpeck.rockbottom.api.tile.entity.IToolStation;
import de.ellpeck.rockbottom.api.tile.entity.TileEntity;
import de.ellpeck.rockbottom.api.tile.entity.TileInventory;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;

public class ConstructionTableTileEntity extends TileEntity implements IToolStation {

    private final TileInventory chiselSlot = new TileInventory(this, inst -> inst != null && inst.getItem() == GameContent.Items.CHISEL);
    private final TileInventory hammerSlot = new TileInventory(this, inst -> inst != null && inst.getItem() == GameContent.Items.HAMMER);
    private final TileInventory sawSlot = new TileInventory(this, inst -> inst != null && inst.getItem() == GameContent.Items.SAW);
    private final TileInventory malletSlot = new TileInventory(this, inst -> inst != null && inst.getItem() == GameContent.Items.MALLET);
    private final TileInventory wrenchSlot = new TileInventory(this, inst -> inst != null && inst.getItem() == GameContent.Items.WRENCH);
    private final CombinedInventory inventory = new CombinedInventory(this.chiselSlot, this.hammerSlot, this.sawSlot, this.malletSlot, this.wrenchSlot);

    public ConstructionTableTileEntity(IWorld world, int x, int y, TileLayer layer) {
        super(world, x, y, layer);
    }

    @Override
    public IFilteredInventory getTileInventory() {
        return this.inventory;
    }

    @Override
    public void update(IGameInstance game) {
        super.update(game);
    }

    @Override
    public int getToolSlot(Item tool) {
        if (tool == GameContent.Items.CHISEL) {
            return 0;
        } else if (tool == GameContent.Items.HAMMER) {
            return 1;
        } else if (tool == GameContent.Items.SAW) {
            return 2;
        } else if (tool == GameContent.Items.MALLET) {
            return 3;
        } else if (tool == GameContent.Items.WRENCH) {
            return 4;
        }
        return -1;
    }

    @Override
    public void save(DataSet set, boolean forSync) {
        if (!forSync) {
            this.inventory.save(set);
        }
    }

    @Override
    public void load(DataSet set, boolean forSync) {
        if (!forSync) {
            this.inventory.load(set);
        }
    }
}
