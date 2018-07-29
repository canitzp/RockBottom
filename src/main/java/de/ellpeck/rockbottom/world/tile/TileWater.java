package de.ellpeck.rockbottom.world.tile;

import de.ellpeck.rockbottom.api.GameContent;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.tile.TileLiquid;
import de.ellpeck.rockbottom.api.tile.state.TileState;
import de.ellpeck.rockbottom.api.util.BoundBox;
import de.ellpeck.rockbottom.api.util.Direction;
import de.ellpeck.rockbottom.api.util.Util;
import de.ellpeck.rockbottom.api.util.reg.ResourceName;
import de.ellpeck.rockbottom.api.world.IWorld;
import de.ellpeck.rockbottom.api.world.layer.TileLayer;

import java.util.List;

public class TileWater extends TileLiquid {

    public TileWater() {
        super(ResourceName.intern("water"));
    }

    @Override
    public int getLevels() {
        return 12;
    }

    @Override
    public boolean doesFlow() {
        return true;
    }

    @Override
    public int getFlowSpeed() {
        return 5;
    }

    @Override
    public void onIntersectWithEntity(IWorld world, int x, int y, TileLayer layer, TileState state, BoundBox entityBox, BoundBox entityBoxMotion, List<BoundBox> tileBoxes, Entity entity) {
        super.onIntersectWithEntity(world, x, y, layer, state, entityBox, entityBoxMotion, tileBoxes, entity);

        if (state.get(this.level) > this.getLevels() / 3) {
            for (BoundBox box : tileBoxes) {
                if (entityBox.intersects(box)) {
                    entity.motionX *= 0.85;
                    if (entity.motionY < 0) {
                        entity.motionY *= 0.85;
                    }
                    entity.fallStartY = entity.getY();

                    break;
                }
            }
        }
    }

    @Override
    public void updateRandomly(IWorld world, int x, int y, TileLayer layer) {
        if (Util.RANDOM.nextFloat() >= 0.95F) {
            TileState state = world.getState(layer, x, y);
            int level = state.get(this.level);
            if (level <= 3) {
                for (Direction dir : Direction.ADJACENT) {
                    if (!world.isPosLoaded(x + dir.x, y + dir.y)) {
                        return;
                    } else {
                        TileState other = world.getState(layer, x + dir.x, y + dir.y);
                        if (other.getTile() == this && other.get(this.level) > 3) {
                            return;
                        }
                    }
                }

                if (level <= 0) {
                    world.setState(layer, x, y, GameContent.TILE_AIR.getDefState());
                } else {
                    world.setState(layer, x, y, state.prop(this.level, level - 1));
                }
            }
        }
    }
}
