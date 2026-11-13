import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    private final Game game;
    private final Timer gameLoopTimer;

    public GamePanel(Game game) {
        this.game = game;
        setPreferredSize(new Dimension(Game.WIDTH, Game.HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Loop principal a ~60 FPS (16ms por quadro)
        this.gameLoopTimer = new Timer(16, this);
    }

    public void startGameLoop() {
        requestFocusInWindow();
        gameLoopTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        game.update();
        repaint();
        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        game.draw(g2d);

        g2d.dispose();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        game.handleKeyPressed(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Movimentacao continua estilo arcade; direcao permanece ate proxima curva ou parede
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Nao utilizado
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Tratado em mousePressed para resposta instantanea
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        game.handleMouseClicked(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Nao utilizado
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nao utilizado
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nao utilizado
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        game.handleMouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        game.handleMouseMoved(e.getX(), e.getY());
    }
}
