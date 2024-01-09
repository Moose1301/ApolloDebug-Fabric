package me.moose.apollodebug.client;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.event.InputEvent;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * @author Moose1301
 * @date 1/8/2024
 */
public class ApolloDebugClient implements ClientModInitializer {
    public static final Identifier APOLLO_CHANNEL = new Identifier("lunar", "apollo");
    private KeyBinding keyBinding;
    private boolean debugEnabled;

    private boolean justPressed;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle Debug", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_P, // The keycode of the key
                "Apollo Debug" // The translation key of the keybinding's category.
        ));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("apollodebug")
                .executes(context -> {
                    this.debugEnabled = !this.debugEnabled;
                    context.getSource().sendMessage(
                            Text.literal("Apollo Debug ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN))
                                    .append(Text.literal(debugEnabled ? "Enabled" : "Disabled").setStyle(Style.EMPTY.withColor(debugEnabled ? Formatting.GREEN : Formatting.RED)))
                    );

                    return 0;
                })));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(!keyBinding.isPressed()) {
                this.justPressed = false;
            }
            while (keyBinding.isPressed() && !this.justPressed) {
                this.justPressed = true;
                this.debugEnabled = !this.debugEnabled;
                client.player.sendMessage(
                        Text.literal("Apollo Debug ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN))
                                .append(Text.literal(debugEnabled ? "Enabled" : "Disabled").setStyle(Style.EMPTY.withColor(debugEnabled ? Formatting.GREEN : Formatting.RED)))
                );

            }
        });
        ClientPlayNetworking.registerGlobalReceiver(APOLLO_CHANNEL, (client, handler, buf, responseSender) -> {
            if (!this.debugEnabled) {
                return;
            }
            try {
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                Any any = Any.parseFrom(data);


                // Extract the message name from the type URL
                String[] parts = any.getTypeUrl().split("/");
                String messageName = parts[parts.length - 1];
                // Dynamically load the message class using reflection

                Class<?> messageClass = Class.forName("com." + messageName);
                messageName = messageClass.getSimpleName();
                Descriptors.Descriptor descriptor = (Descriptors.Descriptor) messageClass.getMethod("getDescriptor").invoke(null);
                // Create a DynamicMessage.Builder for the dynamically loaded class
                DynamicMessage message = DynamicMessage.newBuilder(descriptor).mergeFrom(any.getValue()).build();
                String json = JsonFormat.printer().print(message);
                // Merge the Any ins
                client.player.sendMessage(
                        Text.literal("[Apollo Debug] ").setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.AQUA)))
                                .append(Text.literal(messageName).setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GOLD))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(json))))
                                )
                );
                System.out.println(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
