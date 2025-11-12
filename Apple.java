package common;

import java.io.Serializable;

public class Apple implements Serializable {
    private static final long serialVersionUID = 1L;
    public Vec pos;

    public Apple() {}

    public Apple(Vec pos) { this.pos = pos; }
}
