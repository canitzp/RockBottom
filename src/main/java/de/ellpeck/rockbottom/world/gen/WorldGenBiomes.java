package de.ellpeck.rockbottom.world.gen;

import de.ellpeck.rockbottom.api.util.reg.ResourceName;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.gen.BiomeGen;
import de.ellpeck.rockbottom.api.world.gen.biome.Biome;
import de.ellpeck.rockbottom.api.world.gen.biome.level.BiomeLevel;

import java.util.List;

public class WorldGenBiomes extends BiomeGen {

    public static final ResourceName ID = ResourceName.intern("biomes");

    @Override
    public int getLevelTransition(IWorld world) {
        return 7;
    }

    @Override
    public int getBiomeTransition(IWorld world) {
        return 5;
    }

    @Override
    public int getBiomeBlobSize(IWorld world) {
        return 6;
    }

    @Override
    public int getNoiseSeedScramble(IWorld world) {
        return 12396837;
    }

    @Override
    public List<BiomeLevel> getGenerationLevels(Biome biome, IWorld world) {
        return biome.getGenerationLevels(world);
    }

    @Override
    public int getLayerSeedScramble(IWorld world) {
        return 827398433;
    }
}
