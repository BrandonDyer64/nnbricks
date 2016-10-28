import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class Driver implements Runnable, MouseListener {

    public static final int GENERATION_SIZE = 200;
    public static final int WIDTH = 300;
    public static final int HEIGHT = 600;
    public static int SEED = 101;

    public static LinkedList<Color[]> speciesGraph = new LinkedList<>();
    public static LinkedList<Snapshot> snapshots = new LinkedList<>();

    Canvas canvas;
    Canvas altCanvas;
    boolean fastTick = false;
    int generation = 0, networkSelector = 0, progress = 0;
    Entity[] entities = new Entity[GENERATION_SIZE];
    public LinkedList<Enemy> enemies = new LinkedList<>();
    public boolean forceBack = true;

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
            if (newTime - oldTime > 1000000000 / 60 || fastTick) {
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
            boolean shoot = out[1] >= 0.5f;
            float up = out[0] * 2 - 1;
            if (levelRandom.nextInt(15) == 0) {
                enemies.add(new Enemy(canvas.getWidth(), levelRandom.nextInt(canvas.getHeight())));
            }
            boolean nextEntity = false;
            for (int i = 0; i < enemies.size(); i++) {
                enemies.get(i).tick(up);
                if (enemies.get(i).getBound().intersects(getBound())) {
                    nextEntity = true;
                }
            }
            if (nextEntity) {
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

        if (!fastTick || forceBack) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            forceBack = false;
        }
        g.setColor(Color.green);
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

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, altCanvas.getWidth(), altCanvas.getHeight());
        firstAltRender = false;

        int scaleH = HEIGHT / (snapshots.size() + 1);

        for (int i = 0; i < speciesGraph.size(); i++) {
            for (int j = 0; j < speciesGraph.get(i).length; j++) {
                Color c = speciesGraph.get(i)[j];
                g.setColor(c);
                g.fillRect(i * scaleH + 2, j * (160 / GENERATION_SIZE + 1) + 2, scaleH, (160 / GENERATION_SIZE) + 1);
            }
        }

        g.setColor(Color.GRAY);
        g.drawLine(0, HEIGHT - 64, HEIGHT, HEIGHT - 64);

        float scaleDown = 5;
        if (snapshots.size() > 1) {
            scaleDown = 200f / snapshots.getLast().best;
        }

        for (int i = 1; i < snapshots.size(); i++) {
            g.setColor(Color.WHITE);
            g.drawLine((i - 1) * scaleH, (int) (HEIGHT - 64 - snapshots.get(i - 1).best * scaleDown), i * scaleH, (int) (HEIGHT - 64 - snapshots.get(i).best * scaleDown));
            g.drawLine((i - 1) * scaleH, (int) (HEIGHT - 64 - snapshots.get(i - 1).worst * scaleDown), i * scaleH, (int) (HEIGHT - 64 - snapshots.get(i).worst * scaleDown));
            g.setColor(Color.RED);
            g.drawLine((i - 1) * scaleH, (int) (HEIGHT - 64 - snapshots.get(i - 1).median * scaleDown), i * scaleH, (int) (HEIGHT - 64 - snapshots.get(i).median * scaleDown));
        }

        g.dispose();
        bs.show();
    }

    public void nextGen() {
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
            species[i] = new Color((int) (255f * speciess * 5) % 256, (int) (255f * speciess * 25) % 256, (int) (255f * speciess * 250) % 256);
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
        int averageFit = (best.fitness + worst.fitness) / 2;
        newGen.add(best.breed(0));
        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            if (entity.fitness >= averageFit) {
                newGen.add(entity.breed(1f / (generation / 10f)));
            }
        }
        while (newGen.size() < GENERATION_SIZE) {
            newGen.add(newGen.get(MLP.random.nextInt(newGen.size())).breed(1f / (generation / 10f)));
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
        fastTick = !fastTick;
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
}
