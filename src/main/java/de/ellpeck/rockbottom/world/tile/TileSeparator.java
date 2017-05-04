package de.ellpeck.rockbottom.world.tile;

import de.ellpeck.rockbottom.gui.GuiSeparator;
import de.ellpeck.rockbottom.gui.container.ContainerSeparator;
import de.ellpeck.rockbottom.item.ItemInstance;
import de.ellpeck.rockbottom.render.tile.ITileRenderer;
import de.ellpeck.rockbottom.render.tile.SeparatorTileRenderer;
import de.ellpeck.rockbottom.world.TileLayer;
import de.ellpeck.rockbottom.world.World;
import de.ellpeck.rockbottom.world.entity.player.EntityPlayer;
import de.ellpeck.rockbottom.world.tile.entity.TileEntity;
import de.ellpeck.rockbottom.world.tile.entity.TileEntitySeparator;

public class TileSeparator extends TileBasic{

    public TileSeparator(int id){
        super(id, "separator");
    }

    @Override
    protected ITileRenderer createRenderer(String name){
        return new SeparatorTileRenderer(name);
    }

    @Override
    public boolean canProvideTileEntity(){
        return true;
    }

    @Override
    public TileEntity provideTileEntity(World world, int x, int y){
        return world.getMeta(x, y) == 1 ? new TileEntitySeparator(world, x, y) : null;
    }

    @Override
    public boolean canPlace(World world, int x, int y, TileLayer layer){
        return super.canPlace(world, x, y, layer) && world.getTile(x, y+1).canReplace(world, x, y+1, layer, this);
    }

    @Override
    public void doPlace(World world, int x, int y, TileLayer layer, ItemInstance instance, EntityPlayer placer){
        super.doPlace(world, x, y, layer, instance, placer);
        world.setTile(x, y+1, this);
    }

    @Override
    public int getPlacementMeta(World world, int x, int y, TileLayer layer, ItemInstance instance){
        return 1;
    }

    @Override
    public void doBreak(World world, int x, int y, TileLayer layer, EntityPlayer breaker, boolean isRightTool){
        if(world.getMeta(x, y) == 1){
            world.destroyTile(x, y+1, layer, breaker, false);
        }
        else{
            world.destroyTile(x, y-1, layer, breaker, false);
        }

        super.doBreak(world, x, y, layer, breaker, isRightTool);
    }

    @Override
    public boolean onInteractWith(World world, int x, int y, EntityPlayer player){
        TileEntitySeparator tile;

        if(world.getMeta(x, y) == 1){
            tile = world.getTileEntity(x, y, TileEntitySeparator.class);
        }
        else{
            tile = world.getTileEntity(x, y-1, TileEntitySeparator.class);
        }

        if(tile != null){
            player.openGuiContainer(new GuiSeparator(tile), new ContainerSeparator(player, tile));
            return true;
        }
        else{
            return false;
        }
    }
}