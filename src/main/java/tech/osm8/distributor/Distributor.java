package tech.osm8.distributor;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER;

@Mod("distributor")
public class Distributor {

    public static final String MOD_ID = "distributor";
    private static final Logger LOGGER = LogManager.getLogger();

    public static SimpleChannel networkChannel;

    private static final String PROTOCOL_VERSION = "1";
    public static int packetId = 0;
    public static KeyBinding DISTRIBUTE;

    public Distributor() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetupHandler);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetupHandler);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetupHandler(final FMLCommonSetupEvent event) {
        networkChannel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Distributor.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        networkChannel.registerMessage(packetId++, DistributePacket.class,
                DistributePacket::encode, DistributePacket::decode,
                ServerPacketHandler::onMessageReceived,
                Optional.of(PLAY_TO_SERVER));
    }

    private void clientSetupHandler(final FMLClientSetupEvent event) {
        DISTRIBUTE = new KeyBinding(I18n.format("key.distribute"), InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_X, I18n.format("key.categories.distributor"));
        ClientRegistry.registerKeyBinding(DISTRIBUTE);
    }

}
