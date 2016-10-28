import java.awt.*;
import java.util.Random;

public class Entity {

    private static Random random = new Random();

    public MLP network;
    public Point[] inputs = new Point[16];
    public int fitness = 0;

    public Entity() {
        network = new MLP(inputs.length, new int[]{5, 2});
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new Point(MLP.random.nextInt(Driver.WIDTH), MLP.random.nextInt(Driver.HEIGHT));
        }
    }

    private Entity(MLP network, Point[] inputs) {
        this.network = network;
        this.inputs = inputs;
    }

    public Entity breed(float rate) {
        Point[] newInputs = new Point[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            Point p = new Point(inputs[i].x, inputs[i].y);
            newInputs[i] = p;
            if (rate > 0) {
                p.x += random.nextInt(3) - 1;
                p.y += random.nextInt(3) - 1;
            }
        }
        return new Entity(network.breed(rate), newInputs);
    }

    public float getSpecies() {
        int avgX = 0;
        int avgY = 0;
        for (Point point : inputs) {
            avgX += point.x;
            avgY += point.y;
        }
        avgX /= inputs.length;
        avgY /= inputs.length;
        float x = (float) avgX / Driver.WIDTH;
        float y = (float) avgY / Driver.HEIGHT;
        return (x + y) / 2f;
    }

}
