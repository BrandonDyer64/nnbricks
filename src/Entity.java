import java.awt.*;
import java.util.Random;

public class Entity {

    public MLP network;
    public Point[] inputs = new Point[32];
    public int fitness = 0;

    public Entity() {
        network = new MLP(inputs.length, new int[]{4, 1});
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new Point(MLP.random.nextInt(Driver.WIDTH / 2), MLP.random.nextInt(Driver.HEIGHT / 2) + Driver.HEIGHT / 4);
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
            if (rate > 0 && MLP.random.nextFloat() < rate) {
                p.x += MLP.random.nextInt(3) - 1;
                p.y += MLP.random.nextInt(3) - 1;
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
