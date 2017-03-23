package de.ellpeck.game.data.set;

import de.ellpeck.game.Game;
import de.ellpeck.game.Main;
import de.ellpeck.game.data.DataManager;
import de.ellpeck.game.data.set.part.DataPart;
import de.ellpeck.game.data.set.part.PartUniqueId;
import de.ellpeck.game.data.set.part.PartDataSet;
import de.ellpeck.game.data.set.part.num.*;
import de.ellpeck.game.data.set.part.num.array.PartByteByteArray;
import de.ellpeck.game.data.set.part.num.array.PartIntArray;
import de.ellpeck.game.data.set.part.num.array.PartIntIntArray;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataSet{

    private final Map<String, DataPart> data = new HashMap<>();

    public void addPart(DataPart part){
        this.data.put(part.getName(), part);
    }

    public <T> T getPartContent(String key, Class<? extends DataPart<T>> typeClass, T defaultValue){
        DataPart part = this.data.get(key);

        if(part != null && part.getClass() == typeClass){
            T result = (T)part.get();
            if(result != null){
                return result;
            }
        }

        return defaultValue;
    }

    public int getInt(String key){
        return this.getPartContent(key, PartInt.class, 0);
    }

    public void addInt(String key, int i){
        this.addPart(new PartInt(key, i));
    }

    public long getLong(String key){
        return this.getPartContent(key, PartLong.class, 0L);
    }

    public void addLong(String key, long l){
        this.addPart(new PartLong(key, l));
    }

    public float getFloat(String key){
        return this.getPartContent(key,  PartFloat.class, 0F);
    }

    public void addFloat(String key, float f){
        this.addPart(new PartFloat(key, f));
    }

    public double getDouble(String key){
        return this.getPartContent(key, PartDouble.class, 0D);
    }

    public void addDouble(String key, double d){
        this.addPart(new PartDouble(key, d));
    }

    public DataSet getDataSet(String key){
        return this.getPartContent(key, PartDataSet.class, new DataSet());
    }

    public void addDataSet(String key, DataSet set){
        this.addPart(new PartDataSet(key, set));
    }

    public byte[][] getByteByteArray(String key, int defaultSize){
        return this.getPartContent(key, PartByteByteArray.class, new byte[defaultSize][defaultSize]);
    }

    public void addByteByteArray(String key, byte[][] array){
        this.addPart(new PartByteByteArray(key, array));
    }

    public int[] getIntArray(String key, int defaultSize){
        return this.getPartContent(key, PartIntArray.class, new int[defaultSize]);
    }

    public void addIntArray(String key, int[] array){
        this.addPart(new PartIntArray(key, array));
    }

    public int[][] getIntIntArray(String key, int defaultSize){
        return this.getPartContent(key, PartIntIntArray.class, new int[defaultSize][defaultSize]);
    }

    public void addIntIntArray(String key, int[][] array){
        this.addPart(new PartIntIntArray(key, array));
    }

    public UUID getUniqueId(String key){
        return this.getPartContent(key, PartUniqueId.class, null);
    }

    public void addUniqueId(String key, UUID id){
        this.addPart(new PartUniqueId(key, id));
    }

    public byte getByte(String key){
        return this.getPartContent(key, PartByte.class, (byte)0);
    }

    public void addByte(String key, byte b){
        this.addPart(new PartByte(key, b));
    }

    public void write(File file){
        try{
            if(!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            DataOutputStream stream = new DataOutputStream(new FileOutputStream(file));
            writeSet(stream, this);
            stream.close();
        }
        catch(Exception e){
            Main.doExceptionInfo(Game.get(), e);
        }
    }

    public void read(File file){
        if(!this.data.isEmpty()){
            this.data.clear();
        }

        try{
            if(file.exists()){
                DataInputStream stream = new DataInputStream(new FileInputStream(file));
                readSet(stream, this);
                stream.close();
            }
        }
        catch(Exception e){
            Main.doExceptionInfo(Game.get(), e);
        }
    }

    @Override
    public String toString(){
        return this.data.toString();
    }

    public Map<String, DataPart> getData(){
        return this.data;
    }

    public boolean isEmpty(){
        return this.data.isEmpty();
    }

    public static void writeSet(DataOutputStream stream, DataSet set) throws Exception{
        stream.writeInt(set.data.size());

        for(DataPart part : set.data.values()){
            writePart(stream, part);
        }
    }

    public static void readSet(DataInputStream stream, DataSet set) throws Exception{
        int amount = stream.readInt();

        for(int i = 0; i < amount; i++){
            DataPart part = readPart(stream);
            set.data.put(part.getName(), part);
        }
    }

    public static void writePart(DataOutputStream stream, DataPart part) throws Exception{
        stream.writeInt(DataManager.PART_REGISTRY.getId(part.getClass()));
        stream.writeUTF(part.getName());
        part.write(stream);
    }

    public static DataPart readPart(DataInputStream stream) throws Exception{
        int id = stream.readInt();
        String name = stream.readUTF();

        Class<? extends DataPart> partClass = DataManager.PART_REGISTRY.get(id);
        DataPart part = partClass.getConstructor(String.class).newInstance(name);
        part.read(stream);

        return part;
    }
}
