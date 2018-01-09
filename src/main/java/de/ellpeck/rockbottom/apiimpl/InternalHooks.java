package de.ellpeck.rockbottom.apiimpl;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.data.settings.Settings;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.entity.EntityItem;
import de.ellpeck.rockbottom.api.entity.MovableWorldObject;
import de.ellpeck.rockbottom.api.entity.player.AbstractEntityPlayer;
import de.ellpeck.rockbottom.api.event.impl.WorldObjectCollisionEvent;
import de.ellpeck.rockbottom.api.gui.GuiContainer;
import de.ellpeck.rockbottom.api.gui.component.ComponentSlot;
import de.ellpeck.rockbottom.api.gui.component.GuiComponent;
import de.ellpeck.rockbottom.api.internal.IInternalHooks;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.tile.Tile;
import de.ellpeck.rockbottom.api.tile.TileLiquid;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;
import de.ellpeck.rockbottom.net.packet.toclient.PacketEntityUpdate;
import de.ellpeck.rockbottom.net.packet.toserver.PacketSlotModification;
import de.ellpeck.rockbottom.world.entity.player.InteractionManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InternalHooks implements IInternalHooks{

    @Override
    public void doDefaultEntityUpdate(Entity entity){
        if(!entity.isDead()){
            entity.applyMotion();

            entity.canClimb = false;
            entity.isClimbing = false;

            entity.move();

            if(entity.onGround || entity.isClimbing){
                if(entity.onGround){
                    entity.motionY = 0;
                }

                if(entity.isFalling){
                    double dist = entity.fallStartY-entity.y;
                    if(dist > 0){
                        entity.onGroundHit(dist);
                    }

                    entity.isFalling = false;
                    entity.fallStartY = 0;
                }
            }
            else if(entity.motionY < 0){
                if(!entity.isFalling){
                    entity.isFalling = true;
                    entity.fallStartY = entity.y;
                }
            }

            if(entity.collidedHor){
                entity.motionX = 0;
            }
        }
        else{
            entity.motionX = 0;
            entity.motionY = 0;
        }

        entity.ticksExisted++;

        if(entity.world.isServer()){
            if(entity.doesSync()){
                if(entity.ticksExisted%entity.getSyncFrequency() == 0){
                    if(entity.lastX != entity.x || entity.lastY != entity.y){
                        RockBottomAPI.getNet().sendToAllPlayersWithLoadedPosExcept(entity.world, new PacketEntityUpdate(entity.getUniqueId(), entity.x, entity.y, entity.motionX, entity.motionY, entity.facing), entity.x, entity.y, entity);

                        entity.lastX = entity.x;
                        entity.lastY = entity.y;
                    }
                }
            }
        }
    }

    @Override
    public void doWorldObjectMovement(MovableWorldObject object){
        if(object.motionX != 0 || object.motionY != 0){
            BoundBox ownBox = object.getBoundingBox();
            BoundBox tempBox = ownBox.copy().add(object.x, object.y);
            BoundBox tempBoxMotion = tempBox.copy().add(object.motionX, object.motionY);

            List<BoundBox> boxes = new ArrayList<>();

            for(int x = Util.floor(tempBoxMotion.getMinX()); x < Util.ceil(tempBoxMotion.getMaxX()); x++){
                for(int y = Util.floor(tempBoxMotion.getMinY()); y < Util.ceil(tempBoxMotion.getMaxY()); y++){
                    if(object.world.isPosLoaded(x, y)){
                        for(TileLayer layer : TileLayer.getAllLayers()){
                            TileState state = object.world.getState(layer, x, y);
                            List<BoundBox> tileBoxes = state.getTile().getBoundBoxes(object.world, x, y, layer, object, tempBox, tempBoxMotion);

                            if(layer.canCollide(object) && object.canCollideWithTile(state, x, y, layer)){
                                object.onTileCollision(x, y, layer, state, tempBox, tempBoxMotion, tileBoxes);
                                boxes.addAll(tileBoxes);
                            }

                            object.onTileIntersection(x, y, layer, state, tempBox, tempBoxMotion, tileBoxes);
                        }
                    }
                }
            }

            List<Entity> entities = object.world.getEntities(tempBoxMotion);
            for(Entity entity : entities){
                BoundBox entityTempBox = entity.getBoundingBox().copy().add(entity.x, entity.y);
                BoundBox entityTempBoxMotion = entityTempBox.copy().add(entity.motionX, entity.motionY);

                if(entity.canCollideWith(object, tempBox, tempBoxMotion)){
                    object.onEntityCollision(entity, tempBox, tempBoxMotion, entityTempBox, entityTempBoxMotion);
                    boxes.add(entityTempBox);
                }

                object.onEntityIntersection(entity, tempBox, tempBoxMotion, entityTempBox, entityTempBoxMotion);
            }

            RockBottomAPI.getEventHandler().fireEvent(new WorldObjectCollisionEvent(object, tempBoxMotion, boxes));

            double motionY = object.motionY;
            if(motionY != 0){
                if(!boxes.isEmpty()){
                    for(BoundBox box : boxes){
                        if(motionY != 0){
                            if(!box.isEmpty()){
                                motionY = box.getYDistanceWithMax(tempBox, motionY);
                            }
                        }
                        else{
                            break;
                        }
                    }
                }

                object.y += motionY;
            }

            double motionX = object.motionX;
            if(motionX != 0){
                if(!boxes.isEmpty()){
                    tempBox.set(ownBox).add(object.x, object.y);
                    for(BoundBox box : boxes){
                        if(motionX != 0){
                            if(!box.isEmpty()){
                                motionX = box.getXDistanceWithMax(tempBox, motionX);
                            }
                        }
                        else{
                            break;
                        }
                    }
                }

                object.x += motionX;
            }

            object.collidedHor = motionX != object.motionX;
            object.collidedVert = motionY != object.motionY;
            object.onGround = object.collidedVert && object.motionY < 0;
        }
    }

    @Override
    public boolean doDefaultSlotMovement(IGameInstance game, int button, float x, float y, ComponentSlot slot){
        ItemInstance slotInst = slot.slot.get();
        ItemInstance slotCopy = slotInst == null ? null : slotInst.copy();

        if(Settings.KEY_GUI_ACTION_1.isKey(button)){
            if(slot.container.holdingInst == null){
                if(slotCopy != null){
                    if(this.setToInv(null, slot)){
                        slot.container.holdingInst = slotCopy;

                        return true;
                    }
                }
            }
            else{
                if(slotCopy == null){
                    if(this.setToInv(slot.container.holdingInst, slot)){
                        slot.container.holdingInst = null;

                        return true;
                    }
                }
                else{
                    if(slotCopy.isEffectivelyEqual(slot.container.holdingInst)){
                        int possible = Math.min(slotCopy.getMaxAmount()-slotCopy.getAmount(), slot.container.holdingInst.getAmount());
                        if(possible > 0){
                            if(this.setToInv(slotCopy.addAmount(possible), slot)){
                                slot.container.holdingInst.removeAmount(possible);
                                if(slot.container.holdingInst.getAmount() <= 0){
                                    slot.container.holdingInst = null;
                                }

                                return true;
                            }
                        }
                    }
                    else{
                        ItemInstance copy = slot.container.holdingInst.copy();
                        if(this.setToInv(copy, slot)){
                            slot.container.holdingInst = slotCopy;

                            return true;
                        }
                    }
                }
            }
        }
        else if(Settings.KEY_GUI_ACTION_2.isKey(button)){
            if(slot.container.holdingInst == null){
                if(slotCopy != null){
                    int half = Util.ceil((double)slotCopy.getAmount()/2);
                    slotCopy.removeAmount(half);

                    if(this.setToInv(slotCopy.getAmount() <= 0 ? null : slotCopy, slot)){
                        slot.container.holdingInst = slotCopy.copy().setAmount(half);

                        return true;
                    }
                }
            }
            else{
                if(slotCopy == null){
                    if(this.setToInv(slot.container.holdingInst.copy().setAmount(1), slot)){
                        slot.container.holdingInst.removeAmount(1);
                        if(slot.container.holdingInst.getAmount() <= 0){
                            slot.container.holdingInst = null;
                        }

                        return true;
                    }
                }
                else if(slotCopy.isEffectivelyEqual(slot.container.holdingInst)){
                    if(slotCopy.getAmount() < slotCopy.getMaxAmount()){
                        if(this.setToInv(slotCopy.addAmount(1), slot)){
                            slot.container.holdingInst.removeAmount(1);
                            if(slot.container.holdingInst.getAmount() <= 0){
                                slot.container.holdingInst = null;
                            }

                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean doDefaultShiftClicking(IGameInstance game, GuiContainer gui, ComponentSlot slot){
        if(Settings.KEY_GUI_ACTION_1.isPressed()){
            if(slot.slot.canRemove()){
                ItemInstance remaining = slot.slot.get();

                if(remaining != null){
                    boolean modified = false;
                    ItemInstance remainingCopy = remaining.copy();

                    for(GuiContainer.ShiftClickBehavior behavior : gui.shiftClickBehaviors){
                        if(behavior.slots.contains(slot.componentId)){
                            for(int slotInto : behavior.slotsInto){
                                GuiComponent comp = gui.getComponents().get(slotInto);
                                if(comp instanceof ComponentSlot){
                                    ComponentSlot intoSlot = (ComponentSlot)comp;
                                    if(behavior.condition == null || behavior.condition.apply(slot.slot, intoSlot.slot)){
                                        ItemInstance existing = intoSlot.slot.get();

                                        if(existing == null){
                                            if(this.setToInv(remainingCopy, intoSlot)){
                                                this.setToInv(null, slot);
                                                return true;
                                            }
                                        }
                                        else if(existing.isEffectivelyEqual(remainingCopy)){
                                            int possible = Math.min(existing.getMaxAmount()-existing.getAmount(), remainingCopy.getAmount());
                                            if(possible > 0){
                                                if(this.setToInv(existing.copy().addAmount(possible), intoSlot)){
                                                    modified = true;

                                                    remainingCopy.removeAmount(possible);
                                                    if(remainingCopy.getAmount() <= 0){
                                                        this.setToInv(null, slot);
                                                        return true;
                                                    }
                                                    else{
                                                        this.setToInv(remainingCopy, slot);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return modified;
                }
            }
        }
        return false;
    }

    @Override
    public boolean placeTile(int x, int y, TileLayer layer, AbstractEntityPlayer player, ItemInstance selected, Tile tile, boolean removeItem, boolean simulate){
        if(layer != TileLayer.MAIN || player.world.getEntities(new BoundBox(x, y, x+1, y+1), entity -> !(entity instanceof EntityItem)).isEmpty()){
            if(layer.canTileBeInLayer(player.world, x, y, tile)){
                Tile tileThere = player.world.getState(layer, x, y).getTile();
                if(tileThere != tile && tileThere.canReplace(player.world, x, y, layer)){
                    if(InteractionManager.defaultTilePlacementCheck(player.world, x, y, layer, tile) && tile.canPlace(player.world, x, y, layer)){
                        if(!simulate){
                            tile.doPlace(player.world, x, y, layer, selected, player);

                            if(removeItem){
                                player.getInv().remove(player.getSelectedSlot(), 1);
                            }

                            TileState state = player.world.getState(layer, x, y);
                            if(state.getTile() == tile){
                                IResourceName sound = tile.getPlaceSound(player.world, x, y, layer, player, state);
                                if(sound != null){
                                    player.world.playSound(sound, x+0.5, y+0.5, layer.index(), 1F, 1F);
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void doDefaultLiquidBehavior(IWorld world, int x, int y, TileLayer layer, TileLiquid tile){
        TileState ourState = world.getState(layer, x, y);
        int ourLevel = ourState.get(tile.level)+1;
        if(!world.isPosLoaded(x, y-1)){
            return;
        }
        // Check down
        if(world.getState(x, y-1).getTile().canLiquidSpreadInto(world, x, y-1, tile)){
            TileState beneathState = world.getState(layer, x, y-1);
            if(beneathState.getTile() == tile){
                // Liquid beneath us
                int otherLevel = beneathState.get(tile.level)+1;
                int remaining = ourLevel-tile.getLevels()+otherLevel;
                if(remaining < tile.getLevels() && tile.getLevels()-otherLevel != 0){ // If more liquid can fit beneath us
                    if(remaining > 0){
                        // Transfer one unit to the liquid beneath us
                        world.setState(layer, x, y, ourState.prop(tile.level, remaining-1));
                        world.setState(layer, x, y-1, beneathState.prop(tile.level, tile.getLevels()-1));
                        return;
                    }
                    else{
                        // Transfer our last unit to the liquid beneath us and remove this liquid
                        world.setState(layer, x, y, GameContent.TILE_AIR.getDefState());
                        world.setState(layer, x, y-1, beneathState.prop(tile.level, otherLevel+ourLevel-1));
                        return;
                    }
                }
                else{
                    if(!world.isPosLoaded(x+1, y-1) || !world.isPosLoaded(x-1, y-1)){
                        return;
                    }
                    TileState leftState = world.getState(layer, x-1, y-1);
                    TileState rightState = world.getState(layer, x+1, y-1);
                    // Try to balance to the sides
                    if(Util.RANDOM.nextBoolean()){
                        if(this.transferDown(world, layer, ourState, leftState, x, y, -1, tile) || this.transferDown(world, layer, ourState, rightState, x, y, 1, tile)){
                            return;
                        }
                    }
                    else{
                        if(this.transferDown(world, layer, ourState, rightState, x, y, 1, tile) || this.transferDown(world, layer, ourState, leftState, x, y, -1, tile)){
                            return;
                        }
                    }
                }
            }
            else if(beneathState.getTile().isAir()){
                // Nothing beneath us move down
                world.setState(layer, x, y-1, ourState);
                world.setState(layer, x, y, GameContent.TILE_AIR.getDefState());
                return;
            }
            // Fall through to the balancing and spreading logic
        }

        // Balance and spread
        if(!world.isPosLoaded(x-1, y) || !world.isPosLoaded(x+1, y)){
            return;
        }
        TileState leftState = world.getState(layer, x-1, y);
        TileState rightState = world.getState(layer, x+1, y);
        boolean oneToSpare;
        if(Util.RANDOM.nextBoolean()){
            // Left first
            oneToSpare = this.balanceAndSpread(world, layer, leftState, rightState, ourState, ourLevel, x, y, -1, false, tile);
            ourState = world.getState(layer, x, y);
            ourLevel = ourState.get(tile.level)+1;
            // Right second
            boolean found = this.balanceAndSpread(world, layer, rightState, leftState, ourState, ourLevel, x, y, 1, oneToSpare, tile);
            if(found != oneToSpare){ // We need to check left again
                this.balanceAndSpread(world, layer, leftState, rightState, ourState, ourLevel, x, y, -1, true, tile);
            }
        }
        else{
            // Right first
            oneToSpare = this.balanceAndSpread(world, layer, rightState, leftState, ourState, ourLevel, x, y, 1, false, tile);
            ourState = world.getState(layer, x, y);
            ourLevel = ourState.get(tile.level)+1;
            // Left second
            boolean found = this.balanceAndSpread(world, layer, leftState, rightState, ourState, ourLevel, x, y, -1, oneToSpare, tile);
            if(found != oneToSpare){ // We need to check right again
                this.balanceAndSpread(world, layer, rightState, leftState, ourState, ourLevel, x, y, 1, true, tile);
            }
        }

    }

    @Override
    public String getKeyOrMouseName(boolean isMouse, int key){
        if(isMouse){
            switch(key){
                case GLFW.GLFW_MOUSE_BUTTON_1:
                    return "LEFT MOUSE";
                case GLFW.GLFW_MOUSE_BUTTON_2:
                    return "RIGHT MOUSE";
                case GLFW.GLFW_MOUSE_BUTTON_3:
                    return "MIDDLE MOUSE";
                case GLFW.GLFW_MOUSE_BUTTON_4:
                    return "MOUSE 4";
                case GLFW.GLFW_MOUSE_BUTTON_5:
                    return "MOUSE 5";
                case GLFW.GLFW_MOUSE_BUTTON_6:
                    return "MOUSE 6";
                case GLFW.GLFW_MOUSE_BUTTON_7:
                    return "MOUSE 7";
                case GLFW.GLFW_MOUSE_BUTTON_8:
                    return "MOUSE 8";
                default:
                    return "UNKNOWN";
            }
        }
        else{
            switch(key){
                case GLFW.GLFW_KEY_A:
                    return "A";
                case GLFW.GLFW_KEY_B:
                    return "B";
                case GLFW.GLFW_KEY_C:
                    return "C";
                case GLFW.GLFW_KEY_D:
                    return "D";
                case GLFW.GLFW_KEY_E:
                    return "E";
                case GLFW.GLFW_KEY_F:
                    return "F";
                case GLFW.GLFW_KEY_G:
                    return "G";
                case GLFW.GLFW_KEY_H:
                    return "H";
                case GLFW.GLFW_KEY_I:
                    return "I";
                case GLFW.GLFW_KEY_J:
                    return "J";
                case GLFW.GLFW_KEY_K:
                    return "K";
                case GLFW.GLFW_KEY_L:
                    return "L";
                case GLFW.GLFW_KEY_M:
                    return "M";
                case GLFW.GLFW_KEY_N:
                    return "N";
                case GLFW.GLFW_KEY_O:
                    return "O";
                case GLFW.GLFW_KEY_P:
                    return "P";
                case GLFW.GLFW_KEY_Q:
                    return "Q";
                case GLFW.GLFW_KEY_R:
                    return "R";
                case GLFW.GLFW_KEY_S:
                    return "S";
                case GLFW.GLFW_KEY_T:
                    return "T";
                case GLFW.GLFW_KEY_U:
                    return "U";
                case GLFW.GLFW_KEY_V:
                    return "V";
                case GLFW.GLFW_KEY_W:
                    return "W";
                case GLFW.GLFW_KEY_X:
                    return "X";
                case GLFW.GLFW_KEY_Y:
                    return "Y";
                case GLFW.GLFW_KEY_Z:
                    return "Z";
                case GLFW.GLFW_KEY_1:
                    return "1";
                case GLFW.GLFW_KEY_2:
                    return "2";
                case GLFW.GLFW_KEY_3:
                    return "3";
                case GLFW.GLFW_KEY_4:
                    return "4";
                case GLFW.GLFW_KEY_5:
                    return "5";
                case GLFW.GLFW_KEY_6:
                    return "6";
                case GLFW.GLFW_KEY_7:
                    return "7";
                case GLFW.GLFW_KEY_8:
                    return "8";
                case GLFW.GLFW_KEY_9:
                    return "9";
                case GLFW.GLFW_KEY_0:
                    return "0";
                case GLFW.GLFW_KEY_SPACE:
                    return "SPACE";
                case GLFW.GLFW_KEY_MINUS:
                    return "MINUS";
                case GLFW.GLFW_KEY_EQUAL:
                    return "EQUAL";
                case GLFW.GLFW_KEY_LEFT_BRACKET:
                    return "LEFT BRACKET";
                case GLFW.GLFW_KEY_RIGHT_BRACKET:
                    return "RIGHT BRACKET";
                case GLFW.GLFW_KEY_BACKSLASH:
                    return "BACKSLASH";
                case GLFW.GLFW_KEY_SEMICOLON:
                    return "SEMICOLON";
                case GLFW.GLFW_KEY_APOSTROPHE:
                    return "APOSTROPHE";
                case GLFW.GLFW_KEY_GRAVE_ACCENT:
                    return "GRAVE ACCENT";
                case GLFW.GLFW_KEY_COMMA:
                    return "COMMA";
                case GLFW.GLFW_KEY_PERIOD:
                    return "PERIOD";
                case GLFW.GLFW_KEY_SLASH:
                    return "SLASH";
                case GLFW.GLFW_KEY_WORLD_1:
                    return "WORLD 1";
                case GLFW.GLFW_KEY_WORLD_2:
                    return "WORLD 2";

                case GLFW.GLFW_KEY_ESCAPE:
                    return "ESCAPE";
                case GLFW.GLFW_KEY_F1:
                    return "F1";
                case GLFW.GLFW_KEY_F2:
                    return "F2";
                case GLFW.GLFW_KEY_F3:
                    return "F3";
                case GLFW.GLFW_KEY_F4:
                    return "F4";
                case GLFW.GLFW_KEY_F5:
                    return "F5";
                case GLFW.GLFW_KEY_F6:
                    return "F6";
                case GLFW.GLFW_KEY_F7:
                    return "F7";
                case GLFW.GLFW_KEY_F8:
                    return "F8";
                case GLFW.GLFW_KEY_F9:
                    return "F9";
                case GLFW.GLFW_KEY_F10:
                    return "F10";
                case GLFW.GLFW_KEY_F11:
                    return "F11";
                case GLFW.GLFW_KEY_F12:
                    return "F12";
                case GLFW.GLFW_KEY_F13:
                    return "F13";
                case GLFW.GLFW_KEY_F14:
                    return "F14";
                case GLFW.GLFW_KEY_F15:
                    return "F15";
                case GLFW.GLFW_KEY_F16:
                    return "F16";
                case GLFW.GLFW_KEY_F17:
                    return "F17";
                case GLFW.GLFW_KEY_F18:
                    return "F18";
                case GLFW.GLFW_KEY_F19:
                    return "F19";
                case GLFW.GLFW_KEY_F20:
                    return "F20";
                case GLFW.GLFW_KEY_F21:
                    return "F21";
                case GLFW.GLFW_KEY_F22:
                    return "F22";
                case GLFW.GLFW_KEY_F23:
                    return "F23";
                case GLFW.GLFW_KEY_F24:
                    return "F24";
                case GLFW.GLFW_KEY_F25:
                    return "F25";
                case GLFW.GLFW_KEY_UP:
                    return "UP";
                case GLFW.GLFW_KEY_DOWN:
                    return "DOWN";
                case GLFW.GLFW_KEY_LEFT:
                    return "LEFT";
                case GLFW.GLFW_KEY_RIGHT:
                    return "RIGHT";
                case GLFW.GLFW_KEY_LEFT_SHIFT:
                    return "LEFT SHIFT";
                case GLFW.GLFW_KEY_RIGHT_SHIFT:
                    return "RIGHT SHIFT";
                case GLFW.GLFW_KEY_LEFT_CONTROL:
                    return "LEFT CONTROL";
                case GLFW.GLFW_KEY_RIGHT_CONTROL:
                    return "RIGHT CONTROL";
                case GLFW.GLFW_KEY_LEFT_ALT:
                    return "LEFT ALT";
                case GLFW.GLFW_KEY_RIGHT_ALT:
                    return "RIGHT ALT";
                case GLFW.GLFW_KEY_TAB:
                    return "TAB";
                case GLFW.GLFW_KEY_ENTER:
                    return "ENTER";
                case GLFW.GLFW_KEY_BACKSPACE:
                    return "BACKSPACE";
                case GLFW.GLFW_KEY_INSERT:
                    return "INSERT";
                case GLFW.GLFW_KEY_DELETE:
                    return "DELETE";
                case GLFW.GLFW_KEY_PAGE_UP:
                    return "PAGE UP";
                case GLFW.GLFW_KEY_PAGE_DOWN:
                    return "PAGE DOWN";
                case GLFW.GLFW_KEY_HOME:
                    return "HOME";
                case GLFW.GLFW_KEY_END:
                    return "END";
                case GLFW.GLFW_KEY_KP_0:
                    return "KEYPAD 0";
                case GLFW.GLFW_KEY_KP_1:
                    return "KEYPAD 1";
                case GLFW.GLFW_KEY_KP_2:
                    return "KEYPAD 2";
                case GLFW.GLFW_KEY_KP_3:
                    return "KEYPAD 3";
                case GLFW.GLFW_KEY_KP_4:
                    return "KEYPAD 4";
                case GLFW.GLFW_KEY_KP_5:
                    return "KEYPAD 5";
                case GLFW.GLFW_KEY_KP_6:
                    return "KEYPAD 6";
                case GLFW.GLFW_KEY_KP_7:
                    return "KEYPAD 7";
                case GLFW.GLFW_KEY_KP_8:
                    return "KEYPAD 8";
                case GLFW.GLFW_KEY_KP_9:
                    return "KEYPAD 9";
                case GLFW.GLFW_KEY_KP_DIVIDE:
                    return "KEYPAD DIVIDE";
                case GLFW.GLFW_KEY_KP_MULTIPLY:
                    return "KEYPAD MULTPLY";
                case GLFW.GLFW_KEY_KP_SUBTRACT:
                    return "KEYPAD SUBTRACT";
                case GLFW.GLFW_KEY_KP_ADD:
                    return "KEYPAD ADD";
                case GLFW.GLFW_KEY_KP_DECIMAL:
                    return "KEYPAD DECIMAL";
                case GLFW.GLFW_KEY_KP_EQUAL:
                    return "KEYPAD EQUAL";
                case GLFW.GLFW_KEY_KP_ENTER:
                    return "KEYPAD ENTER";
                case GLFW.GLFW_KEY_PRINT_SCREEN:
                    return "PRINT SCREEN";
                case GLFW.GLFW_KEY_NUM_LOCK:
                    return "NUM LOCK";
                case GLFW.GLFW_KEY_CAPS_LOCK:
                    return "CAPS LOCK";
                case GLFW.GLFW_KEY_SCROLL_LOCK:
                    return "SCROLL LOCK";
                case GLFW.GLFW_KEY_PAUSE:
                    return "PAUSE";
                case GLFW.GLFW_KEY_LEFT_SUPER:
                    return "LEFT SUPER";
                case GLFW.GLFW_KEY_RIGHT_SUPER:
                    return "RIGHT SUPER";
                case GLFW.GLFW_KEY_MENU:
                    return "MENU";

                default:
                    return "UNKNOWN";
            }
        }
    }

    // Direction: 1 = right, -1 = left
    private boolean balanceAndSpread(IWorld world, TileLayer layer, TileState otherState, TileState oppositeState, TileState ourState, int ourLevel, int x, int y, int direction, boolean oneToSpare, TileLiquid tile){
        if(world.getState(x+direction, y).getTile().canLiquidSpreadInto(world, x+direction, y, tile)){
            if(otherState.getTile() == tile){
                // Balance with left
                int otherLevel = otherState.get(tile.level)+1;
                if(otherLevel > ourLevel){
                    if(otherLevel-ourLevel > 1){
                        this.transfer(world, layer, ourLevel, otherState, ourState, x+direction, x, y, tile);
                    }
                    else{
                        // Remember for balancing
                        return true;
                    }
                }
                else if(otherLevel < ourLevel){
                    if(otherLevel-ourLevel < -1){
                        this.transfer(world, layer, otherLevel, ourState, otherState, x, x+direction, y, tile);
                    }
                    else if(oneToSpare){ // If we have one to spare we can transfer it here
                        this.transfer(world, layer, otherLevel, oppositeState, otherState, x-direction, x+direction, y, tile);
                    }
                    else{ // Check if we have one to spare further away
                        if(!world.isPosLoaded(x-direction*2, y)){
                            return false; // Maybe this should be an exception being thrown to completely cancel the update. I am not doing this however because exceptions are really bad for performance
                        }
                        TileState farState = world.getState(layer, x-direction*2, y);
                        if(farState.getTile() == tile){
                            if(farState.get(tile.level)-otherLevel > 0){
                                this.transfer(world, layer, otherLevel, farState, otherState, x-direction*2, x+direction, y, tile);
                            }
                        }
                    }
                }
            }
            else if(otherState.getTile().isAir()){
                if(ourLevel > 1){
                    // Spread
                    this.spread(world, layer, ourLevel, ourState, x, y, direction, tile);
                }
            }
        }
        return oneToSpare;
    }

    // Direction: 1 = right, -1 = left
    private void spread(IWorld world, TileLayer layer, int ourLevel, TileState ourState, int x, int y, int direction, TileLiquid tile){
        world.setState(layer, x+direction, y, tile.getDefState()); // Place one unit
        world.setState(layer, x, y, ourState.prop(tile.level, ourLevel-2)); // Decrease our level
    }

    // Direction: 1 = right, -1 = left
    private void transfer(IWorld world, TileLayer layer, int secondLevel, TileState firstState, TileState secondState, int x1, int x2, int y, TileLiquid tile){
        world.setState(layer, x1, y, firstState.prop(tile.level, firstState.get(tile.level)-1)); // Decrease first by one
        world.setState(layer, x2, y, secondState.prop(tile.level, secondLevel)); // Increase second by one
        world.scheduleUpdate(x2, y, layer, tile.getFlowSpeed());
    }

    private boolean transferDown(IWorld world, TileLayer layer, TileState firstState, TileState secondState, int x, int y, int direction, TileLiquid tile){
        boolean empty = false;
        if(secondState.getTile() == tile){
            int secondLevel = secondState.get(tile.level)+1;
            if(secondLevel < tile.getLevels()){
                if(firstState.get(tile.level) == 0){
                    world.setState(layer, x, y, GameContent.TILE_AIR.getDefState());
                    empty = true;
                }
                else{
                    world.setState(layer, x, y, firstState.prop(tile.level, firstState.get(tile.level)-1)); // Decrease first by one
                }
                world.setState(layer, x+direction, y-1, secondState.prop(tile.level, secondLevel)); // Increase second by one
            }
        }
        return empty;
    }

    private boolean setToInv(ItemInstance inst, ComponentSlot slot){
        if(inst == null ? slot.slot.canRemove() : slot.slot.canPlace(inst)){
            slot.slot.set(inst);

            if(RockBottomAPI.getNet().isClient()){
                RockBottomAPI.getNet().sendToServer(new PacketSlotModification(RockBottomAPI.getGame().getPlayer().getUniqueId(), slot.componentId, inst));
            }
            return true;
        }
        else{
            return false;
        }
    }
}