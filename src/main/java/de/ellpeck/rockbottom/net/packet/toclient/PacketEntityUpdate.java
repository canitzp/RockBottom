package de.ellpeck.rockbottom.net.packet.toclient;

import de.ellpeck.rockbottom.RockBottom;
import de.ellpeck.rockbottom.net.packet.IPacket;
import de.ellpeck.rockbottom.world.entity.Entity;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.UUID;

public class PacketEntityUpdate implements IPacket{

    private UUID uniqueId;
    private boolean isPlayer;
    private double x;
    private double y;
    private double motionX;
    private double motionY;

    public PacketEntityUpdate(UUID uniqueId, boolean isPlayer, double x, double y, double motionX, double motionY){
        this.uniqueId = uniqueId;
        this.isPlayer = isPlayer;
        this.x = x;
        this.y = y;
        this.motionX = motionX;
        this.motionY = motionY;
    }

    public PacketEntityUpdate(){

    }

    @Override
    public void toBuffer(ByteBuf buf) throws IOException{
        buf.writeLong(this.uniqueId.getMostSignificantBits());
        buf.writeLong(this.uniqueId.getLeastSignificantBits());
        buf.writeBoolean(this.isPlayer);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.motionX);
        buf.writeDouble(this.motionY);
    }

    @Override
    public void fromBuffer(ByteBuf buf) throws IOException{
        this.uniqueId = new UUID(buf.readLong(), buf.readLong());
        this.isPlayer = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.motionX = buf.readDouble();
        this.motionY = buf.readDouble();
    }

    @Override
    public void handle(RockBottom game, ChannelHandlerContext context){
        game.scheduleAction(() -> {
            if(game.world != null){
                Entity entity;

                if(this.isPlayer){
                    entity = game.world.getPlayer(this.uniqueId);
                }
                else{
                    entity = game.world.getEntity(this.uniqueId);
                }

                if(entity != null){
                    entity.setPos(this.x, this.y);
                    entity.motionX = this.motionX;
                    entity.motionY = this.motionY;
                }
            }
            return true;
        });
    }
}