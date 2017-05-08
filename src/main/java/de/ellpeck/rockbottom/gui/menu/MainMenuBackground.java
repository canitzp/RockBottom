package de.ellpeck.rockbottom.gui.menu;

import de.ellpeck.rockbottom.ContentRegistry;
import de.ellpeck.rockbottom.RockBottom;
import de.ellpeck.rockbottom.assets.AssetManager;
import de.ellpeck.rockbottom.render.WorldRenderer;
import de.ellpeck.rockbottom.render.tile.DefaultTileRenderer;
import de.ellpeck.rockbottom.render.tile.ITileRenderer;
import de.ellpeck.rockbottom.util.Util;
import de.ellpeck.rockbottom.world.tile.Tile;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;

public class MainMenuBackground{

    private static final int[] KONAMI_CODE = new int[]{Input.KEY_UP, Input.KEY_UP, Input.KEY_DOWN, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_B, Input.KEY_A};
    private static final int TILE_SIZE = 16;
    private int konamiAt;
    private Tile[][] menuTileGrid;

    private int tileAmountX;
    private int tileAmountY;

    private int stackedAmountX;
    private int stackAtY;

    private int timer;

    public void init(RockBottom game){
        this.tileAmountX = Util.ceil(game.getWidthInGui()/TILE_SIZE);
        this.tileAmountY = Util.ceil(game.getHeightInGui()/TILE_SIZE);
        this.menuTileGrid = new Tile[this.tileAmountX][this.tileAmountY];
        this.stackedAmountX = 0;
        this.stackAtY = 0;
    }

    public void update(RockBottom game){
        this.timer++;

        if(this.stackAtY < this.tileAmountY){
            if(this.timer%8 == 0 && Util.RANDOM.nextBoolean()){
                int placeY = this.tileAmountY-this.stackAtY-1;

                int placeX;
                do{
                    placeX = Util.RANDOM.nextInt(this.tileAmountX);
                }
                while(this.menuTileGrid[placeX][placeY] != null);

                Tile tile;
                if(this.konamiAt >= KONAMI_CODE.length){
                    tile = ContentRegistry.TILE_LEAVES;
                }
                else{
                    float f = Util.RANDOM.nextFloat();

                    if(f >= 0.9F){
                        tile = ContentRegistry.TILE_COPPER_ORE;
                    }
                    else if(f >= 0.75F){
                        tile = ContentRegistry.TILE_COAL_ORE;
                    }
                    else{
                        tile = ContentRegistry.TILE_ROCK;
                    }
                }
                this.menuTileGrid[placeX][placeY] = tile;

                this.stackedAmountX++;
                if(this.stackedAmountX >= this.tileAmountX){
                    this.stackedAmountX = 0;
                    this.stackAtY++;
                }
            }
        }
    }

    public void onKeyInput(int button){
        if(this.konamiAt < KONAMI_CODE.length){
            if(button == KONAMI_CODE[this.konamiAt]){
                this.konamiAt++;
            }
            else{
                this.konamiAt = 0;
            }
        }
    }

    public void render(RockBottom game, AssetManager manager, Graphics g){
        g.setBackground(WorldRenderer.SKY_COLORS[WorldRenderer.SKY_COLORS.length-1]);

        float offsetY = (float)game.getHeightInGui()-this.tileAmountY*TILE_SIZE;
        for(int x = 0; x < this.tileAmountX; x++){
            for(int y = 0; y < this.tileAmountY; y++){
                Tile tile = this.menuTileGrid[x][y];
                if(tile != null){
                    ITileRenderer renderer = tile.getRenderer();
                    if(renderer instanceof DefaultTileRenderer){
                        String tex = ((DefaultTileRenderer)renderer).texture;
                        manager.getImage(tex).draw(x*TILE_SIZE, offsetY+y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        manager.getFont().drawString(10, 5, "&aRock", 3F);
        manager.getFont().drawStringFromRight((float)game.getWidthInGui()-10, 55, "&aBottom", 3F);
    }
}
