package de.ellpeck.rockbottom.world.gen.biome;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.util.reg.ResourceName;
import de.ellpeck.rockbottom.api.world.IChunk;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.gen.INoiseGen;
import de.ellpeck.rockbottom.api.world.gen.biome.BiomeBasic;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;

public class BiomeGrassland extends BiomeBasic{

    public BiomeGrassland(ResourceName name, int highestY, int lowestY, int weight){
        super(name, highestY, lowestY, weight);
    }

    @Override
    public TileState getState(IWorld world, IChunk chunk, int x, int y, TileLayer layer, INoiseGen noise){
        int height = this.getExpectedSurfaceHeight(world, chunk.getX()+x, layer, noise);
        int stoneHeight = height-Util.ceil(noise.make2dNoise((chunk.getX()+x)/5D, 0D)*3D)-2;
        return getState(layer, chunk.getY()+y, height, stoneHeight);
    }

    public static TileState getState(TileLayer layer, int y, int height, int stoneHeight){
        if(layer == TileLayer.MAIN || layer == TileLayer.BACKGROUND){
            if(y == height && layer == TileLayer.MAIN){
                return GameContent.TILE_GRASS.getDefState();
            }
            else if(y <= height){
                if(y >= stoneHeight){
                    return GameContent.TILE_SOIL.getDefState();
                }
                else{
                    return GameContent.TILE_STONE.getDefState();
                }
            }
        }
        return GameContent.TILE_AIR.getDefState();
    }

    public static int getHeight(TileLayer layer, int x, INoiseGen noise, int minHeight, int maxHeight){
        int height = (int)(((noise.make2dNoise(x/100D, 0D)+noise.make2dNoise(x/20D, 0D)*2D)/3D)*(double)(maxHeight-minHeight))+minHeight;

        if(layer == TileLayer.BACKGROUND){
            height -= Util.ceil(noise.make2dNoise(x/10D, 0D)*3D);
        }

        return height;
    }

    @Override
    public int getExpectedSurfaceHeight(IWorld world, int x, TileLayer layer, INoiseGen noise){
        return getHeight(layer, x, noise, 0, 10);
    }

    @Override
    public boolean hasGrasslandDecoration(){
        return true;
    }

    @Override
    public float getFlowerChance(){
        return 0.35F;
    }

    @Override
    public float getPebbleChance(){
        return 0.2F;
    }

    @Override
    public boolean canTreeGrow(IWorld world, IChunk chunk, int x, int y){
        return y > 0 && chunk.getStateInner(x, y-1).getTile().canKeepPlants(world, chunk.getX()+x, chunk.getY()+y, TileLayer.MAIN);
    }
}
