package game.enums;

public enum Side {
    NEUTRAL,
    FRIENDLY,
    ENEMY;

    public Side reverse() {
        switch (this) {
            case FRIENDLY:
                return ENEMY;

            case ENEMY:
                return FRIENDLY;

            default:
                return NEUTRAL;
        }
    }
}
