package client;

import common.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class GameClient {
    private final String host;
    private final int port;
    private final String name;
    private Socket sock;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private AtomicReference<ServerUpdate> latest = new AtomicReference<>();
    private long lastFrameCount = 0;
    private int frames = 0;
    // track locally the last known direction for this client to prevent sending 180-degree reversals
    private volatile int localDir = -1;

    public GameClient(String host, int port, String name) {
        this.host = host; this.port = port; this.name = name;
    }

    public void start() throws Exception {
        sock = new Socket(host, port);
        out = new ObjectOutputStream(sock.getOutputStream());
        in = new ObjectInputStream(sock.getInputStream());

        // send initial name
        out.writeObject(new ClientInput(name));
        out.flush();

        // reader thread
        new Thread(this::readerLoop).start();

        SwingUtilities.invokeLater(this::createGui);
    }

    private void readerLoop() {
        try {
            while (true) {
                Object o = in.readObject();
                if (o instanceof ServerUpdate) {
                    ServerUpdate su = (ServerUpdate) o;
                    latest.set(su);
                    // update our localDir from server authoritative state if present
                    if (su.snakes != null && su.snakes.containsKey(name)) {
                        Snake me = su.snakes.get(name);
                        if (me != null) localDir = me.dir;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Disconnected from server: " + ex.getMessage());
        }
    }

    private void sendDir(int dir) {
        try {
            ClientInput ci = new ClientInput(); ci.dir = dir; ci.ts = System.currentTimeMillis();
            out.writeObject(ci);
            out.flush();
        } catch (Exception ignored) {}
    }

    private void createGui() {
        JFrame frame = new JFrame("Snake Client - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 640);

        GamePanel gp = new GamePanel();
        frame.add(gp, BorderLayout.CENTER);

        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                int want = -1;
                if (k == KeyEvent.VK_UP) want = 0;
                if (k == KeyEvent.VK_RIGHT) want = 1;
                if (k == KeyEvent.VK_DOWN) want = 2;
                if (k == KeyEvent.VK_LEFT) want = 3;
                if (want >= 0) {
                    // if we don't yet know localDir, still send (server will validate)
                    if (localDir >= 0 && isOpposite(localDir, want)) {
                        // ignore immediate 180-degree reversal locally
                        return;
                    }
                    // update localDir optimistically and send
                    localDir = want;
                    sendDir(want);
                }
            }
        });

        Timer t = new Timer(40, ev -> { gp.repaint(); frames++; });
        t.start();

        // fps counter
        new Timer(1000, ev -> { frames = 0; }).start();

        frame.setVisible(true);
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ServerUpdate su = latest.get();
            if (su == null) {
                g.drawString("Waiting for server...", 20, 20); return;
            }

            int cellW = Math.max(4, getWidth() / su.width);
            int cellH = Math.max(4, getHeight() / su.height);

            // background
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(),getHeight());

            // apples
            g.setColor(Color.RED);
            for (Apple a : su.apples) {
                g.fillOval(a.pos.x * cellW, a.pos.y * cellH, cellW, cellH);
            }

            // snakes
            for (Map.Entry<String, Snake> e : su.snakes.entrySet()) {
                Snake s = e.getValue();
                if (!s.alive) g.setColor(Color.DARK_GRAY);
                else if (s.id.equals(name)) g.setColor(Color.GREEN);
                else g.setColor(Color.RED);

                for (Vec v : s.body) {
                    g.fillRect(v.x * cellW, v.y * cellH, cellW, cellH);
                }
            }

            // HUD: list players
            int x = getWidth() - 180; int y = 10;
            g.setColor(new Color(0,0,0,160));
            g.fillRect(x-6, y-6, 176, 120);
            g.setColor(Color.WHITE);
            g.drawString("Players:", x, y);
            int yy = y + 16;
            for (Snake s : su.snakes.values()) {
                g.drawString(s.id + " - " + s.score + (s.alive ? "" : " (dead)"), x, yy);
                yy += 14;
            }

            // ping & frames
            g.drawString("Server time: " + su.serverTime, 10, 20);
            g.drawString("FPS: " + frames, 10, 36);
        }
    }

    private boolean isOpposite(int a, int b) {
        return ((a + 2) % 4) == b;
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 12345;
        String name = "player" + (int)(Math.random()*1000);
        if (args.length >= 1) name = args[0];
        if (args.length >= 2) host = args[1];
        GameClient gc = new GameClient(host, port, name);
        gc.start();
    }
}
