package tech.osm8.distributor;


import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class OverlayRenderHelper {
    static List<Pair<Pair<BlockPos, Direction>, ItemStack>> renderList = new ArrayList<>();
    static ItemStack currentItem = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onKeyPress(TickEvent.PlayerTickEvent event) {
        if (Distributor.DISTRIBUTE.isKeyDown()) {
            RayTraceResult result = Minecraft.getInstance().objectMouseOver;
            if (result.getType().equals(RayTraceResult.Type.BLOCK)) {
                BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) result;
                if (Minecraft.getInstance().world.getBlockState(blockRayTraceResult.getPos()).hasTileEntity()) {
                    TileEntity tileEntity = Minecraft.getInstance().world.getTileEntity(blockRayTraceResult.getPos());
                    ItemStack heldItem = Minecraft.getInstance().player.getHeldItem(Hand.MAIN_HAND);
                    if (!currentItem.equals(heldItem) && !currentItem.isEmpty()) {
                        currentItem = ItemStack.EMPTY;
                        renderList.clear();
                        return;
                    }
                    if (tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent() && renderList.stream().noneMatch((x) -> x.getLeft().getLeft().equals(blockRayTraceResult.getPos()))) {
                        currentItem = heldItem;
                        addAndRecalculateAmount(Pair.of(blockRayTraceResult.getPos(), blockRayTraceResult.getFace()), Minecraft.getInstance().player.getHeldItem(Hand.MAIN_HAND));
                    }
                }
            }
        } else {
            if (!renderList.isEmpty()) {
                ArrayList<Pair<BlockPos, ItemStack>> toFill = new ArrayList<>();
                renderList.forEach(x -> toFill.add(Pair.of(x.getLeft().getLeft(), x.getRight())));
                Distributor.networkChannel.sendToServer(new DistributePacket(toFill));
                renderList.clear();
            }
        }
    }

    public static void addAndRecalculateAmount(Pair<BlockPos, Direction> block, ItemStack heldItem) {
        int amount = Math.floorDiv(heldItem.getCount(), (renderList.size() + 1));
        if (amount <= 0) return;
        renderList.add(Pair.of(block, new ItemStack(heldItem.getItem(), amount)));
        renderList.forEach(x -> x.getRight().setCount(amount));
    }

    @SubscribeEvent
    public static void renderWorldLastEvent(RenderWorldLastEvent event) {
        if (event.getPhase() != EventPriority.NORMAL)
            return;

        MatrixStack matrixStack = event.getMatrixStack();
        renderOverlays(renderList, matrixStack);
    }

    public static void renderOverlays(List<Pair<Pair<BlockPos, Direction>, ItemStack>> toRender, MatrixStack matrixStack) {
        List<Pair<Pair<BlockPos, Direction>, ItemStack>> listCopy = new ArrayList<>(toRender);
        for (Pair<Pair<BlockPos, Direction>, ItemStack> item : listCopy) {
            renderOverlay(((Integer) item.getRight().getCount()).toString(), item.getRight(), matrixStack, Vector3d.copy(item.getLeft().getLeft()), item.getLeft().getRight());
        }
    }

    public static void handleViewingAngle(MatrixStack matrixStack, Direction face, boolean isItem) {
        if (face.equals(Direction.NORTH)) {
            matrixStack.rotate(new Quaternion(0f, 1f, 0f, 0f));
            if (isItem) {
                matrixStack.translate(0f, 0f, -2f);
                return;
            }
            matrixStack.translate(-1f, 0f, 1f);
        } else if (face.equals(Direction.EAST)) {
            matrixStack.rotate(new Quaternion(0f, .707f, 0f, .707f));
            if (isItem) {
                matrixStack.translate(-1f, 0f, -1f);
                return;
            }
            matrixStack.translate(0f, 0f, 1f);
        } else if (face.equals(Direction.WEST)) {
            matrixStack.rotate(new Quaternion(0f, -.707f, 0f, .707f));
            if (isItem) {
                matrixStack.translate(1f, 0f, -1f);
                return;
            }
            matrixStack.translate(-1f, 0f, 0f);
        }
        if (face.equals(Direction.UP)) {
            matrixStack.rotate(new Quaternion(-.707f, 0f, 0f, .707f));
            if (isItem) {
                matrixStack.rotate(new Quaternion(1f, 0f, 0f, 0f));
                matrixStack.translate(0f, 1f, -1f);
                return;
            }
            matrixStack.translate(0f, 1f, 0f);
        }
    }

    public static void renderOverlay(String text, ItemStack item, MatrixStack matrixStack, Vector3d renderCoordinates, Direction face) {
        Vector3d projectedView = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();

     //   IRenderTypeBuffer.Impl renderTypeBuffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        renderOverlayText(text, matrixStack, renderTypeBuffer, projectedView, renderCoordinates, face);
        renderOverlayItem(item, matrixStack, renderTypeBuffer, projectedView, renderCoordinates, face);

        renderTypeBuffer.finish();
    }

    public static void renderOverlayItem(ItemStack item, MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Vector3d projectedView, Vector3d renderCoordinates, Direction face) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        matrixStack.push();
        matrixStack.translate(-projectedView.x + renderCoordinates.x, -projectedView.y + renderCoordinates.y + 1, -projectedView.z + renderCoordinates.z + 1);
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        matrixStack.rotate(new Quaternion(0f, 1f, 0f, 0f));
        matrixStack.translate(-1f, -1f, 0f);
        handleViewingAngle(matrixStack, face, true);
        itemRenderer.renderItem(item, ItemCameraTransforms.TransformType.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        matrixStack.pop();
    }

    public static void renderOverlayText(String text, MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Vector3d projectedView, Vector3d renderCoordinates, Direction face) {
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        matrixStack.push();
        matrixStack.translate(-projectedView.x + renderCoordinates.x, -projectedView.y + renderCoordinates.y + 1, -projectedView.z + renderCoordinates.z + 1);
        handleViewingAngle(matrixStack, face, false);
        matrixStack.translate(0f, 0f, 0.025f);
        float f3 = 0.0075F;
        float scale = 4f;
        matrixStack.scale(f3 * scale, f3 * scale, f3);
        matrixStack.rotate(new Quaternion(1f, 0f, 0f, 0f));
        fontRenderer.renderString(fontRenderer.trimStringToWidth(text, 115), 0, 0, 0xffffff, false, matrixStack.getLast().getMatrix(), renderTypeBuffer, false, 0, 140);
        matrixStack.pop();
    }
}
