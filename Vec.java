package common;

import java.io.Serializable;

public class Vec implements Serializable {
    private static final long serialVersionUID = 1L;
    public int x, y;

    public Vec() {}

    public Vec(int x, int y) { this.x = x; this.y = y; }

    public Vec copy() { return new Vec(x,y); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec)) return false;
        Vec v = (Vec) o;
        return x == v.x && y == v.y;
    }

    @Override
    public int hashCode() { return x * 31 + y; }
}
