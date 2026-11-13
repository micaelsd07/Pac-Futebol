import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class VisualEffect {

    public static class Particle {
        private double x, y;
        private double vx, vy;
        private float alpha;
        private float decay;
        private float size;
        private Color color;

        public Particle(double x, double y, double vx, double vy, float size, float decay, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.decay = decay;
            this.alpha = 1.0f;
            this.color = color;
        }

        public boolean update() {
            x += vx;
            y += vy;
            vx *= 0.95;
            vy *= 0.95;
            alpha -= decay;
            return alpha > 0.02f;
        }

        public void draw(Graphics2D g2d) {
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            int s = Math.max(2, (int) (size * alpha));
            g2d.fillOval((int) (x - s / 2.0), (int) (y - s / 2.0), s, s);
        }
    }

    public static class Shockwave {
        private double x, y;
        private double radius;
        private double maxRadius;
        private double speed;
        private float alpha;
        private Color color;

        public Shockwave(double x, double y, double maxRadius, double speed, Color color) {
            this.x = x;
            this.y = y;
            this.radius = 4.0;
            this.maxRadius = maxRadius;
            this.speed = speed;
            this.alpha = 1.0f;
            this.color = color;
        }

        public boolean update() {
            radius += speed;
            alpha = (float) Math.max(0.0, 1.0 - (radius / maxRadius));
            return radius < maxRadius && alpha > 0.02f;
        }

        public void draw(Graphics2D g2d) {
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g2d.setStroke(new BasicStroke(2.6f));
            int r = (int) radius;
            g2d.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        }
    }

    public static class FloatingText {
        private double x, y;
        private String text;
        private Color color;
        private float alpha;
        private int life;
        private int maxLife;

        public FloatingText(double x, double y, String text, Color color, int maxLife) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.alpha = 1.0f;
            this.life = 0;
            this.maxLife = maxLife;
        }

        public boolean update() {
            y -= 0.65;
            life++;
            if (life > maxLife * 0.6) {
                alpha = (float) Math.max(0.0, 1.0 - (life - maxLife * 0.6) / (maxLife * 0.4));
            }
            return life < maxLife && alpha > 0.02f;
        }

        public void draw(Graphics2D g2d) {
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int tw = fm.stringWidth(text);

            g2d.setColor(new Color(0, 0, 0, a));
            g2d.drawString(text, (int) x - tw / 2 + 1, (int) y + 1);

            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            g2d.drawString(text, (int) x - tw / 2, (int) y);
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Shockwave> shockwaves = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Random random = new Random();

    public void clear() {
        particles.clear();
        shockwaves.clear();
        floatingTexts.clear();
    }

    public void spawnPelletSpark(double x, double y, Color color) {
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.8 + random.nextDouble() * 1.8;
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    5.0f,
                    0.06f + random.nextFloat() * 0.04f,
                    color
            ));
        }
    }

    public void spawnPowerPelletBurst(double x, double y) {
        shockwaves.add(new Shockwave(x, y, 52.0, 2.8, new Color(255, 220, 40)));
        shockwaves.add(new Shockwave(x, y, 36.0, 1.9, new Color(50, 255, 130)));
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.2 + random.nextDouble() * 3.5;
            Color c = (i % 2 == 0) ? new Color(255, 230, 50) : new Color(60, 255, 140);
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    7.0f,
                    0.03f + random.nextFloat() * 0.02f,
                    c
            ));
        }
    }

    public void spawnGoldenBallUnlockEffect(double x, double y) {
        shockwaves.add(new Shockwave(x, y, 120.0, 4.0, new Color(255, 215, 0)));
        shockwaves.add(new Shockwave(x, y, 80.0, 2.6, new Color(255, 255, 160)));
        for (int i = 0; i < 40; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 4.5;
            Color c = (i % 2 == 0) ? new Color(255, 215, 0) : new Color(255, 250, 190);
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    8.5f,
                    0.02f + random.nextFloat() * 0.015f,
                    c
            ));
        }
        floatingTexts.add(new FloatingText(x, y - 14, "BOLA DE OURO LIBERADA!", new Color(255, 230, 50), 85));
    }

    public void spawnGoldenCelebrationFirework(double x, double y) {
        shockwaves.add(new Shockwave(x, y, 75.0, 3.2, new Color(255, 215, 0)));
        Color[] fireworkColors = {
            new Color(255, 220, 30),
            new Color(50, 255, 120),
            new Color(60, 220, 255),
            new Color(255, 255, 255)
        };
        for (int i = 0; i < 28; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.4 + random.nextDouble() * 4.2;
            Color c = fireworkColors[i % fireworkColors.length];
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    8.0f,
                    0.022f + random.nextFloat() * 0.018f,
                    c
            ));
        }
    }

    public void spawnGhostEatenEffect(double x, double y, int points, Color ghostColor) {
        shockwaves.add(new Shockwave(x, y, 60.0, 3.4, new Color(255, 230, 60)));
        shockwaves.add(new Shockwave(x, y, 42.0, 2.2, ghostColor));
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 4.2;
            Color c = (i % 3 == 0) ? Color.WHITE : (i % 3 == 1 ? ghostColor : new Color(255, 225, 40));
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    7.5f,
                    0.025f + random.nextFloat() * 0.02f,
                    c
            ));
        }
        floatingTexts.add(new FloatingText(x, y - 6, "DRIBLE! +" + points, new Color(255, 240, 80), 55));
    }

    public void spawnFloatingScore(double x, double y, String text, Color color) {
        floatingTexts.add(new FloatingText(x, y, text, color, 48));
    }

    public void spawnDeathExplosion(double x, double y) {
        shockwaves.add(new Shockwave(x, y, 55.0, 2.5, new Color(255, 70, 50)));
        for (int i = 0; i < 34; i++) {
            double angle = (Math.PI * 2 * i) / 34.0;
            double speed = 1.0 + random.nextDouble() * 3.2;
            Color c = (i % 2 == 0) ? new Color(255, 215, 0) : new Color(255, 60, 40);
            particles.add(new Particle(
                    x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    7.5f,
                    0.022f + random.nextFloat() * 0.015f,
                    c
            ));
        }
        floatingTexts.add(new FloatingText(x, y - 10, "DESARME!", new Color(255, 90, 90), 55));
    }

    public void update() {
        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            if (!pit.next().update()) {
                pit.remove();
            }
        }

        Iterator<Shockwave> sit = shockwaves.iterator();
        while (sit.hasNext()) {
            if (!sit.next().update()) {
                sit.remove();
            }
        }

        Iterator<FloatingText> fit = floatingTexts.iterator();
        while (fit.hasNext()) {
            if (!fit.next().update()) {
                fit.remove();
            }
        }
    }

    public void draw(Graphics2D g2d) {
        java.awt.Stroke oldStroke = g2d.getStroke();
        java.awt.Composite oldComp = g2d.getComposite();

        for (Shockwave sw : shockwaves) {
            sw.draw(g2d);
        }
        for (Particle p : particles) {
            p.draw(g2d);
        }
        for (FloatingText ft : floatingTexts) {
            ft.draw(g2d);
        }

        g2d.setStroke(oldStroke);
        g2d.setComposite(oldComp);
    }
}
