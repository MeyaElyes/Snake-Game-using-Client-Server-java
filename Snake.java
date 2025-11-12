package common;

import java.io.Serializable;
import java.util.LinkedList;

public class Snake implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id; // client id / name
    public LinkedList<Vec> body = new LinkedList<>();
    public int dir = 1; // 0=up,1=right,2=down,3=left
    public boolean alive = true;
    public int score = 0;

    public Snake() {}

    public Snake(String id, Vec start, int initialLength, int dir) {
        this.id = id;
        this.dir = dir;
        for (int i = 0; i < initialLength; i++) {
            int dx = 0, dy = 0;
            switch (dir) {
                case 0: dy = i; break;
                case 1: dx = -i; break;
                case 2: dy = -i; break;
                case 3: dx = i; break;
            }
            body.add(new Vec(start.x + dx, start.y + dy));
        }
    }
}
