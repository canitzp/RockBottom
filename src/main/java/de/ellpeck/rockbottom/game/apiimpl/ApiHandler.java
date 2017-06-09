package de.ellpeck.rockbottom.game.apiimpl;

import de.ellpeck.rockbottom.api.IApiHandler;
import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.assets.font.Font;
import de.ellpeck.rockbottom.api.data.set.DataSet;
import de.ellpeck.rockbottom.api.data.set.part.DataPart;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.gui.component.ComponentSlot;
import de.ellpeck.rockbottom.api.item.Item;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.render.item.IItemRenderer;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;
import de.ellpeck.rockbottom.game.RockBottom;
import de.ellpeck.rockbottom.game.net.packet.toclient.PacketEntityUpdate;
import de.ellpeck.rockbottom.game.net.packet.toserver.PacketSlotModification;
import de.ellpeck.rockbottom.game.util.Util;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApiHandler implements IApiHandler{

    private static final IResourceName SLOT_NAME = RockBottom.internalRes("gui.slot");

    @Override
    public void writeDataSet(DataSet set, File file){
        try{
            if(!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            DataOutputStream stream = new DataOutputStream(new FileOutputStream(file));
            this.writeSet(stream, set);
            stream.close();
        }
        catch(Exception e){
            Log.error("Exception saving a data set to disk!", e);
        }
    }

    @Override
    public void readDataSet(DataSet set, File file){
        if(!set.data.isEmpty()){
            set.data.clear();
        }

        try{
            if(file.exists()){
                DataInputStream stream = new DataInputStream(new FileInputStream(file));
                this.readSet(stream, set);
                stream.close();
            }
        }
        catch(Exception e){
            Log.error("Exception loading a data set from disk!", e);
        }
    }

    @Override
    public void writeSet(DataOutput stream, DataSet set) throws Exception{
        stream.writeInt(set.data.size());

        for(DataPart part : set.data.values()){
            this.writePart(stream, part);
        }
    }

    @Override
    public void readSet(DataInput stream, DataSet set) throws Exception{
        int amount = stream.readInt();

        for(int i = 0; i < amount; i++){
            DataPart part = this.readPart(stream);
            set.data.put(part.getName(), part);
        }
    }

    @Override
    public void writePart(DataOutput stream, DataPart part) throws Exception{
        stream.writeByte(RockBottomAPI.PART_REGISTRY.getId(part.getClass()));
        stream.writeUTF(part.getName());
        part.write(stream);
    }

    @Override
    public DataPart readPart(DataInput stream) throws Exception{
        int id = stream.readByte();
        String name = stream.readUTF();

        Class<? extends DataPart> partClass = RockBottomAPI.PART_REGISTRY.get(id);
        DataPart part = partClass.getConstructor(String.class).newInstance(name);
        part.read(stream);

        return part;
    }

    @Override
    public void doDefaultEntityUpdate(Entity entity){
        if(!entity.isDead()){
            entity.applyMotion();

            entity.move(entity.motionX, entity.motionY);

            if(entity.onGround){
                entity.motionY = 0;

                if(entity.fallAmount > 0){
                    entity.onGroundHit();
                    entity.fallAmount = 0;
                }
            }
            else if(entity.motionY < 0){
                entity.fallAmount++;
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

        if(RockBottomAPI.getNet().isServer()){
            if(entity.ticksExisted%entity.getUpdateFrequency() == 0){
                if(entity.lastX != entity.x || entity.lastY != entity.y){
                    RockBottomAPI.getNet().sendToAllPlayers(entity.world, new PacketEntityUpdate(entity.getUniqueId(), entity.x, entity.y, entity.motionX, entity.motionY));

                    entity.lastX = entity.x;
                    entity.lastY = entity.y;
                }
            }
        }
    }

    @Override
    public boolean doDefaultSlotMovement(IGameInstance game, int button, float x, float y, ComponentSlot slot){
        if(slot.isMouseOver(game)){
            ItemInstance slotInst = slot.slot.get();
            ItemInstance slotCopy = slotInst == null ? null : slotInst.copy();

            if(button == game.getSettings().buttonGuiAction1){
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
                        if(slotCopy.isItemEqual(slot.container.holdingInst)){
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
            else if(button == game.getSettings().buttonGuiAction2){
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
                    else if(slotCopy.isItemEqual(slot.container.holdingInst)){
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
        }
        return false;
    }

    @Override
    public void renderSlotInGui(IGameInstance game, IAssetManager manager, Graphics g, ItemInstance slot, float x, float y, float scale){
        this.drawScaledImage(g, manager.getImage(SLOT_NAME), x, y, scale, game.getSettings().guiColor);

        if(slot != null){
            this.renderItemInGui(game, manager, g, slot, x+3F*scale, y+3F*scale, scale, Color.white);
        }
    }

    @Override
    public void renderItemInGui(IGameInstance game, IAssetManager manager, Graphics g, ItemInstance slot, float x, float y, float scale, Color color){
        Item item = slot.getItem();
        IItemRenderer renderer = item.getRenderer();
        if(renderer != null){
            renderer.render(game, manager, g, item, slot, x, y, 12F*scale, color);
        }

        manager.getFont().drawStringFromRight(x+15F*scale, y+9F*scale, String.valueOf(slot.getAmount()), 0.25F*scale);
    }

    @Override
    public void describeItem(IGameInstance game, IAssetManager manager, Graphics g, ItemInstance instance){
        boolean advanced = game.getContainer().getInput().isKeyDown(game.getSettings().keyAdvancedInfo.key);

        List<String> desc = new ArrayList<>();
        instance.getItem().describeItem(manager, instance, desc, advanced);

        this.drawHoverInfoAtMouse(game, manager, g, true, 0, desc);
    }

    @Override
    public void drawHoverInfoAtMouse(IGameInstance game, IAssetManager manager, Graphics g, boolean firstLineOffset, int maxLength, String... text){
        this.drawHoverInfoAtMouse(game, manager, g, firstLineOffset, maxLength, Arrays.asList(text));
    }

    @Override
    public void drawHoverInfoAtMouse(IGameInstance game, IAssetManager manager, Graphics g, boolean firstLineOffset, int maxLength, List<String> text){
        float mouseX = game.getMouseInGuiX();
        float mouseY = game.getMouseInGuiY();

        this.drawHoverInfo(game, manager, g, mouseX+18F/game.getSettings().guiScale, mouseY+18F/game.getSettings().guiScale, 0.25F, firstLineOffset, false, maxLength, text);
    }

    @Override
    public void drawHoverInfo(IGameInstance game, IAssetManager manager, Graphics g, float x, float y, float scale, boolean firstLineOffset, boolean canLeaveScreen, int maxLength, List<String> text){
        Font font = manager.getFont();

        float boxWidth = 0F;
        float boxHeight = 0F;

        if(maxLength > 0){
            text = font.splitTextToLength(maxLength, scale, true, text);
        }

        for(String s : text){
            float length = font.getWidth(s, scale);
            if(length > boxWidth){
                boxWidth = length;
            }

            if(firstLineOffset && boxHeight == 0F && text.size() > 1){
                boxHeight += 3F;
            }
            boxHeight += font.getHeight(scale);
        }

        if(boxWidth > 0F && boxHeight > 0F){
            boxWidth += 4F;
            boxHeight += 4F;

            if(!canLeaveScreen){
                x = Math.max(0, Math.min(x, (float)game.getWidthInGui()-boxWidth));
                y = Math.max(0, Math.min(y, (float)game.getHeightInGui()-boxHeight));
            }

            g.setColor(Gui.HOVER_INFO_BACKGROUND);
            g.fillRect(x, y, boxWidth, boxHeight);

            g.setColor(Color.black);
            g.drawRect(x, y, boxWidth, boxHeight);

            float yOffset = 0F;
            for(String s : text){
                font.drawString(x+2F, y+2F+yOffset, s, scale);

                if(firstLineOffset && yOffset == 0F){
                    yOffset += 3F;
                }
                yOffset += font.getHeight(scale);
            }
        }
    }

    @Override
    public void drawScaledImage(Graphics g, Image image, float x, float y, float scale, Color color){
        g.pushTransform();
        g.scale(scale, scale);
        image.draw(x/scale, y/scale, color);
        g.popTransform();
    }

    private boolean setToInv(ItemInstance inst, ComponentSlot slot){
        if(inst == null ? slot.slot.canRemove() : slot.slot.canPlace(inst)){
            slot.slot.set(inst);

            if(RockBottomAPI.getNet().isClient()){
                RockBottomAPI.getNet().sendToServer(new PacketSlotModification(RockBottom.get().getPlayer().getUniqueId(), slot.componentId, inst));
            }
            return true;
        }
        else{
            return false;
        }
    }
}