package mydisruptor.falsesharing;

public class PointNoFalseSharing {

    private long lp1, lp2, lp3, lp4, lp5, lp6, lp7;
    public volatile long x;
    private long rp1, rp2, rp3, rp4, rp5, rp6, rp7;

    public volatile long y;

    public PointNoFalseSharing(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
