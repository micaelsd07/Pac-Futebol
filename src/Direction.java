public enum Direction {
    UP(0, -1, 90),
    DOWN(0, 1, 270),
    LEFT(-1, 0, 180),
    RIGHT(1, 0, 0),
    NONE(0, 0, 0);

    public final int dx;
    public final int dy;
    public final int angle;

    Direction(int dx, int dy, int angle) {
        this.dx = dx;
        this.dy = dy;
        this.angle = angle;
    }

    public Direction opposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: return NONE;
        }
    }
}
