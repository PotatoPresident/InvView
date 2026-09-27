package us.potatoboy.invview.compat.trinkets;

import eu.pb4.trinkets.api.TrinketSlotAccess;

import java.util.List;

final class TrinketsPageState {
    private TrinketsMode mode = TrinketsMode.REGULAR;
    private int page;

    TrinketsMode mode() {
        return mode;
    }

    int page() {
        return page;
    }

    int pageCount(TrinketsLayout layout, int pageSize) {
        return Math.max(1, layout.pageCount(mode, pageSize));
    }

    List<TrinketSlotAccess> visibleSlots(TrinketsLayout layout, int pageSize) {
        clamp(layout, pageSize);
        return layout.page(mode, page, pageSize);
    }

    boolean previous(TrinketsLayout layout, int pageSize) {
        clamp(layout, pageSize);
        if (page == 0) {
            return false;
        }
        page--;
        return true;
    }

    boolean next(TrinketsLayout layout, int pageSize) {
        clamp(layout, pageSize);
        if (page + 1 >= pageCount(layout, pageSize)) {
            return false;
        }
        page++;
        return true;
    }

    void toggleMode(TrinketsLayout layout, int pageSize) {
        mode = mode.toggled();
        page = 0;
        clamp(layout, pageSize);
    }

    private void clamp(TrinketsLayout layout, int pageSize) {
        page = Math.clamp(page, 0, pageCount(layout, pageSize) - 1);
    }
}
