package de.ellpeck.rockbottom.gui.cursor;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.IRenderer;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.entity.player.IInteractionManager;
import de.ellpeck.rockbottom.api.gui.Gui;
import de.ellpeck.rockbottom.api.gui.GuiContainer;
import de.ellpeck.rockbottom.api.gui.IGuiManager;
import de.ellpeck.rockbottom.api.gui.ISpecialCursor;
import de.ellpeck.rockbottom.api.util.reg.IResourceName;

public class CursorClosedHand implements ISpecialCursor{

    @Override
    public IResourceName getTexture(){
        return RockBottomAPI.createInternalRes("gui.cursor.closed_hand");
    }

    @Override
    public boolean shouldUseCursor(IGameInstance game, IAssetManager manager, IRenderer graphics, IGuiManager guiManager, IInteractionManager interactionManager){
        Gui gui = guiManager.getGui();
        return gui instanceof GuiContainer && ((GuiContainer)gui).getContainer().holdingInst != null;
    }

    @Override
    public int getPriority(){
        return 1000;
    }
}
