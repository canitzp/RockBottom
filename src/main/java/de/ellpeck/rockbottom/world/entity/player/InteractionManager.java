package de.ellpeck.rockbottom.world.entity.player;

import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.data.settings.Settings;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.entity.player.AbstractEntityPlayer;
import de.ellpeck.rockbottom.api.entity.player.IInteractionManager;
import de.ellpeck.rockbottom.api.event.EventResult;
import de.ellpeck.rockbottom.api.event.impl.AddBreakProgressEvent;
import de.ellpeck.rockbottom.api.event.impl.BreakEvent;
import de.ellpeck.rockbottom.api.event.impl.InteractionEvent;
import de.ellpeck.rockbottom.api.event.impl.LayerActionEvent;
import de.ellpeck.rockbottom.api.event.impl.LayerActionEvent.Type;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.item.Item;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.item.ToolType;
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Direction;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;
import de.ellpeck.rockbottom.init.RockBottom;
import de.ellpeck.rockbottom.net.packet.toserver.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InteractionManager implements IInteractionManager{

    public TileLayer breakingLayer;
    public int breakTileX;
    public int breakTileY;

    public float breakProgress;
    public int placeCooldown;
    public int attackCooldown;

    public static boolean interact(AbstractEntityPlayer player, TileLayer inputLayer, double mouseX, double mouseY){
        List<Entity> entities = player.world.getEntities(new BoundBox(mouseX, mouseY, mouseX, mouseY).expand(0.01F));

        InteractionEvent event = new InteractionEvent(player, entities, inputLayer, Util.floor(mouseX), Util.floor(mouseY), mouseX, mouseY);
        if(RockBottomAPI.getEventHandler().fireEvent(event) != EventResult.CANCELLED){
            TileLayer layer = event.layer;
            int x = event.x;
            int y = event.y;

            List<InteractionInfo> interactions = new ArrayList<>();

            for(Entity entity : entities){
                if(player.isInRange(mouseX, mouseY, entity.getMaxInteractionDistance(player.world, mouseX, mouseY, player))){
                    interactions.add(new InteractionInfo(() -> entity.onInteractWith(player, mouseX, mouseY), entity.getInteractionPriority(player, mouseX, mouseY)));
                }
            }

            Tile tile = player.world.getState(layer, x, y).getTile();
            if(player.isInRange(mouseX, mouseY, tile.getMaxInteractionDistance(player.world, x, y, layer, mouseX, mouseY, player))){
                interactions.add(new InteractionInfo(() -> tile.onInteractWith(player.world, x, y, layer, mouseX, mouseY, player), tile.getInteractionPriority(player.world, x, y, layer, mouseX, mouseY, player)));
            }

            ItemInstance selected = player.getInv().get(player.getSelectedSlot());
            if(selected != null){
                Item item = selected.getItem();
                if(player.isInRange(mouseX, mouseY, item.getMaxInteractionDistance(player.world, x, y, layer, mouseX, mouseY, player))){
                    interactions.add(new InteractionInfo(() -> item.onInteractWith(player.world, x, y, layer, mouseX, mouseY, player, selected), item.getInteractionPriority(player.world, x, y, layer, mouseX, mouseY, player, selected)));
                }
            }

            interactions.sort(Comparator.comparingInt(InteractionInfo:: getPriority).reversed());
            for(InteractionInfo info : interactions){
                if(info.interaction.get()){
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean attackEntity(AbstractEntityPlayer player, double mouseX, double mouseY){
        List<Entity> entities = player.world.getEntities(new BoundBox(mouseX, mouseY, mouseX, mouseY).expand(0.01F));
        for(Entity entity : entities){
            if(player.isInRange(mouseX, mouseY, entity.getMaxInteractionDistance(player.world, mouseX, mouseY, player))){
                if(entity.onAttack(player, mouseX, mouseY)){
                    return true;
                }
            }
        }
        return false;
    }

    public static void breakTile(Tile tile, AbstractEntityPlayer player, int x, int y, TileLayer layer, boolean effective){
        BreakEvent event = new BreakEvent(player, layer, x, y, effective);
        if(RockBottomAPI.getEventHandler().fireEvent(event) != EventResult.CANCELLED){
            layer = event.layer;
            x = event.x;
            y = event.y;
            effective = event.effective;

            tile.doBreak(player.world, x, y, layer, player, effective, true);
        }
    }

    private static void moveAndSend(EntityPlayer player, int type){
        if(player.move(type)){
            if(RockBottomAPI.getNet().isClient()){
                RockBottomAPI.getNet().sendToServer(new PacketPlayerMovement(player.getUniqueId(), player.x, player.y, player.motionX, player.motionY, player.facing));
            }
        }
    }

    public static boolean defaultTileBreakingCheck(IWorld world, int x, int y, TileLayer layer, double mouseX, double mouseY, AbstractEntityPlayer player){
        if(player.isInRange(mouseX, mouseY, world.getState(layer, x, y).getTile().getMaxInteractionDistance(world, x, y, layer, mouseX, mouseY, player))){
            if(layer == TileLayer.MAIN){
                return true;
            }
            else if(layer == TileLayer.BACKGROUND){
                if(!world.getState(x, y).getTile().isFullTile()){
                    for(Direction dir : Direction.ADJACENT){
                        Tile other = world.getState(layer, x+dir.x, y+dir.y).getTile();
                        if(!other.isFullTile()){
                            return true;
                        }
                    }
                }
            }
            else{
                return true;
            }
        }
        return false;
    }

    public static boolean defaultTilePlacementCheck(IWorld world, int x, int y, TileLayer layer, Tile tile){
        if(!tile.canPlaceInLayer(layer)){
            return false;
        }

        for(TileLayer testLayer : TileLayer.getLayersByInteractionPrio()){
            for(Direction dir : Direction.ADJACENT_INCLUDING_NONE){
                Tile other = world.getState(testLayer, x+dir.x, y+dir.y).getTile();
                if(!other.isAir()){
                    return true;
                }
            }
        }
        return false;

    }

    public static boolean isToolEffective(AbstractEntityPlayer player, ItemInstance instance, Tile tile, TileLayer layer, int x, int y){
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

    public void update(RockBottom game){
        if(game.getWorld() != null){
            EntityPlayer player = game.getPlayer();
            Gui gui = game.getGuiManager().getGui();

            if(gui == null && !player.isDead()){
                if(this.placeCooldown > 0){
                    this.placeCooldown--;
                }

                if(this.attackCooldown > 0){
                    this.attackCooldown--;
                }

                if(Settings.KEY_LEFT.isDown()){
                    moveAndSend(player, 0);
                }
                else if(Settings.KEY_RIGHT.isDown()){
                    moveAndSend(player, 1);
                }

                if(Settings.KEY_UP.isDown()){
                    moveAndSend(player, 3);
                }
                else if(Settings.KEY_DOWN.isDown()){
                    moveAndSend(player, 4);
                }

                if(Settings.KEY_JUMP.isDown()){
                    moveAndSend(player, 2);
                }

                double mousedTileX = game.getRenderer().getMousedTileX();
                double mousedTileY = game.getRenderer().getMousedTileY();

                int x = Util.floor(mousedTileX);
                int y = Util.floor(mousedTileY);

                if(player.world.isPosLoaded(x, y)){
                    boolean didBreakProgress = false;
                    boolean attacked = false;

                    if(this.breakTileX != x || this.breakTileY != y){
                        this.breakProgress = 0;
                    }

                    if(Settings.KEY_DESTROY.isDown()){
                        if(this.attackCooldown <= 0 && attackEntity(player, mousedTileX, mousedTileY)){
                            if(RockBottomAPI.getNet().isClient()){
                                RockBottomAPI.getNet().sendToServer(new PacketAttack(player.getUniqueId(), mousedTileX, mousedTileY));
                            }

                            this.attackCooldown = 40;
                            attacked = true;
                        }
                    }

                    if(!attacked){
                        for(TileLayer layer : TileLayer.getLayersByInteractionPrio()){
                            EventResult breakResult = RockBottomAPI.getEventHandler().fireEvent(new LayerActionEvent(Type.BREAK, player.world, layer, mousedTileX, mousedTileY));
                            if(breakResult != EventResult.CANCELLED && (breakResult == EventResult.MODIFIED || layer.canEditLayer(game, player))){
                                if(Settings.KEY_DESTROY.isDown()){
                                    Tile tile = player.world.getState(layer, x, y).getTile();
                                    if(defaultTileBreakingCheck(player.world, x, y, layer, mousedTileX, mousedTileY, player) && tile.canBreak(player.world, x, y, layer)){
                                        float hardness = tile.getHardness(player.world, x, y, layer);
                                        float progressAmount = 0.05F/hardness;

                                        ItemInstance selected = player.getInv().get(player.getSelectedSlot());
                                        boolean effective = isToolEffective(player, selected, tile, layer, x, y);
                                        if(selected != null){
                                            progressAmount *= selected.getItem().getMiningSpeed(player.world, x, y, layer, tile, effective);
                                        }

                                        AddBreakProgressEvent event = new AddBreakProgressEvent(player, layer, x, y, this.breakProgress, progressAmount);
                                        RockBottomAPI.getEventHandler().fireEvent(event);
                                        this.breakProgress = event.totalProgress;
                                        progressAmount = event.progressAdded;

                                        this.breakProgress += progressAmount;
                                        didBreakProgress = true;

                                        if(this.breakProgress >= 1){
                                            this.breakProgress = 0;

                                            if(RockBottomAPI.getNet().isClient()){
                                                RockBottomAPI.getNet().sendToServer(new PacketBreakTile(player.getUniqueId(), layer, mousedTileX, mousedTileY));
                                            }
                                            else{
                                                breakTile(tile, player, x, y, layer, effective);
                                            }
                                            break;
                                        }
                                        else{
                                            this.breakTileX = x;
                                            this.breakTileY = y;
                                            this.breakingLayer = layer;
                                        }

                                        break;
                                    }
                                }
                            }

                            EventResult placeResult = RockBottomAPI.getEventHandler().fireEvent(new LayerActionEvent(Type.BREAK, player.world, layer, mousedTileX, mousedTileY));
                            if(placeResult != EventResult.CANCELLED && (placeResult == EventResult.MODIFIED || layer.canEditLayer(game, player))){
                                if(this.placeCooldown <= 0){
                                    if(Settings.KEY_PLACE.isDown()){
                                        if(interact(player, layer, mousedTileX, mousedTileY)){
                                            if(RockBottomAPI.getNet().isClient()){
                                                RockBottomAPI.getNet().sendToServer(new PacketInteract(player.getUniqueId(), layer, mousedTileX, mousedTileY));
                                            }

                                            this.placeCooldown = 10;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if(!didBreakProgress){
                        this.breakProgress = 0;
                    }
                }
                else{
                    this.breakProgress = 0;
                }

                boolean slotChange = false;
                int slot = player.getSelectedSlot();

                int scroll = game.getInput().getMouseWheelChange();
                int scrollHor = game.getInput().getHorizontalMouseWheelChange();

                if(scroll < 0 || scrollHor < 0){
                    slot++;
                    if(slot >= 8){
                        slot = 0;
                    }
                    slotChange = true;
                }
                else if(scroll > 0 || scrollHor > 0){
                    slot--;
                    if(slot < 0){
                        slot = 7;
                    }
                    slotChange = true;
                }

                if(slotChange){
                    player.setSelectedSlot(slot);

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

    public boolean onMouseAction(RockBottom game, int button){
        return game.getGuiManager().onMouseAction(game, button, game.getRenderer().getMouseInGuiX(), game.getRenderer().getMouseInGuiY());
    }

    public boolean onKeyPressed(RockBottom game, int button){
        if(game.getGuiManager().onKeyPressed(game, button)){
            return true;
        }
        else{
            if(game.getWorld() != null && game.getGuiManager().getGui() == null){
                for(int i = 0; i < Settings.KEYS_ITEM_SELECTION.length; i++){
                    if(Settings.KEYS_ITEM_SELECTION[i].isKey(button)){
                        game.getPlayer().setSelectedSlot(i);

                        if(RockBottomAPI.getNet().isClient()){
                            RockBottomAPI.getNet().sendToServer(new PacketHotbar(game.getPlayer().getUniqueId(), i));
                        }

                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean onCharInput(RockBottom game, int codePoint, char[] characters){
        return game.getGuiManager().onCharInput(game, codePoint, characters);
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

    private static class InteractionInfo{

        private final Supplier<Boolean> interaction;
        private final int priority;

        public InteractionInfo(Supplier<Boolean> interaction, int priority){
            this.interaction = interaction;
            this.priority = priority;
        }

        public int getPriority(){
            return this.priority;
        }
    }
}
