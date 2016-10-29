import java.awt.*;
import java.awt.image.PackedColorModel;

public class Enemy {

    public float x, y;
    public static final float moveSpeed = 10f;

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void tick(float up) {
        this.x -= moveSpeed;
        this.y += up * moveSpeed * 0.5f;
    }

    public void render(Graphics g) {
        if (x < 0) {
            Driver.enemies.remove(this);
        }
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect((int) x, (int) y, (int) 64, (int) 32);
    }

    public Rectangle getBound() {
        return new Rectangle((int) x, (int) y, (int) 64, (int) 32);
    }

}
