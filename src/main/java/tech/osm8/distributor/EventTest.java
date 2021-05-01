package tech.osm8.distributor;


import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.system.CallbackI;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class EventTest {

    @SubscribeEvent
    public static void renderWorldLastEvent(RenderWorldLastEvent event) {
        if (event.getPhase() != EventPriority.NORMAL || !Distributor.DISTRIBUTE.isKeyDown())
            return;
        // Get instances of the classes required for a block render.
        World world = Minecraft.getInstance().world;
        MatrixStack matrixStack = event.getMatrixStack();

        // Get the projected view coordinates.
        Vector3d projectedView = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();

        // Begin rendering the block.
        IRenderTypeBuffer.Impl renderTypeBuffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        RayTraceResult result = Minecraft.getInstance().objectMouseOver;

        if (result.getType().equals(RayTraceResult.Type.BLOCK)) {
            BlockRayTraceResult resultBlock = (BlockRayTraceResult) result;
            Vector3d coords = new Vector3d(resultBlock.getPos().getX(), resultBlock.getPos().getY(), resultBlock.getPos().getZ());
            Direction face = resultBlock.getFace();
            renderBlock(matrixStack, renderTypeBuffer, projectedView, coords, face);
        }
        renderTypeBuffer.finish();
    }

    public static void renderBlock(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, Vector3d projectedView, Vector3d renderCoordinates, Direction face) {
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        matrixStack.push();
        matrixStack.translate(-projectedView.x + renderCoordinates.x, -projectedView.y + renderCoordinates.y + 1, -projectedView.z + renderCoordinates.z + 1);
        if (face.equals(Direction.NORTH)) {
            matrixStack.rotate(new Quaternion(0f, 1f, 0f, 0f));
            matrixStack.translate(-1f, 0f, 1f);
        } else if (face.equals(Direction.EAST)) {
            matrixStack.rotate(new Quaternion(0f, .707f, 0f, .707f));
            matrixStack.translate(0f, 0f, 1f);
        } else if (face.equals(Direction.WEST)) {
            matrixStack.rotate(new Quaternion(0f, -.707f, 0f, .707f));
            matrixStack.translate(-1f, 0f, 0f);
        } if (face.equals(Direction.UP)) {
            matrixStack.rotate(new Quaternion(-.707f, 0f, 0f, .707f));
            matrixStack.translate(0f, 1f, 0f);
        }
        matrixStack.translate(0f, 0f, 0.025f);
        float f3 = 0.0075F;
        float scale = 4f;
        matrixStack.scale(f3 * scale, -f3 * scale, f3);

        fontRenderer.renderString(fontRenderer.trimStringToWidth("fuck", 115), 0, 0, 0xffffff, false, matrixStack.getLast().getMatrix(), renderTypeBuffer, false, 0, 140);
        matrixStack.pop();
        matrixStack.push();
        matrixStack.translate(-projectedView.x + renderCoordinates.x, -projectedView.y + renderCoordinates.y + 1, -projectedView.z + renderCoordinates.z + 1);
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        matrixStack.rotate(new Quaternion(0f,1f,0f,0f));
        matrixStack.translate(-1f, -1f, 0f);
        ItemStack heldItem = Minecraft.getInstance().player.getHeldItem(Hand.MAIN_HAND);
        itemRenderer.renderItem(heldItem, ItemCameraTransforms.TransformType.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        matrixStack.pop();
    }

}
