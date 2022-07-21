package mydisruptor.falsesharing;

public class Point {

    public volatile int x;
    public volatile int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
