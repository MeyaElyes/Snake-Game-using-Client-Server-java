package common;

import java.io.Serializable;

public class ClientInput implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name; // sent once when connecting
    public int dir = -1; // direction change; -1 = no change
    public long ts = 0; // timestamp for pinging/ordering

    public ClientInput() {}

    public ClientInput(String name) { this.name = name; }
}
