package de.ellpeck.rockbottom.gui.container;

import de.ellpeck.rockbottom.api.gui.container.ItemContainer;
import de.ellpeck.rockbottom.api.util.reg.ResourceName;
import de.ellpeck.rockbottom.world.entity.player.EntityPlayer;

public class ContainerInventory extends ItemContainer{

    public static final ResourceName NAME = ResourceName.intern("inventory");

    public ContainerInventory(EntityPlayer player){
        super(player);
        this.addPlayerInventory(player, 0, 0);
    }

    @Override
    public ResourceName getName(){
        return NAME;
    }
}
