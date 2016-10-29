import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class Driver implements Runnable, MouseListener {

    public static final int GENERATION_SIZE = 500;
    public static final int WIDTH = 600;
    public static final int HEIGHT = 600;
    public static int SEED = 101;

    public static LinkedList<Color[]> speciesGraph = new LinkedList<>();
    public static LinkedList<Snapshot> snapshots = new LinkedList<>();

    Canvas canvas;
    Canvas altCanvas;
    int fastTick = 1;
    int generation = 0, networkSelector = 0, progress = 0;
    Entity[] entities = new Entity[GENERATION_SIZE];
    public static LinkedList<Enemy> enemies = new LinkedList<>();
    public boolean forceBack = true;
    public float playerY = 0;
    public float bigSpec, lilSpec;

    public static final Random levelRandom = new Random(SEED);

    public Driver() {
        JFrame frame = new JFrame("Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas = new Canvas());
        canvas.addMouseListener(this);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        JFrame altFrame = new JFrame("Game Info");
        altFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        altFrame.add(altCanvas = new Canvas());
        altFrame.setSize(HEIGHT, HEIGHT);
        altFrame.setVisible(true);
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new Entity();
        }
        while (firstAltRender) {
            renderAlt();
        }
        Thread thread = new Thread(this);
        thread.start();
    }

    public static void main(String[] args) {
        new Driver();
    }

    public int altFrame = 0;

    public void run() {
        long oldTime = 0;
        while (true) {
            long newTime = System.nanoTime();
            if ((newTime - oldTime > 1000000000 / 60 || (fastTick > 0 && networkSelector != 0)) || fastTick > 1) {
                oldTime = newTime;
                tick();
            }
            render();
            altFrame++;
            if (altFrame % 60 == 0) {
                renderAlt();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void tick() {
        if (networkSelector < entities.length) {
            progress++;
            Entity entity = entities[networkSelector];
            MLP network = entity.network;
            Point[] points = entity.inputs;
            float[] inputs = new float[points.length];
            for (int i = 0; i < inputs.length; i++) {
                for (Enemy enemy : enemies) {
                    if (enemy.getBound().contains(points[i])) {
                        inputs[i] = 1;
                    }
                }
            }
            float[] out = network.run(inputs);
            //boolean shoot = out[1] >= 0.5f;
            float up = out[0] * 2 - 1;
            playerY += up * Enemy.moveSpeed / 2;
            if (levelRandom.nextInt(4) == 0) {
                enemies.add(new Enemy(canvas.getWidth(), levelRandom.nextInt(canvas.getHeight()) * 3 - canvas.getHeight() / 2));
            }
            boolean nextEntity = false;
            for (int i = 0; i < enemies.size(); i++) {
                enemies.get(i).tick(up);
                if (enemies.get(i).getBound().intersects(getBound())) {
                    nextEntity = true;
                }
            }
            if (nextEntity) {
                playerY = 0;
                enemies.clear();
                levelRandom.setSeed(SEED);
                entity.fitness = progress;
                progress = 0;
                if (networkSelector % 10 == 0)
                    System.out.print(networkSelector + " ");
                networkSelector++;
                forceBack = true;
            }
        } else {
            playerY = 0;
            if (networkSelector % 10 == 0)
                System.out.print(networkSelector + " ");
            generation++;
            nextGen();
            enemies.clear();
            levelRandom.setSeed(SEED);
            progress = 0;
            networkSelector = 0;
            forceBack = true;
        }
    }

    public void render() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) {
            canvas.createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();

        if (fastTick < 2 || forceBack) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            forceBack = false;
        }

        g.setColor(Color.GRAY);
        g.drawLine(0, (int) playerY % (HEIGHT / 2) - HEIGHT / 2, WIDTH, (int) playerY % (HEIGHT / 2) - HEIGHT / 2);
        g.drawLine(0, (int) playerY % (HEIGHT / 2) + HEIGHT / 2, WIDTH, (int) playerY % (HEIGHT / 2) + HEIGHT / 2);
        g.drawLine(0, (int) playerY % (HEIGHT / 2) + HEIGHT, WIDTH, (int) playerY % (HEIGHT / 2) + HEIGHT);
        g.drawLine(WIDTH - (int) (progress * Enemy.moveSpeed) % WIDTH, 0, WIDTH - (int) (progress * Enemy.moveSpeed) % WIDTH, HEIGHT);
        g.drawLine(WIDTH - (int) (progress * Enemy.moveSpeed + WIDTH / 2) % WIDTH, 0, WIDTH - (int) (progress * Enemy.moveSpeed + WIDTH / 2) % WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        Rectangle b = getBound();
        g.fillRect(b.x, b.y, 64, 32);

        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).render(g);
        }
        if (networkSelector < entities.length) {
            Point[] points = entities[networkSelector].inputs;
            for (Point point : points) {
                g.setColor(Color.GRAY);
                g.fillRect(point.x, point.y, 2, 2);
            }
        }

        g.dispose();
        bs.show();
    }

    boolean firstAltRender = true;

    public void renderAlt() {
        BufferStrategy bs = altCanvas.getBufferStrategy();
        if (bs == null) {
            altCanvas.createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, altCanvas.getWidth(), altCanvas.getHeight());
        firstAltRender = false;

        int H = altCanvas.getHeight();
        int W = altCanvas.getWidth();

        int scaleH = (W - 64) / (snapshots.size() + 1);

        for (int i = 0; i < speciesGraph.size(); i++) {
            for (int j = 0; j < speciesGraph.get(i).length; j++) {
                Color c = speciesGraph.get(i)[j];
                g.setColor(c);
                if (c.equals(Color.BLACK)) {
                    g.fillRect(i * scaleH + 2, (int) (j * (160f / GENERATION_SIZE)) + 1, scaleH, (160 / GENERATION_SIZE) + 2);
                } else {
                    g.fillRect(i * scaleH + 2, (int) (j * (160f / GENERATION_SIZE)) + 2, scaleH, (160 / GENERATION_SIZE) + 1);
                }
            }
        }

        g.setColor(Color.GRAY);
        g.drawLine(0, H - 64, W, H - 64);
        g.fillRect(0, H - 64 + 2, (int) (((float) networkSelector / entities.length) * W), 10);

        float scaleDown = 5;
        if (snapshots.size() > 1) {
            scaleDown = 300f / snapshots.getLast().best;
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(snapshots.getLast().best),(snapshots.size()-1) * scaleH,(int) (H - 64 - snapshots.getLast().best * scaleDown));
            g.drawString(String.valueOf(snapshots.getLast().worst),(snapshots.size()-1) * scaleH,(int) (H - 64 - snapshots.getLast().worst * scaleDown));
            g.setColor(Color.RED);
            g.drawString(String.valueOf(snapshots.getLast().median),(snapshots.size()-1) * scaleH,(int) (H - 64 - snapshots.getLast().median * scaleDown));
        }

        for (int i = 1; i < snapshots.size(); i++) {
            g.setColor(Color.WHITE);
            g.drawLine((i - 1) * scaleH, (int) (H - 64 - snapshots.get(i - 1).best * scaleDown), i * scaleH, (int) (H - 64 - snapshots.get(i).best * scaleDown));
            g.drawLine((i - 1) * scaleH, (int) (H - 64 - snapshots.get(i - 1).worst * scaleDown), i * scaleH, (int) (H - 64 - snapshots.get(i).worst * scaleDown));
            g.setColor(Color.RED);
            g.drawLine((i - 1) * scaleH, (int) (H - 64 - snapshots.get(i - 1).median * scaleDown), i * scaleH, (int) (H - 64 - snapshots.get(i).median * scaleDown));
        }

        g.dispose();
        bs.show();
    }

    public void nextGen() {
        //if (generation % 10 == 0)
        //    SEED++;
        float[] speciesf = new float[entities.length];
        for (int i = 0; i < entities.length; i++) {
            float speciess = entities[i].getSpecies();
            speciesf[i] = speciess;
        }
        Arrays.sort(speciesf);
        int[] fits = new int[entities.length];
        Color[] species = new Color[entities.length];
        for (int i = 0; i < entities.length; i++) {
            float speciess = speciesf[i];
            if (speciesGraph.size() < 1) {
                bigSpec = speciesf[speciesf.length - 1];
                lilSpec = speciesf[0];
            }
            Color c = Color.getHSBColor(((speciess - bigSpec) / (bigSpec - lilSpec)) * 1.9f, 1, 1);
            if (i > GENERATION_SIZE / 100 && speciesf[i] - speciesf[i - GENERATION_SIZE / 100] > 0.008f) {
                c = Color.BLACK;
            }
            species[i] = c; // new Color((int) (255f * speciess * 5) % 256, (int) (255f * speciess * 25) % 256, (int) (255f * speciess * 250) % 256);
        }
        speciesGraph.add(species);
        //SEED++;
        Entity best = entities[0];
        Entity worst = entities[0];
        LinkedList<Entity> newGen = new LinkedList<>();
        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            fits[i] = entity.fitness;
            if (entity.fitness > best.fitness) {
                best = entity;
            }
            if (entity.fitness < worst.fitness) {
                worst = entity;
            }
        }
        Arrays.sort(fits);
        Snapshot snapshot = new Snapshot(best.fitness, fits[fits.length / 2], worst.fitness);
        snapshots.add(snapshot);
        System.out.println("Generation " + generation + " complete. " + "BEST: " + best.fitness);
        int averageFit = fits[fits.length / 2];
        newGen.add(best.breed(0));
        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            if (entity.fitness >= averageFit) {
                newGen.add(entity.breed(1f / (generation / 10f + 1)));
            }
        }
        while (newGen.size() < GENERATION_SIZE) {
            newGen.add(newGen.get(MLP.random.nextInt(newGen.size())).breed(1f / (generation / 10f + 1)));
        }
        for (int i = 0; i < entities.length; i++) {
            entities[i] = newGen.get(i);
        }
    }

    public Rectangle getBound() {
        return new Rectangle(0, canvas.getHeight() / 2 - 16, 64, 32);
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        fastTick = (fastTick + 1) % 3;
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    public static String hsvToRgb(float hue, float saturation, float value) {

        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        switch (h) {
            case 0:
                return rgbToString(value, t, p);
            case 1:
                return rgbToString(q, value, p);
            case 2:
                return rgbToString(p, value, t);
            case 3:
                return rgbToString(p, q, value);
            case 4:
                return rgbToString(t, p, value);
            case 5:
                return rgbToString(value, p, q);
            default:
                throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }
    }

    public static String rgbToString(float r, float g, float b) {
        String rs = Integer.toHexString((int) (r * 256));
        String gs = Integer.toHexString((int) (g * 256));
        String bs = Integer.toHexString((int) (b * 256));
        return rs + gs + bs;
    }
}
