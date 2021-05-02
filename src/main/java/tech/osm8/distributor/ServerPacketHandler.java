package tech.osm8.distributor;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ServerPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void onMessageReceived(DistributePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            LOGGER.warn("DistributePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }
        if (!msg.isMessageValid()) {
            LOGGER.warn("DistributePacket was invalid" + msg);
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            LOGGER.warn("EntityPlayerMP was null when DistributePacket was received");
        }
        ctx.enqueueWork(() -> processMessage(msg, sendingPlayer));
    }

    static void processMessage(DistributePacket msg, ServerPlayerEntity sender) {
        ArrayList<Pair<BlockPos, ItemStack>> blocks = msg.blockList;
        for (Pair<BlockPos, ItemStack> block : blocks) {
            TileEntity tileEntity = sender.getServerWorld().getTileEntity(block.getLeft());
            if (block.getRight().getCount() * blocks.size() > 64 || !sender.getServerWorld().isAreaLoaded(block.getLeft(), 1) || tileEntity == null)
//                || checkRange(block.getLeft(), new Vector3d(sender.lastTickPosX, sender.lastTickPosY, sender.lastTickPosZ), 15)
                return;
            LazyOptional<IItemHandler> itemCap = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
            itemCap.ifPresent(x -> {
                ItemStack currItem = sender.inventory.getCurrentItem();
                if (currItem.getItem().equals(block.getRight().getItem()) && currItem.getCount() >= block.getRight().getCount()) {
                    int notInserted = insertToFirstAvailable(x, block.getRight());
                    currItem.shrink(block.getRight().getCount() - notInserted);
                }
            });
        }
    }

    static int insertToFirstAvailable(IItemHandler itemHandler, ItemStack itemStack) {
        ItemStack copyStack = itemStack.copy();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.insertItem(i, copyStack, true).equals(copyStack)) {
                copyStack = itemHandler.insertItem(i, copyStack, false);
                if(copyStack.getCount() > 0){
                    copyStack.setCount(insertToFirstAvailable(itemHandler, copyStack));
                }
                break;
            }
        }
        return copyStack.getCount();
    }

    static boolean checkRange(Vector3i blockPos, Vector3d playerPos, float distance) {
        double d0 = playerPos.x - blockPos.getX();
        double d1 = playerPos.y - blockPos.getY();
        double d2 = playerPos.z - blockPos.getZ();
        return d0 * d0 + d1 * d1 + d2 * d2 < distance * distance;
    }
}
