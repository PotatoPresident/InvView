package us.potatoboy.invview.compat.trinkets;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import us.potatoboy.invview.gui.ContainerClickValidator;
import us.potatoboy.invview.gui.SavingPlayerDataGui;

import java.util.ArrayList;
import java.util.List;

final class TrinketsGui extends SavingPlayerDataGui implements ContainerClickValidator {
    private static final int PAGE_SIZE = 18;
    private static final int PREVIOUS_PAGE_SLOT = 18;
    private static final int INFO_SLOT = 22;
    private static final int MODE_SLOT = 24;
    private static final int NEXT_PAGE_SLOT = 26;
    private static final String NO_SLOTS_MESSAGE = "Requested player has no trinket slots";
    private static final String EDITING_LABEL = "Trinkets: editing";
    private static final String READ_ONLY_LABEL = "Trinkets: read only";
    private static final String PREVIOUS_PAGE_LABEL = "Previous page";
    private static final String NEXT_PAGE_LABEL = "Next page";
    private static final String REGULAR_MODE_LABEL = "Show regular slots";
    private static final String COSMETIC_MODE_LABEL = "Show cosmetic slots";

    private final TrinketsSession session;
    private final boolean canModify;
    private final TrinketsPageState pageState = new TrinketsPageState();
    private final List<TrinketsSlot> boundSlots = new ArrayList<>();
    private TrinketsLayout layout;
    private String closeReason;
    private boolean removed;
    private boolean dirty;

    TrinketsGui(MinecraftServer server, ServerPlayer viewer, ServerPlayer target, boolean canModify) {
        super(MenuType.GENERIC_9x3, viewer, target);
        this.session = new TrinketsSession(server, target);
        this.canModify = canModify;
    }

    boolean prepare() {
        var candidate = session.readLayout();
        if (candidate.regular().isEmpty()) {
            getPlayer().sendSystemMessage(Component.literal(NO_SLOTS_MESSAGE));
            return false;
        }
        layout = candidate;
        renderPage();
        return true;
    }

    private void unbindSlots() {
        boundSlots.forEach(TrinketsSlot::unbind);
        boundSlots.clear();
    }

    private boolean validateSession() {
        if (removed || closeReason != null) {
            return false;
        }
        closeReason = session.invalidReason(layout);
        if (closeReason != null) {
            unbindSlots();
            return false;
        }
        return true;
    }

    private void resetDrag() {
        if (wrappedMenu != null) {
            // Vanilla resets an ongoing quick-craft on a non-drag input. PICKUP at
            // -1 otherwise does nothing (unlike -999, which drops the cursor stack).
            wrappedMenu.clicked(-1, 0, ContainerInput.PICKUP, getPlayer());
        }
    }

    private boolean isUsable() {
        return closeReason == null && !removed;
    }

    private void renderPage() {
        resetDrag();
        unbindSlots();
        var visible = pageState.visibleSlots(layout, PAGE_SIZE);
        boolean wasOpen = isOpen();
        clearMenuSlots();
        bindVisibleSlots(visible);
        renderNavigation();
        refreshTitleAndContent(wasOpen);
    }

    private void clearMenuSlots() {
        // Old redirects are already inert. Replace every menu position before the
        // final title/content refresh publishes the new layout as a complete state.
        for (int i = 0; i < getSize(); i++) {
            setSlot(i, new GuiElementBuilder(Items.BARRIER).setName(Component.literal(" ")).build());
        }
    }

    private void bindVisibleSlots(List<TrinketSlotAccess> visible) {
        int firstSlotNumber = pageState.page() * PAGE_SIZE;
        var info = new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal((canModify ? EDITING_LABEL : READ_ONLY_LABEL)
                        + " — Page " + (pageState.page() + 1) + "/" + pageState.pageCount(layout, PAGE_SIZE)));
        for (int i = 0; i < visible.size(); i++) {
            var access = visible.get(i);
            var slot = canModify ? new TrinketsSlot(access, () -> dirty = true) : new ReadOnlyTrinketsSlot(access);
            slot.bindToMenuIndex(i);
            boundSlots.add(slot);
            setSlot(i, slot);
            info.addLoreLine(Component.literal((firstSlotNumber + i + 1) + ": " + access.slotType().getId()
                    + " [" + (access.index() + 1) + "]"));
        }
        setSlot(INFO_SLOT, info.build());
    }

    private void renderNavigation() {
        if (pageState.page() > 0) {
            setSlot(PREVIOUS_PAGE_SLOT, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal(PREVIOUS_PAGE_LABEL))
                    .build());
        }
        if (!layout.cosmetic().isEmpty()) {
            setSlot(MODE_SLOT, new GuiElementBuilder(Items.ENDER_EYE)
                    .setName(Component.literal(pageState.mode().isCosmetic()
                            ? REGULAR_MODE_LABEL : COSMETIC_MODE_LABEL))
                    .build());
        }
        if (pageState.page() + 1 < pageState.pageCount(layout, PAGE_SIZE)) {
            setSlot(NEXT_PAGE_SLOT, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal(NEXT_PAGE_LABEL))
                    .build());
        }
    }

    private void refreshTitleAndContent(boolean wasOpen) {
        if (wasOpen) {
            // SGUI's title/content refresh otherwise reuses the old state id.
            wrappedMenu.incrementStateId();
        }
        int pages = pageState.pageCount(layout, PAGE_SIZE);
        var title = session.target().getName().copy()
                .append(pageState.mode().isCosmetic() ? " — Cosmetic trinkets" : " — Trinkets");
        if (pages > 1) {
            title.append(" — " + (pageState.page() + 1) + "/" + pages);
        }
        setTitle(title);
    }

    @Override
    public boolean acceptPacket(ServerboundContainerClickPacket packet) {
        if (packet.containerId() != getSyncId()) {
            return false;
        }
        int index = packet.slotNum();
        boolean validIndex = index == -999 || index == -1
                || (index >= 0 && index < wrappedMenu.slots.size());
        if (!validateSession() || !validIndex || packet.stateId() != wrappedMenu.getStateId()) {
            resetDrag();
            forceUpdateAll();
            return false;
        }
        return true;
    }

    @Override
    public void onTick() {
        if (!removed && !validateSession()) {
            // Never close inside onAnyClick: SGUI is still handling the old menu.
            getPlayer().sendSystemMessage(Component.literal(closeReason));
            close();
        }
    }

    @Override
    public boolean onAnyClick(int index, ClickType type, ContainerInput action) {
        return validateSession() && (canModify || !(getCustomSlot(index) instanceof TrinketsSlot));
    }

    @Override
    public boolean onClick(int index, ClickType type, ContainerInput action, GuiElement element) {
        boolean controlClick = action == ContainerInput.PICKUP
                && (type == ClickType.MOUSE_LEFT || type == ClickType.MOUSE_RIGHT);
        if (isUsable() && controlClick && index == PREVIOUS_PAGE_SLOT
                && pageState.previous(layout, PAGE_SIZE)) {
            renderPage();
        } else if (isUsable() && controlClick && index == NEXT_PAGE_SLOT
                && pageState.next(layout, PAGE_SIZE)) {
            renderPage();
        } else if (isUsable() && controlClick && index == MODE_SLOT
                && !layout.cosmetic().isEmpty()) {
            pageState.toggleMode(layout, PAGE_SIZE);
            renderPage();
        } else if (isOpen()) {
            // SGUI has already consumed the client's predicted changes. Replace
            // them with authoritative content, including the cursor, even when
            // no server-side stack changed (read-only clicks and GUI elements).
            resetDrag();
            forceUpdateAll();
        }
        return false; // Resynchronization is performed above, not by SGUI.
    }

    @Override
    public ItemStack quickMove(int index) {
        if (!canModify || !validateSession()) {
            return ItemStack.EMPTY;
        }
        var slot = getSlotRedirectOrPlayer(index);
        if (slot == null || !slot.isActive() || !slot.mayPickup(getPlayer())) {
            return ItemStack.EMPTY;
        }
        return super.quickMove(index);
    }

    @Override
    public void onRemoved() {
        if (removed) {
            return;
        }
        removed = true;
        unbindSlots();
        // A respawn/reconnected player is a different entity. Never serialize
        // the stale entity over its replacement's player data.
        if (dirty && session.targetIsCurrent()) {
            super.onRemoved();
        }
    }
}
