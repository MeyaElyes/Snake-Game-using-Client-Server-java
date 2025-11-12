package server;

import common.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private final int port;
    private final int width = 40;
    private final int height = 30;
    private final Map<String, Snake> snakes = Collections.synchronizedMap(new LinkedHashMap<>());
    private final List<Apple> apples = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final Random rand = new Random();

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // spawn some apples
        for (int i = 0; i < 5; i++) spawnApple();

        ScheduledExecutorService tickExec = Executors.newSingleThreadScheduledExecutor();
        tickExec.scheduleAtFixedRate(this::tick, 0, 150, TimeUnit.MILLISECONDS); // ~6.6 ticks/sec

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server listening on " + port);
            while (true) {
                Socket s = ss.accept();
                ClientHandler ch = new ClientHandler(s);
                clients.add(ch);
                new Thread(ch).start();
            }
        }
    }

    private void spawnApple() {
        Vec p;
        do {
            p = new Vec(rand.nextInt(width), rand.nextInt(height));
        } while (isOccupied(p));
        apples.add(new Apple(p));
    }

    private boolean isOccupied(Vec p) {
        synchronized (snakes) {
            for (Snake s : snakes.values()) {
                for (Vec v : s.body) if (v.equals(p)) return true;
            }
        }
        synchronized (apples) {
            for (Apple a : apples) if (a.pos.equals(p)) return true;
        }
        return false;
    }

    private void tick() {
        // move snakes
        synchronized (snakes) {
            for (Snake s : new ArrayList<>(snakes.values())) {
                if (!s.alive) continue;
                Vec head = s.body.getFirst();
                Vec nh = moveHead(head, s.dir);

                // determine if this move will eat an apple (so tail will not move)
                boolean willEat = false;
                synchronized (apples) {
                    for (Apple a : apples) { if (a.pos.equals(nh)) { willEat = true; break; } }
                }

                // self or others collision. allow moving into own tail if tail will move away (!willEat)
                boolean collided = false;
                for (Snake other : snakes.values()) {
                    int len = other.body.size();
                    for (int i = 0; i < len; i++) {
                        Vec seg = other.body.get(i);
                        if (other == s && i == len - 1 && !willEat) continue; // tail will vacate
                        if (seg.equals(nh)) { collided = true; break; }
                    }
                    if (collided) break;
                }
                if (collided) { s.alive = false; continue; }

                s.body.addFirst(nh);

                // apple eating
                boolean ate = false;
                synchronized (apples) {
                    Iterator<Apple> it = apples.iterator();
                    while (it.hasNext()) {
                        Apple a = it.next();
                        if (a.pos.equals(nh)) {
                            it.remove();
                            s.score += 1;
                            ate = true;
                            spawnApple();
                            break;
                        }
                    }
                }

                if (!ate) s.body.removeLast();
            }
        }

        broadcastState();
    }

    private Vec moveHead(Vec h, int dir) {
        int dx = 0, dy = 0;
        switch (dir) {
            case 0: dy = -1; break;
            case 1: dx = 1; break;
            case 2: dy = 1; break;
            case 3: dx = -1; break;
        }
        int nx = (h.x + dx) % width;
        int ny = (h.y + dy) % height;
        if (nx < 0) nx += width;
        if (ny < 0) ny += height;
        return new Vec(nx, ny);
    }

    // return true if b is the opposite direction of a (180 degrees)
    private boolean isOpposite(int a, int b) {
        return ((a + 2) % 4) == b;
    }

    private void broadcastState() {
        ServerUpdate su;
        synchronized (snakes) {
            synchronized (apples) {
                su = new ServerUpdate(width, height, new ArrayList<>(apples), deepCopySnakes());
            }
        }
        synchronized (clients) {
            Iterator<ClientHandler> it = clients.iterator();
            while (it.hasNext()) {
                ClientHandler ch = it.next();
                if (!ch.isAlive()) { it.remove(); continue; }
                try {
                    ch.sendUpdate(su);
                } catch (Exception ex) {
                    System.out.println("Error sending update: " + ex.getMessage());
                    ch.close();
                    it.remove();
                }
            }
        }
    }

    private Map<String, Snake> deepCopySnakes() {
        Map<String, Snake> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Snake> e : snakes.entrySet()) {
            Snake s = e.getValue();
            Snake ns = new Snake();
            ns.id = s.id; ns.dir = s.dir; ns.alive = s.alive; ns.score = s.score;
            ns.body = new LinkedList<>();
            for (Vec v : s.body) ns.body.add(v.copy());
            copy.put(e.getKey(), ns);
        }
        return copy;
    }

    class ClientHandler implements Runnable {
        private Socket sock;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientId;
        private volatile boolean alive = true;

        ClientHandler(Socket s) {
            this.sock = s;
        }

        public boolean isAlive() { return alive && !sock.isClosed(); }

        public void close() {
            alive = false;
            try { sock.close(); } catch (Exception ignored) {}
        }

        public void sendUpdate(ServerUpdate su) throws Exception {
            out.writeObject(su);
            out.reset();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(sock.getOutputStream());
                in = new ObjectInputStream(sock.getInputStream());

                // expect first message to be ClientInput with name
                Object o = in.readObject();
                if (o instanceof ClientInput) {
                    ClientInput ci = (ClientInput) o;
                    clientId = ci.name != null ? ci.name : "player" + rand.nextInt(1000);
                    // create a snake for this client
                    Vec start = new Vec(rand.nextInt(width), rand.nextInt(height));
                    Snake s = new Snake(clientId, start, 4, rand.nextInt(4));
                    synchronized (snakes) { snakes.put(clientId, s); }
                    System.out.println("Player connected: " + clientId);
                } else {
                    close(); return;
                }

                while (isAlive()) {
                    Object inObj = in.readObject();
                    if (inObj instanceof ClientInput) {
                        ClientInput ci = (ClientInput) inObj;
                        if (ci.dir >= 0 && clientId != null) {
                            Snake s = snakes.get(clientId);
                            if (s != null) {
                                synchronized (s) {
                                    // prevent immediate 180-degree reversal which would collide with own neck
                                    if (!isOpposite(s.dir, ci.dir)) {
                                        s.dir = ci.dir;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // client disconnected
            } finally {
                if (clientId != null) {
                    synchronized (snakes) { snakes.remove(clientId); }
                }
                close();
                System.out.println("Client handler closed for " + clientId);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 12345;
        GameServer gs = new GameServer(port);
        gs.start();
    }
}
