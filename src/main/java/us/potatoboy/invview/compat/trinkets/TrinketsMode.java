package us.potatoboy.invview.compat.trinkets;

enum TrinketsMode {
    REGULAR,
    COSMETIC;

    boolean isCosmetic() {
        return this == COSMETIC;
    }

    TrinketsMode toggled() {
        return isCosmetic() ? REGULAR : COSMETIC;
    }
}
