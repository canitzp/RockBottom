package de.ellpeck.rockbottom.net.client;

import de.ellpeck.rockbottom.RockBottom;
import de.ellpeck.rockbottom.data.set.DataSet;
import de.ellpeck.rockbottom.world.Chunk;
import de.ellpeck.rockbottom.world.TileLayer;
import de.ellpeck.rockbottom.world.World;

public class ClientChunk extends Chunk{

    public ClientChunk(World world, int gridX, int gridY){
        super(world, gridX, gridY);
    }

    @Override
    public void loadOrCreate(DataSet set){
        throw new UnsupportedOperationException("Cannot load or create client chunk");
    }

    @Override
    public void save(DataSet set){
        throw new UnsupportedOperationException("Cannot save client chunk");
    }

    @Override
    public void update(RockBottom game){
        this.checkListSync();

        if(!this.isGenerating){
            this.updateEntities(game);
        }

        this.updateTimer();
    }

    @Override
    public void scheduleUpdate(int x, int y, TileLayer layer, int time){
        throw new UnsupportedOperationException("Cannot schedule updates in a client chunk");
    }

    @Override
    public int getScheduledUpdateAmount(){
        return 0;
    }
}