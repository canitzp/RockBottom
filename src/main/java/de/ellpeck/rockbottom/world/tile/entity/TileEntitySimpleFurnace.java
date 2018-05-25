package de.ellpeck.rockbottom.world.tile.entity;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.construction.resource.IUseInfo;
import de.ellpeck.rockbottom.api.construction.smelting.FuelInput;
import de.ellpeck.rockbottom.api.construction.smelting.SmeltingRecipe;
import de.ellpeck.rockbottom.api.data.set.DataSet;
import de.ellpeck.rockbottom.api.item.ItemInstance;
import de.ellpeck.rockbottom.api.tile.entity.IFilteredInventory;
import de.ellpeck.rockbottom.api.tile.entity.SyncedInt;
import de.ellpeck.rockbottom.api.tile.entity.TileEntity;
import de.ellpeck.rockbottom.api.tile.entity.TileInventory;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileEntitySimpleFurnace extends TileEntity{

    private final TileInventory inventory = new TileInventory(this, 3, inst -> {
        List<Integer> list = new ArrayList<>(2);
        if(SmeltingRecipe.forInput(inst) != null){
            list.add(0);
        }
        if(FuelInput.getFuelTime(inst) > 0){
            list.add(1);
        }
        return list;
    }, Collections.singletonList(2));
    private final SyncedInt smeltTime = new SyncedInt("smelt_time");
    private final SyncedInt maxSmeltTime = new SyncedInt("max_smelt_time");
    private final SyncedInt fuelTime = new SyncedInt("fuel_time");
    private final SyncedInt maxFuelTime = new SyncedInt("max_fuel_time");
    private ItemInstance scheduledOutput;
    private boolean lastActive;

    public TileEntitySimpleFurnace(IWorld world, int x, int y, TileLayer layer){
        super(world, x, y, layer);
    }

    @Override
    public IFilteredInventory getTileInventory(){
        return this.inventory;
    }

    @Override
    public boolean doesTick(){
        return true;
    }

    @Override
    public void update(IGameInstance game){
        super.update(game);

        if(!this.world.isClient()){
            if(this.maxSmeltTime.get() <= 0){
                ItemInstance input = this.inventory.get(0);
                if(input != null){
                    SmeltingRecipe recipe = SmeltingRecipe.forInput(input);
                    if(recipe != null){
                        IUseInfo recipeInput = recipe.getInput();
                        if(input.getAmount() >= recipeInput.getAmount()){
                            ItemInstance output = recipe.getOutput();
                            ItemInstance currentOutput = this.inventory.get(2);

                            if(currentOutput == null || (currentOutput.isEffectivelyEqual(output) && currentOutput.fitsAmount(output.getAmount()))){
                                this.maxSmeltTime.set(recipe.getTime());
                                this.scheduledOutput = output.copy();
                                this.inventory.remove(0, recipeInput.getAmount());
                            }
                        }
                    }
                }

                if(this.maxSmeltTime.get() <= 0){
                    this.scheduledOutput = null;
                }
            }
            else{
                if(this.fuelTime.get() <= 0){
                    ItemInstance fuel = this.inventory.get(1);
                    if(fuel != null){
                        int time = FuelInput.getFuelTime(fuel);
                        if(time > 0){
                            this.fuelTime.set(time);
                            this.maxFuelTime.set(time);
                            this.inventory.remove(1, 1);
                        }
                    }

                    if(this.fuelTime.get() <= 0 && this.smeltTime.get() > 0){
                        this.smeltTime.remove(1);
                    }
                }
                else{
                    if(Util.RANDOM.nextFloat() >= 0.45F){
                        this.smeltTime.add(1);
                    }

                    if(this.smeltTime.get() >= this.maxSmeltTime.get()){
                        ItemInstance currentOutput = this.inventory.get(2);
                        if(currentOutput != null && currentOutput.isEffectivelyEqual(this.scheduledOutput)){
                            this.inventory.add(2, this.scheduledOutput.getAmount());
                        }
                        else{
                            this.inventory.set(2, this.scheduledOutput);
                        }

                        this.scheduledOutput = null;
                        this.smeltTime.set(0);
                        this.maxSmeltTime.set(0);
                    }
                }
            }

            if(this.fuelTime.get() > 0){
                this.fuelTime.remove(1);
            }
        }

        boolean active = this.isActive();
        if(active != this.lastActive){
            this.lastActive = active;
            this.world.causeLightUpdate(this.x, this.y);
        }
    }

    @Override
    protected boolean needsSync(){
        return this.smeltTime.needsSync() || this.maxSmeltTime.needsSync() || this.fuelTime.needsSync() || this.maxFuelTime.needsSync();
    }

    @Override
    protected void onSync(){
        this.smeltTime.onSync();
        this.maxSmeltTime.onSync();
        this.fuelTime.onSync();
        this.maxFuelTime.onSync();
    }

    public float getFuelPercentage(){
        return this.maxFuelTime.get() > 0 ? this.fuelTime.get()/(float)this.maxFuelTime.get() : 0;
    }

    public float getSmeltPercentage(){
        return this.maxSmeltTime.get() > 0 ? this.smeltTime.get()/(float)this.maxSmeltTime.get() : 0;
    }

    public boolean isActive(){
        return this.smeltTime.get() > 0 || this.fuelTime.get() > 0;
    }

    @Override
    public void save(DataSet set, boolean forSync){
        if(!forSync){
            this.inventory.save(set);

            if(this.scheduledOutput != null){
                DataSet sub = new DataSet();
                this.scheduledOutput.save(sub);
                set.addDataSet("output", sub);
            }
        }

        this.smeltTime.save(set);
        this.maxSmeltTime.save(set);
        this.fuelTime.save(set);
        this.maxFuelTime.save(set);
    }

    @Override
    public void load(DataSet set, boolean forSync){
        if(!forSync){
            this.inventory.load(set);

            if(set.hasKey("output")){
                DataSet sub = set.getDataSet("output");
                this.scheduledOutput = ItemInstance.load(sub);
            }
        }

        this.smeltTime.load(set);
        this.maxSmeltTime.load(set);
        this.fuelTime.load(set);
        this.maxFuelTime.load(set);
    }
}
