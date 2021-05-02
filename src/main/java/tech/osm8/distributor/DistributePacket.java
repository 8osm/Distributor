package tech.osm8.distributor;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;

public class DistributePacket {
    ArrayList<Pair<BlockPos, ItemStack>> blockList;

    public DistributePacket(ArrayList<Pair<BlockPos, ItemStack>> blockList) {
        this.blockList = blockList;
        messageIsValid = true;
    }

    public boolean isMessageValid() {
        return messageIsValid;
    }

    private DistributePacket() {
        messageIsValid = false;
    }

    public static DistributePacket decode(PacketBuffer buf) {
        DistributePacket result = new DistributePacket();
        try {
            int size = buf.readInt();
            result.blockList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                BlockPos blockPos = buf.readBlockPos();
                ItemStack itemStack = buf.readItemStack();
                result.blockList.add(Pair.of(blockPos, itemStack));
            }


        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            LOGGER.warn("Exception while reading DistributePacket: " + e);
            return result;
        }
        result.messageIsValid = true;
        return result;
    }

    public void encode(PacketBuffer buf) {
        if (!messageIsValid) return;
        buf.writeInt(blockList.size());
        for (Pair<BlockPos, ItemStack> item : blockList) {
            buf.writeBlockPos(item.getLeft());
            buf.writeItemStack(item.getRight());
        }
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder("DistributePacket[size= " + blockList.size() + ",items = [ ");
        for (Pair<BlockPos, ItemStack> item : blockList) {
            toString.append(String.format("{ x= %d, y= %d, z= %d, item= %s, count= %d }, ", item.getLeft().getX(), item.getLeft().getY(), item.getLeft().getZ(), item.getRight().getItem().getName().getString(), item.getRight().getCount()));
        }
        return toString.toString();
    }

    private boolean messageIsValid;

    private static final Logger LOGGER = LogManager.getLogger();
}
