package de.ellpeck.rockbottom.gui;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.assets.font.Font;
import de.ellpeck.rockbottom.api.assets.tex.Texture;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.gui.component.ComponentInputField;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;
import de.ellpeck.rockbottom.gui.component.ComponentToggleButton;
import de.ellpeck.rockbottom.net.packet.toserver.PacketSignText;
import de.ellpeck.rockbottom.world.tile.entity.TileEntitySign;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import java.util.List;

public class GuiSign extends Gui{

    private final TileEntitySign tile;

    private boolean editMode;
    private ComponentInputField inputField;
    private String text;

    public GuiSign(TileEntitySign tile){
        super(200, 120);
        this.tile = tile;
        this.text = this.tile.text;
    }

    @Override
    public void initGui(IGameInstance game){
        super.initGui(game);

        this.components.add(new ComponentToggleButton(this, this.guiLeft+this.sizeX/2-25, this.guiTop+this.sizeY-16, 50, 16, this.editMode, () -> {
            this.editMode = !this.editMode;
            this.sendAndSave();
            this.initGui(game);
            return true;
        }, "button.edit"));

        if(this.editMode){
            this.inputField = new ComponentInputField(this, this.guiLeft+this.sizeX/2-75, this.guiTop+this.sizeY-34, 150, 16, true, false, true, 256, true, s -> this.text = this.inputField.getText());
            if(this.text != null){
                this.inputField.setText(this.text);
            }
            this.components.add(this.inputField);
        }
        else{
            this.inputField = null;
        }
    }

    @Override
    public void render(IGameInstance game, IAssetManager manager, Graphics g){
        renderSignText(manager, g, this.text, this.guiLeft, this.sizeX/2, this.guiTop, 1F);
        super.render(game, manager, g);
    }

    public static void renderSignText(IAssetManager manager, Graphics g, String text, float x, float textOffset, float y, float scale){
        Texture tex = manager.getTexture(RockBottomAPI.createInternalRes("gui.sign"));
        tex.draw(x, y, scale);
        g.setColor(Color.black);
        g.drawRect(x, y, tex.getWidth()*scale, tex.getHeight()*scale);

        if(text != null && !text.isEmpty()){
            Font font = manager.getFont();
            List<String> split = font.splitTextToLength((int)(200F*scale), scale*0.45F, true, text);
            for(int i = 0; i < Math.min(split.size(), 6); i++){
                font.drawCenteredString(x+textOffset, y+2+(i*manager.getFont().getHeight(scale*0.45F)), split.get(i), scale*0.45F, false);
            }
        }
    }

    @Override
    public void onClosed(IGameInstance game){
        super.onClosed(game);
        this.sendAndSave();
    }

    private void sendAndSave(){
        if(this.text != null && !this.text.equals(this.tile.text)){
            this.tile.text = this.text;

            if(RockBottomAPI.getNet().isClient()){
                RockBottomAPI.getNet().sendToServer(new PacketSignText(this.tile.x, this.tile.y, this.text));
            }
        }
    }

    @Override
    public IResourceName getName(){
        return RockBottomAPI.createInternalRes("sign");
    }

    @Override
    public boolean doesPauseGame(){
        return false;
    }

    @Override
    public boolean canCloseWithInvKey(){
        return this.inputField == null || !this.inputField.isActive();
    }
}