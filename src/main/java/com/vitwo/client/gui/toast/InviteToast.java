package com.vitwo.client.gui.toast;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class InviteToast implements Toast {
    private static final Identifier TOAST_TEXTURE = Identifier.ofVanilla("toast/advancement");
    private final UUID inviterUuid;
    private final String inviterName;
    private static UUID activePendingInviter = null;

    public InviteToast(UUID inviterUuid, String inviterName) {
        this.inviterUuid = inviterUuid;
        this.inviterName = inviterName;
        activePendingInviter = inviterUuid;
    }

    public static boolean hasActivePendingInvite() {
        return activePendingInviter != null;
    }

    public static void clearPendingInvite() {
        activePendingInviter = null;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        // Draw toast background
        context.drawGuiTexture(TOAST_TEXTURE, 0, 0, this.getWidth(), this.getHeight());

        // Draw title
        context.drawText(manager.getClient().textRenderer, Text.translatable("vitwo.toast.invite_title"), 12, 7, 0xFFAA00, false);

        // Draw description with inviter name & keybind prompt
        String desc = inviterName + " [Press Y for Hub]";
        context.drawText(manager.getClient().textRenderer, Text.literal(desc), 12, 18, 0xFFFFFF, false);

        // Show for 10 seconds (10,000 ms)
        if (startTime >= 10000L || activePendingInviter == null) {
            if (activePendingInviter != null && activePendingInviter.equals(inviterUuid)) {
                activePendingInviter = null;
            }
            return Visibility.HIDE;
        }

        return Visibility.SHOW;
    }
}
