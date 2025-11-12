package common;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ServerUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    public int width, height;
    public List<Apple> apples;
    public Map<String, Snake> snakes;
    public long serverTime;

    public ServerUpdate() {}

    public ServerUpdate(int w, int h, List<Apple> apples, Map<String, Snake> snakes) {
        this.width = w; this.height = h; this.apples = apples; this.snakes = snakes; this.serverTime = System.currentTimeMillis();
    }
}
