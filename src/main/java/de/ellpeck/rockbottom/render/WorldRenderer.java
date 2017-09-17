package de.ellpeck.rockbottom.render;

import de.ellpeck.rockbottom.api.*;
import de.ellpeck.rockbottom.api.assets.IAssetManager;
import de.ellpeck.rockbottom.api.assets.tex.ITexture;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.event.impl.WorldRenderEvent;
import de.ellpeck.rockbottom.api.render.entity.IEntityRenderer;
import de.ellpeck.rockbottom.api.render.tile.ITileRenderer;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.Colors;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IChunk;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;
import de.ellpeck.rockbottom.particle.ParticleManager;
import de.ellpeck.rockbottom.world.World;
import de.ellpeck.rockbottom.world.entity.player.EntityPlayer;
import de.ellpeck.rockbottom.world.entity.player.InteractionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorldRenderer{

    public static final int[] SKY_COLORS = new int[256];
    public static final int[] BACKGROUND_COLORS = new int[Constants.MAX_LIGHT+1];
    public static final int[] MAIN_COLORS = new int[Constants.MAX_LIGHT+1];

    public static void init(){
        float step = 1F/(Constants.MAX_LIGHT+1);
        for(int i = 0; i <= Constants.MAX_LIGHT; i++){
            float modifier = i*step;
            MAIN_COLORS[i] = Colors.rgb(modifier, modifier, modifier, 1F);
            BACKGROUND_COLORS[i] = Colors.rgb(modifier*0.5F, modifier*0.5F, modifier*0.5F, 1F);
        }

        int sky = 0x4C8DFF;
        for(int i = 0; i < SKY_COLORS.length; i++){
            float percent = (float)i/(float)SKY_COLORS.length;
            SKY_COLORS[i] = Colors.multiply(sky, percent);
        }
    }

    public void render(IGameInstance game, IAssetManager manager, ParticleManager particles, IGraphics g, World world, EntityPlayer player, InteractionManager input){
        IApiHandler api = RockBottomAPI.getApiHandler();
        float scale = game.getWorldScale();

        int skyLight = (int)(world.getSkylightModifier()*(SKY_COLORS.length-1));
        int color = SKY_COLORS[game.isLightDebug() ? SKY_COLORS.length-1 : skyLight];
        g.backgroundColor(color);

        double width = game.getWidthInWorld();
        double height = game.getHeightInWorld();
        float transX = (float)(player.x-width/2);
        float transY = (float)(-player.y-height/2);

        int topLeftX = Util.toGridPos(transX);
        int topLeftY = Util.toGridPos(-transY+1);
        int bottomRightX = Util.toGridPos(transX+width);
        int bottomRightY = Util.toGridPos(-transY-height);

        int minX = Math.min(topLeftX, bottomRightX);
        int minY = Math.min(topLeftY, bottomRightY);
        int maxX = Math.max(topLeftX, bottomRightX);
        int maxY = Math.max(topLeftY, bottomRightY);

        List<Entity> entities = new ArrayList<>();
        List<EntityPlayer> players = new ArrayList<>();

        for(int gridX = minX; gridX <= maxX; gridX++){
            for(int gridY = minY; gridY <= maxY; gridY++){
                if(world.isChunkLoaded(gridX, gridY)){
                    IChunk chunk = world.getChunkFromGridCoords(gridX, gridY);

                    for(int x = 0; x < Constants.CHUNK_SIZE; x++){
                        for(int y = 0; y < Constants.CHUNK_SIZE; y++){
                            int tileX = chunk.getX()+x;
                            int tileY = chunk.getY()+y;

                            if(tileX >= transX-1 && -tileY >= transY-1 && tileX < transX+width && -tileY < transY+height){
                                TileState tile = chunk.getStateInner(x, y);

                                int[] light = api.interpolateLight(world, chunk.getX()+x, chunk.getY()+y);

                                if(!game.isBackgroundDebug()){
                                    if(!tile.getTile().obscuresBackground() || game.isForegroundDebug()){
                                        TileState tileBack = chunk.getStateInner(TileLayer.BACKGROUND, x, y);
                                        ITileRenderer rendererBack = tileBack.getTile().getRenderer();
                                        if(rendererBack != null){
                                            rendererBack.render(game, manager, g, world, tileBack.getTile(), tileBack, tileX, tileY, TileLayer.BACKGROUND, (tileX-transX)*scale, (-tileY-transY)*scale, scale, api.interpolateWorldColor(light, TileLayer.BACKGROUND));

                                            if(input.breakingLayer == TileLayer.BACKGROUND){
                                                this.doBreakAnimation(input, manager, tileX, tileY, transX, transY, scale);
                                            }
                                        }
                                    }
                                }

                                if(!game.isForegroundDebug()){
                                    ITileRenderer renderer = tile.getTile().getRenderer();
                                    if(renderer != null){
                                        renderer.render(game, manager, g, world, tile.getTile(), tile, tileX, tileY, TileLayer.MAIN, (tileX-transX)*scale, (-tileY-transY)*scale, scale, api.interpolateWorldColor(light, TileLayer.MAIN));

                                        if(input.breakingLayer == TileLayer.MAIN){
                                            this.doBreakAnimation(input, manager, tileX, tileY, transX, transY, scale);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for(Entity entity : chunk.getAllEntities()){
                        entities.add(entity);

                        if(entity instanceof EntityPlayer){
                            players.add((EntityPlayer)entity);
                        }
                    }
                }
            }
        }

        g.pushMatrix();
        g.scale(game.getWorldScale(), game.getWorldScale());

        particles.render(game, manager, g, world, transX, transY);

        entities.stream().sorted(Comparator.comparingInt(Entity:: getRenderPriority)).forEach(entity -> {
            if(entity.shouldRender()){
                IEntityRenderer renderer = entity.getRenderer();
                if(renderer != null){
                    int light = world.getCombinedLight(Util.floor(entity.x), Util.floor(entity.y));
                    renderer.render(game, manager, g, world, entity, (float)entity.x-transX, (float)-entity.y-transY+1F, api.getColorByLight(light, TileLayer.MAIN));
                }
            }
        });

        RockBottomAPI.getEventHandler().fireEvent(new WorldRenderEvent(game, manager, g, world, player, transX, transY));

        players.forEach(entity -> {
            if(entity.shouldRender() && !RockBottomAPI.getNet().isThePlayer(entity)){
                manager.getFont().drawCenteredString((float)entity.x-transX, (float)-entity.y-transY-1.25F, entity.getChatColorFormat()+entity.getName(), 0.015F, false);
            }
        });

        if(game.isChunkBorderDebug()){
            for(int gridX = minX; gridX <= maxX; gridX++){
                for(int gridY = minY; gridY <= maxY; gridY++){
                    if(world.isChunkLoaded(gridX, gridY)){
                        int x = Util.toWorldPos(gridX);
                        int y = Util.toWorldPos(gridY);

                        g.drawRect(x-transX, -y-transY+1F-Constants.CHUNK_SIZE, Constants.CHUNK_SIZE, Constants.CHUNK_SIZE, Colors.GREEN);
                    }
                }
            }
        }

        g.popMatrix();
    }

    private void doBreakAnimation(InteractionManager input, IAssetManager manager, int tileX, int tileY, float transX, float transY, float scale){
        if(input.breakProgress > 0){
            if(tileX == input.breakTileX && tileY == input.breakTileY){
                ITexture brk = manager.getTexture(RockBottomAPI.createInternalRes("break."+Util.ceil(input.breakProgress*8F)));
                brk.draw((tileX-transX)*scale, (-tileY-transY)*scale, scale, scale);
            }
        }
    }
}
