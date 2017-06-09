package de.ellpeck.rockbottom.game.world.entity.player;

import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.data.settings.Settings;
import de.ellpeck.rockbottom.api.entity.EntityItem;
import de.ellpeck.rockbottom.api.entity.player.AbstractEntityPlayer;
import de.ellpeck.rockbottom.api.entity.player.IInteractionManager;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.item.Item;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.item.ItemTile;
import de.ellpeck.rockbottom.api.item.ToolType;
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.world.TileLayer;
import de.ellpeck.rockbottom.game.RockBottom;
import de.ellpeck.rockbottom.game.net.packet.toserver.PacketBreakTile;
import de.ellpeck.rockbottom.game.net.packet.toserver.PacketHotbar;
import de.ellpeck.rockbottom.game.net.packet.toserver.PacketInteract;
import de.ellpeck.rockbottom.game.net.packet.toserver.PacketPlayerMovement;
import de.ellpeck.rockbottom.game.util.Util;
import org.lwjgl.input.Mouse;
import org.newdawn.slick.Input;

import java.util.Map;

public class InteractionManager implements IInteractionManager{

    public TileLayer breakingLayer;
    public int breakTileX;
    public int breakTileY;

    public float breakProgress;
    public int placeCooldown;

    public int mousedTileX;
    public int mousedTileY;

    public static boolean interact(AbstractEntityPlayer player, TileLayer layer, int x, int y, boolean simulate){
        Tile tileThere = player.world.getTile(layer, x, y);

        if(layer == TileLayer.MAIN){
            if(tileThere.onInteractWith(player.world, x, y, player)){
                return true;
            }
        }

        ItemInstance selected = player.getInv().get(player.getSelectedSlot());
        if(selected != null){
            Item item = selected.getItem();
            if(item instanceof ItemTile){
                if(layer != TileLayer.MAIN || player.world.getEntities(new BoundBox(x, y, x+1, y+1), entity -> !(entity instanceof EntityItem)).isEmpty()){
                    Tile tile = ((ItemTile)item).getTile();
                    if(tileThere.canReplace(player.world, x, y, layer, tile)){
                        if(tile.canPlace(player.world, x, y, layer)){

                            if(!simulate){
                                tile.doPlace(player.world, x, y, layer, selected, player);
                                player.getInv().remove(player.getSelectedSlot(), 1);
                            }

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void moveAndSend(EntityPlayer player, int type){
        player.move(type);

        if(RockBottomAPI.getNet().isClient()){
            RockBottomAPI.getNet().sendToServer(new PacketPlayerMovement(player.getUniqueId(), type));
        }
    }

    public void update(RockBottom game){
        if(game.isInWorld()){
            EntityPlayer player = game.getPlayer();
            Gui gui = game.getGuiManager().getGui();
            Settings settings = game.getSettings();

            if(gui == null && !player.isDead()){
                Input input = game.getContainer().getInput();
                double mouseX = input.getMouseX();
                double mouseY = input.getMouseY();

                double worldAtScreenX = player.x-game.getWidthInWorld()/2;
                double worldAtScreenY = -player.y-game.getHeightInWorld()/2;
                this.mousedTileX = Util.floor(worldAtScreenX+mouseX/(double)settings.renderScale);
                this.mousedTileY = -Util.floor(worldAtScreenY+mouseY/(double)settings.renderScale);

                if(input.isKeyDown(settings.keyLeft.key)){
                    moveAndSend(player, 0);
                }
                else if(input.isKeyDown(settings.keyRight.key)){
                    moveAndSend(player, 1);
                }

                if(input.isKeyDown(settings.keyJump.key)){
                    moveAndSend(player, 2);
                }

                if(player.world.isPosLoaded(this.mousedTileX, this.mousedTileY)){
                    TileLayer layer = input.isKeyDown(settings.keyBackground.key) ? TileLayer.BACKGROUND : TileLayer.MAIN;

                    if(input.isMouseButtonDown(settings.buttonDestroy)){
                        if(this.breakTileX != this.mousedTileX || this.breakTileY != this.mousedTileY){
                            this.breakProgress = 0;
                        }

                        Tile tile = player.world.getTile(layer, this.mousedTileX, this.mousedTileY);
                        if(tile.canBreak(player.world, this.mousedTileX, this.mousedTileY, layer)){
                            float hardness = tile.getHardness(player.world, this.mousedTileX, this.mousedTileY, layer);
                            float progressAmount = 0.05F/hardness;

                            ItemInstance selected = player.getInv().get(player.getSelectedSlot());
                            boolean effective = this.isToolEffective(player, selected, tile, layer, this.mousedTileX, this.mousedTileY);
                            if(selected != null){
                                progressAmount *= selected.getItem().getMiningSpeed(player.world, this.mousedTileX, this.mousedTileY, layer, tile, effective);
                            }

                            this.breakProgress += progressAmount;

                            if(this.breakProgress >= 1){
                                this.breakProgress = 0;

                                if(RockBottomAPI.getNet().isClient()){
                                    RockBottomAPI.getNet().sendToServer(new PacketBreakTile(player.getUniqueId(), layer, this.mousedTileX, this.mousedTileY));
                                }
                                else{
                                    tile.doBreak(game.getWorld(), this.mousedTileX, this.mousedTileY, layer, player, effective);
                                }
                            }
                            else{
                                this.breakTileX = this.mousedTileX;
                                this.breakTileY = this.mousedTileY;
                                this.breakingLayer = layer;
                            }
                        }
                        else{
                            this.breakProgress = 0;
                        }
                    }
                    else{
                        this.breakProgress = 0;
                    }

                    if(this.placeCooldown <= 0){
                        if(input.isMouseButtonDown(settings.buttonPlace)){
                            boolean client = RockBottomAPI.getNet().isClient();

                            if(interact(player, layer, this.mousedTileX, this.mousedTileY, client)){
                                if(client){
                                    RockBottomAPI.getNet().sendToServer(new PacketInteract(player.getUniqueId(), layer, this.mousedTileX, this.mousedTileY));
                                }

                                this.placeCooldown = 5;
                            }
                        }
                    }
                    else{
                        this.placeCooldown--;
                    }
                }

                boolean slotChange = false;

                int scroll = Mouse.getDWheel();
                if(scroll < 0){
                    player.setSelectedSlot(player.getSelectedSlot()+1);
                    if(player.getSelectedSlot() >= 8){
                        player.setSelectedSlot(0);
                    }
                    slotChange = true;
                }
                else if(scroll > 0){
                    player.setSelectedSlot(player.getSelectedSlot()-1);
                    if(player.getSelectedSlot() < 0){
                        player.setSelectedSlot(7);
                    }
                    slotChange = true;
                }

                if(slotChange){
                    if(RockBottomAPI.getNet().isClient()){
                        RockBottomAPI.getNet().sendToServer(new PacketHotbar(player.getUniqueId(), player.getSelectedSlot()));
                    }
                }
            }
            else{
                this.breakProgress = 0;
            }
        }
    }

    public void onMouseAction(RockBottom game, int button){
        game.getGuiManager().onMouseAction(game, button, game.getMouseInGuiX(), game.getMouseInGuiY());
    }

    public void onKeyboardAction(RockBottom game, int button, char character){
        if(!game.getGuiManager().onKeyboardAction(game, button, character)){
            if(game.isInWorld() && game.getGuiManager().getGui() == null){
                for(int i = 0; i < game.getSettings().keysItemSelection.length; i++){
                    if(button == game.getSettings().keysItemSelection[i]){
                        game.getPlayer().setSelectedSlot(i);

                        if(RockBottomAPI.getNet().isClient()){
                            RockBottomAPI.getNet().sendToServer(new PacketHotbar(game.getPlayer().getUniqueId(), i));
                        }

                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isToolEffective(AbstractEntityPlayer player, ItemInstance instance, Tile tile, TileLayer layer, int x, int y){
        if(instance != null){
            Map<ToolType, Integer> tools = instance.getItem().getToolTypes(instance);
            if(!tools.isEmpty()){
                for(Map.Entry<ToolType, Integer> entry : tools.entrySet()){
                    if(tile.isToolEffective(player.world, x, y, layer, entry.getKey(), entry.getValue())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public TileLayer getBreakingLayer(){
        return this.breakingLayer;
    }

    @Override
    public int getBreakTileX(){
        return this.breakTileX;
    }

    @Override
    public int getBreakTileY(){
        return this.breakTileY;
    }

    @Override
    public float getBreakProgress(){
        return this.breakProgress;
    }

    @Override
    public int getPlaceCooldown(){
        return this.placeCooldown;
    }

    @Override
    public int getMousedTileX(){
        return this.mousedTileX;
    }

    @Override
    public int getMousedTileY(){
        return this.mousedTileY;
    }
}