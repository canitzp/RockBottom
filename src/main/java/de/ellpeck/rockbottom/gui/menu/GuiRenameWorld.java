package de.ellpeck.rockbottom.gui.menu;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.IRenderer;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.gui.component.ComponentButton;
import de.ellpeck.rockbottom.api.gui.component.ComponentInputField;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;

import java.io.File;

public class GuiRenameWorld extends Gui{

    private final File worldFile;
    private String name;

    public GuiRenameWorld(Gui parent, File worldFile){
        super(parent);
        this.worldFile = worldFile;
    }

    @Override
    public void init(IGameInstance game){
        super.init(game);

        ComponentInputField field = new ComponentInputField(this, this.width/2-80, this.height/2-40, 160, 16, true, false, true, 128, false, strg -> this.name = GuiCreateWorld.makeNameSafe(game, strg));
        this.components.add(field);

        this.components.add(new ComponentButton(this, this.width/2-82, this.height-30, 80, 16, () -> {
            if(this.name != null && !this.name.isEmpty()){
                if(this.worldFile.renameTo(new File(this.worldFile.getParent(), this.name))){
                    RockBottomAPI.logger().info("Successfully renamed world to "+this.name);
                }
                else{
                    RockBottomAPI.logger().warning("Couldn't rename world to "+this.name);
                }
            }

            game.getGuiManager().openGui(this.parent);
            return true;
        }, "Rename boi"));

        this.components.add(new ComponentButton(this, this.width/2+2, this.height-30, 80, 16, () -> {
            game.getGuiManager().openGui(this.parent);
            return true;
        }, game.getAssetManager().localize(RockBottomAPI.createInternalRes("button.back"))));

        this.name = GuiCreateWorld.makeNameSafe(game, "");
    }

    @Override
    public void render(IGameInstance game, IAssetManager manager, IRenderer g){
        super.render(game, manager, g);

        if(this.name != null && !this.name.isEmpty()){
            manager.getFont().drawString(this.x+this.width/2-80, this.height/2-40+18, "Final Name: "+this.name, 0.25F);
        }
    }

    @Override
    public IResourceName getName(){
        return RockBottomAPI.createInternalRes("rename_world");
    }
}