import java.awt.*;

/**
 * Created by brandon on 10/26/16.
 */
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
        g.setColor(Color.RED);
        g.fillRect((int) x, (int) y, (int) 64, (int) 32);
    }

    public Rectangle getBound() {
        return new Rectangle((int) x, (int) y, (int) 64, (int) 32);
    }

}
