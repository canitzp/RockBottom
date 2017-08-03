package de.ellpeck.rockbottom.net.packet.toclient;

import de.ellpeck.rockbottom.api.IGameInstance;
import de.ellpeck.rockbottom.api.entity.Entity;
import de.ellpeck.rockbottom.api.net.packet.IPacket;
import de.ellpeck.rockbottom.api.util.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.UUID;

public class PacketEntityUpdate implements IPacket{

    private UUID uniqueId;
    private double x;
    private double y;
    private double motionX;
    private double motionY;
    private Direction facing;

    public PacketEntityUpdate(UUID uniqueId, double x, double y, double motionX, double motionY, Direction facing){
        this.uniqueId = uniqueId;
        this.x = x;
        this.y = y;
        this.motionX = motionX;
        this.motionY = motionY;
        this.facing = facing;
    }

    public PacketEntityUpdate(){

    }

    @Override
    public void toBuffer(ByteBuf buf) throws IOException{
        buf.writeLong(this.uniqueId.getMostSignificantBits());
        buf.writeLong(this.uniqueId.getLeastSignificantBits());
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.motionX);
        buf.writeDouble(this.motionY);
        buf.writeInt(this.facing.ordinal());
    }

    @Override
    public void fromBuffer(ByteBuf buf) throws IOException{
        this.uniqueId = new UUID(buf.readLong(), buf.readLong());
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.motionX = buf.readDouble();
        this.motionY = buf.readDouble();
        this.facing = Direction.DIRECTIONS[buf.readInt()];
    }

    @Override
    public void handle(IGameInstance game, ChannelHandlerContext context){
        game.scheduleAction(() -> {
            if(game.getWorld() != null){
                Entity entity = game.getWorld().getEntity(this.uniqueId);
                if(entity != null){
                    entity.x = this.x;
                    entity.y = this.y;
                    entity.motionX = this.motionX;
                    entity.motionY = this.motionY;
                    entity.facing = this.facing;
                }
            }
            return true;
        });
    }
}
