package de.ellpeck.game.gui;

import de.ellpeck.game.Game;
import de.ellpeck.game.assets.AssetManager;
import de.ellpeck.game.gui.component.ComponentHotbarSlot;
import de.ellpeck.game.util.MathUtil;
import de.ellpeck.game.world.entity.player.EntityPlayer;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import java.util.ArrayList;
import java.util.List;

public class GuiManager{

    private final List<ComponentHotbarSlot> hotbarSlots = new ArrayList<>();
    private Gui gui;

    public GuiManager(EntityPlayer player){
        for(int i = 0; i < 8; i++){
            int x = (int)(Game.get().getWidthInGui()/2-59.25+i*15);
            this.hotbarSlots.add(new ComponentHotbarSlot(player.inv, i, x, 3));
        }
    }

    public void update(Game game){
        if(this.gui != null){
            this.gui.update(game);
        }
    }

    public void render(Game game, AssetManager manager, Graphics g, EntityPlayer player){
        g.scale(game.settings.guiScale, game.settings.guiScale);

        this.hotbarSlots.forEach(slot -> slot.render(game, manager, g));

        this.drawHealth(game, manager, g, player);

        Gui gui = player.guiManager.getGui();
        if(gui != null){
            g.setColor(Gui.GRADIENT);
            g.fillRect(0F, 0F, (float)game.getWidthInGui(), (float)game.getHeightInGui());

            gui.render(game, manager, g);
            gui.renderOverlay(game, manager, g);
        }
        else{
            this.hotbarSlots.forEach(slot -> slot.renderOverlay(game, manager, g));
        }
    }

    private void drawHealth(Game game, AssetManager manager, Graphics g, EntityPlayer player){
        int healthParts = MathUtil.floor(player.getHealth()/20);
        int maxHealthParts = MathUtil.floor(player.getMaxHealth()/20);

        Image heart = manager.getImage("gui.heart");
        Image heartEmpty = manager.getImage("gui.heart_empty");

        int step = 13;
        int xStart = (int)game.getWidthInGui()-3-maxHealthParts*step;
        int yStart = (int)game.getHeightInGui()-3-12;

        int currX = 0;
        for(int i = 0; i < maxHealthParts; i++){
            Gui.drawScaledImage(g, healthParts > i ? heart : heartEmpty, xStart+currX, yStart, 0.75F, Color.white);
            currX += step;
        }

        if(player.guiManager.getGui() == null){
            float mouseX = game.getMouseInGuiX();
            float mouseY = game.getMouseInGuiY();

            if(mouseX >= xStart && mouseX < xStart+step*maxHealthParts-1 && mouseY >= yStart && mouseY < yStart+12){
                Gui.drawHoverInfoAtMouse(game, manager, g, false, manager.localize("info.health")+":", player.getHealth()+"/"+player.getMaxHealth());
            }
        }
    }

    public void openGui(Gui gui){
        Game game = Game.get();

        if(this.gui != null){
            this.gui.onClosed(game);
        }

        this.gui = gui;

        if(this.gui != null){
            this.gui.initGui(game);
        }
    }

    public void closeGui(){
        this.openGui(null);
    }

    public Gui getGui(){
        return this.gui;
    }

    public boolean onMouseAction(Game game, int button){
        if(this.gui != null){
            return this.gui.onMouseAction(game, button);
        }
        else{
            for(ComponentHotbarSlot slot : this.hotbarSlots){
                if(slot.onMouseAction(game, button)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean onKeyboardAction(Game game, int button){
        return this.gui != null && this.gui.onKeyboardAction(game, button);
    }
}