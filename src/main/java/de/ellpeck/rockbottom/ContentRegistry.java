package de.ellpeck.rockbottom;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.RockBottomAPI;
import de.ellpeck.rockbottom.api.construction.resource.ResUseInfo;
import de.ellpeck.rockbottom.api.construction.smelting.FuelInput;
import de.ellpeck.rockbottom.api.effect.BasicEffect;
import de.ellpeck.rockbottom.api.item.ItemBasic;
import de.ellpeck.rockbottom.api.item.ItemTool;
import de.ellpeck.rockbottom.api.item.ToolType;
import de.ellpeck.rockbottom.api.tile.TileBasic;
import de.ellpeck.rockbottom.api.util.reg.ResourceName;
import de.ellpeck.rockbottom.item.ItemCopperCanister;
import de.ellpeck.rockbottom.item.ItemFirework;
import de.ellpeck.rockbottom.item.ItemStartNote;
import de.ellpeck.rockbottom.item.ItemTwig;
import de.ellpeck.rockbottom.world.entity.EntityFirework;
import de.ellpeck.rockbottom.world.entity.EntityItem;
import de.ellpeck.rockbottom.world.entity.EntitySand;
import de.ellpeck.rockbottom.world.entity.player.knowledge.RecipeInformation;
import de.ellpeck.rockbottom.world.gen.WorldGenBiomes;
import de.ellpeck.rockbottom.world.gen.biome.BiomeDesert;
import de.ellpeck.rockbottom.world.gen.biome.BiomeGrassland;
import de.ellpeck.rockbottom.world.gen.biome.BiomeSky;
import de.ellpeck.rockbottom.world.gen.biome.BiomeUnderground;
import de.ellpeck.rockbottom.world.gen.feature.*;
import de.ellpeck.rockbottom.world.gen.ore.WorldGenCoal;
import de.ellpeck.rockbottom.world.gen.ore.WorldGenCopper;
import de.ellpeck.rockbottom.world.tile.*;

public final class ContentRegistry{

    public static void init(){
        new TileAir().register();
        new TileSoil().register();
        new TileGrass().register();
        new TileStone().register();
        new TileGrassTuft().register();
        new TileLog().register();
        new TileLeaves().register();
        new TileFlower().register();
        new TilePebbles().register();
        new TileSand().register();
        new TileBasic(ResourceName.intern("sandstone")).register();
        new TileOreMaterial(ResourceName.intern("coal")).register();
        new TileTorch(ResourceName.intern("torch")).register();
        new TileLadder().register();
        new TileChest().register();
        new TileSign().register();
        new TileSapling().register();
        new TileWater().register();
        new TileWoodBoards().register();
        new TileWoodDoor(ResourceName.intern("wood_door")).register();
        new TileWoodDoor(ResourceName.intern("wood_door_old")).register();
        new TileRemainsGoo().register();
        new TileGrassTorch().register();
        new TileCopper().register();
        new TileSimpleFurnace().register();

        new ItemTool(ResourceName.intern("brittle_pickaxe"), 2F, ToolType.PICKAXE, 1).register();
        new ItemTool(ResourceName.intern("brittle_axe"), 2F, ToolType.AXE, 1).register();
        new ItemTool(ResourceName.intern("brittle_shovel"), 2F, ToolType.SHOVEL, 1).register();
        new ItemFirework().register();
        new ItemStartNote().register();
        new ItemBasic(ResourceName.intern("plant_fiber")).register();
        new ItemTwig().register();
        new ItemTool(ResourceName.intern("stone_pickaxe"), 4F, ToolType.PICKAXE, 5).register();
        new ItemTool(ResourceName.intern("stone_axe"), 3F, ToolType.AXE, 5).register();
        new ItemTool(ResourceName.intern("stone_shovel"), 3F, ToolType.SHOVEL, 5).register();
        new ItemCopperCanister().register();
        new ItemTool(ResourceName.intern("super_pickaxe"), Float.MAX_VALUE, ToolType.PICKAXE, Integer.MAX_VALUE).addToolType(ToolType.AXE, Integer.MAX_VALUE).addToolType(ToolType.SHOVEL, Integer.MAX_VALUE).register();

        new BiomeSky(ResourceName.intern("sky"), Integer.MAX_VALUE, 40, 100).register();
        new BiomeGrassland(ResourceName.intern("grassland"), 60, -5, 1000).register();
        new BiomeDesert(ResourceName.intern("desert"), 60, -5, 500).register();
        new BiomeUnderground(ResourceName.intern("underground"), -5, Integer.MIN_VALUE, 1000).register();

        RockBottomAPI.ENTITY_REGISTRY.register(ResourceName.intern("item"), EntityItem.class);
        RockBottomAPI.ENTITY_REGISTRY.register(ResourceName.intern("sand"), EntitySand.class);
        RockBottomAPI.ENTITY_REGISTRY.register(ResourceName.intern("firework"), EntityFirework.class);

        RockBottomAPI.WORLD_GENERATORS.register(WorldGenBiomes.ID, WorldGenBiomes.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("grass"), WorldGenGrass.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("trees"), WorldGenTrees.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("flowers"), WorldGenFlowers.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("pebbles"), WorldGenPebbles.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("coal"), WorldGenCoal.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("start_hut"), WorldGenStartHut.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("copper"), WorldGenCopper.class);
        RockBottomAPI.WORLD_GENERATORS.register(ResourceName.intern("caves"), WorldGenCaves.class);

        RockBottomAPI.INFORMATION_REGISTRY.register(RecipeInformation.REG_NAME, RecipeInformation.class);

        new BasicEffect(ResourceName.intern("speed"), false, false, 36000).register();
        new BasicEffect(ResourceName.intern("jump_height"), false, false, 36000).register();

        new FuelInput(new ResUseInfo(GameContent.RES_COAL), 1000).register();
        new FuelInput(new ResUseInfo(GameContent.RES_WOOD_RAW), 300).register();
        new FuelInput(new ResUseInfo(GameContent.RES_WOOD_PROCESSED), 100).register();
        new FuelInput(new ResUseInfo(GameContent.RES_PLANT_FIBER), 20).register();
        new FuelInput(new ResUseInfo(GameContent.RES_STICK), 20).register();
    }
}
