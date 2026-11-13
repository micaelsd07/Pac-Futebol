import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class Pacman {

    private static final int SPAWN_ROW = 23;
    private static final int SPAWN_COL = 13;

    private double x;
    private double y;
    private double baseSpeed = 2.68;
    private Direction currentDir = Direction.NONE;
    private Direction requestedDir = Direction.NONE;
    private Direction facingDir = Direction.LEFT;

    // Animacoes de corrida e drible
    private double stridePhase = 0.0;
    private double dribblePhase = 0.0;
    private boolean isMoving = false;

    // Animacao de desarme (perda de vida)
    private boolean dying = false;
    private double deathProgress = 0.0;

    // Rastro de velocidade e drible
    private final double[] trailX = new double[6];
    private final double[] trailY = new double[6];
    private int trailTick = 0;

    public Pacman(Maze maze) {
        reset(maze);
    }

    public void reset(Maze maze) {
        this.x = maze.tileCenterX(SPAWN_COL) + Maze.TILE_SIZE / 2.0;
        this.y = maze.tileCenterY(SPAWN_ROW);
        this.currentDir = Direction.LEFT;
        this.requestedDir = Direction.LEFT;
        this.facingDir = Direction.LEFT;
        this.stridePhase = 0.0;
        this.dribblePhase = 0.0;
        this.isMoving = false;
        this.dying = false;
        this.deathProgress = 0.0;

        for (int i = 0; i < trailX.length; i++) {
            trailX[i] = this.x;
            trailY[i] = this.y;
        }
    }

    public void setRequestedDirection(Direction dir) {
        if (dir != null && dir != Direction.NONE) {
            this.requestedDir = dir;
        }
    }

    public Direction getCurrentDirection() {
        return currentDir;
    }

    public Direction getFacingDirection() {
        return facingDir;
    }

    public void startDeathAnimation() {
        this.dying = true;
        this.deathProgress = 0.0;
    }

    public boolean updateDeathAnimation() {
        if (!dying) return true;
        deathProgress += 0.025;
        if (deathProgress >= 1.0) {
            deathProgress = 1.0;
            return true;
        }
        return false;
    }

    public boolean isDying() {
        return dying;
    }

    public void update(Maze maze, int level) {
        if (dying) return;

        double speed = Math.min(3.35, baseSpeed + (level - 1) * 0.07);
        int row = maze.pixelToRow(y);
        int col = maze.pixelToCol(x);

        row = Math.max(0, Math.min(Maze.ROWS - 1, row));
        col = ((col % Maze.COLS) + Maze.COLS) % Maze.COLS;

        double centerX = maze.tileCenterX(col);
        double centerY = maze.tileCenterY(row);

        // 1. Verifica mudanca para a direcao solicitada
        if (requestedDir != Direction.NONE) {
            if (requestedDir == currentDir.opposite()) {
                currentDir = requestedDir;
                facingDir = currentDir;
            } else if (requestedDir != currentDir) {
                int targetRow = row + requestedDir.dy;
                int targetCol = col + requestedDir.dx;
                if (maze.isWalkableForPacman(targetRow, targetCol)) {
                    double distToCenter = Math.hypot(x - centerX, y - centerY);
                    if (distToCenter <= speed + 4.2) {
                        x = centerX;
                        y = centerY;
                        currentDir = requestedDir;
                        facingDir = currentDir;
                    }
                }
            }
        }

        // 2. Inicia movimento se estava parado e corredor abriu
        if (currentDir == Direction.NONE && requestedDir != Direction.NONE) {
            if (maze.isWalkableForPacman(row + requestedDir.dy, col + requestedDir.dx)) {
                currentDir = requestedDir;
                facingDir = currentDir;
            }
        }

        // 3. Movimenta no corredor respeitando colisoes com paredes
        boolean moved = false;
        if (currentDir != Direction.NONE) {
            double nextX = x + currentDir.dx * speed;
            double nextY = y + currentDir.dy * speed;

            if (currentDir.dx != 0) {
                y += (centerY - y) * 0.45;
            } else if (currentDir.dy != 0) {
                x += (centerX - x) * 0.45;
            }

            double leftLimit = Maze.OFFSET_X + 2.0;
            double rightLimit = Maze.OFFSET_X + Maze.COLS * Maze.TILE_SIZE - 2.0;
            if (nextX < leftLimit) {
                x = rightLimit - 2.0;
                moved = true;
            } else if (nextX > rightLimit) {
                x = leftLimit + 2.0;
                moved = true;
            } else {
                int aheadRow = row + currentDir.dy;
                int aheadCol = col + currentDir.dx;
                boolean blockedAhead = !maze.isWalkableForPacman(aheadRow, aheadCol);

                if (blockedAhead) {
                    if (currentDir == Direction.RIGHT && nextX > centerX) {
                        nextX = centerX;
                        currentDir = Direction.NONE;
                    } else if (currentDir == Direction.LEFT && nextX < centerX) {
                        nextX = centerX;
                        currentDir = Direction.NONE;
                    } else if (currentDir == Direction.DOWN && nextY > centerY) {
                        nextY = centerY;
                        currentDir = Direction.NONE;
                    } else if (currentDir == Direction.UP && nextY < centerY) {
                        nextY = centerY;
                        currentDir = Direction.NONE;
                    }
                }

                if (Math.hypot(nextX - x, nextY - y) > 0.05) {
                    moved = true;
                }
                x = nextX;
                y = nextY;
            }
        }

        this.isMoving = moved;
        if (moved) {
            facingDir = (currentDir != Direction.NONE) ? currentDir : facingDir;
            stridePhase += 0.34;
            dribblePhase += 0.28;
        } else {
            // Mesmo parado, mantem leve controle da bola nos pes
            dribblePhase += 0.08;
        }

        trailTick++;
        if (trailTick % 2 == 0) {
            for (int i = trailX.length - 1; i > 0; i--) {
                trailX[i] = trailX[i - 1];
                trailY[i] = trailY[i - 1];
            }
            trailX[0] = x;
            trailY[0] = y;
        }
    }

    public void draw(Graphics2D g2d, boolean specialDribbleActive) {
        if (dying) {
            drawTackleAnimation(g2d);
            return;
        }

        // 1. Rastro de corrida / drible especial
        for (int i = trailX.length - 1; i >= 1; i--) {
            int alpha = (specialDribbleActive ? 70 : 30) * (trailX.length - i) / trailX.length;
            Color tc = specialDribbleActive
                    ? new Color(50, 255, 160, alpha)
                    : new Color(255, 230, 20, alpha);
            g2d.setColor(tc);
            int tr = Math.max(4, 10 - i);
            g2d.fillOval((int) trailX[i] - tr, (int) trailY[i] - tr, tr * 2, tr * 2);
        }

        // 2. Aura do Modo de Drible Especial
        if (specialDribbleActive) {
            int auraR = 14 + (int) (2 * Math.sin(dribblePhase * 2));
            g2d.setColor(new Color(40, 255, 150, 80));
            g2d.fillOval((int) x - auraR, (int) y - auraR, auraR * 2, auraR * 2);
            g2d.setColor(new Color(255, 235, 60, 160));
            g2d.setStroke(new BasicStroke(1.6f));
            g2d.drawOval((int) x - auraR, (int) y - auraR, auraR * 2, auraR * 2);
        }

        // 3. Desenha o jogador visto de cima rotacionado na direcao do movimento
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(x, y);

        double rotationRadians = 0.0;
        switch (facingDir) {
            case RIGHT:
                rotationRadians = 0.0;
                break;
            case DOWN:
                rotationRadians = Math.PI / 2.0;
                break;
            case LEFT:
                rotationRadians = Math.PI;
                break;
            case UP:
                rotationRadians = -Math.PI / 2.0;
                break;
            default:
                break;
        }
        g2d.rotate(rotationRadians);

        double speedMult = specialDribbleActive ? 1.65 : 1.0;
        double legSwing = isMoving ? Math.sin(stridePhase * speedMult) * 4.5 : 0.0;
        double ballDribbleY = Math.sin(dribblePhase * speedMult) * (specialDribbleActive ? 4.8 : 2.8);
        double ballDribbleX = 9.5 + Math.abs(Math.cos(dribblePhase * speedMult)) * 2.2;

        // Chuteiras (Esquerda e Direita alternando na corrida)
        g2d.setColor(new Color(20, 25, 35));
        g2d.fillRoundRect((int) (2 + legSwing), -7, 7, 4, 3, 3);
        g2d.fillRoundRect((int) (2 - legSwing), 3, 7, 4, 3, 3);
        // Detalhe neon nas chuteiras
        g2d.setColor(new Color(50, 255, 120));
        g2d.fillRect((int) (4 + legSwing), -6, 3, 2);
        g2d.fillRect((int) (4 - legSwing), 4, 3, 2);

        // Bola de futebol nos pes do jogador (animada no drible)
        int bx = (int) Math.round(ballDribbleX);
        int by = (int) Math.round(ballDribbleY);
        g2d.setColor(specialDribbleActive ? new Color(255, 240, 90) : Color.WHITE);
        g2d.fillOval(bx - 4, by - 4, 8, 8);
        g2d.setColor(new Color(20, 25, 30));
        g2d.fillRect(bx - 1, by - 1, 2, 2);
        g2d.fillRect(bx + 1, by - 3, 2, 1);
        g2d.fillRect(bx - 3, by + 1, 2, 1);

        // Bracos / Mangas se movendo em oposicao as pernas
        g2d.setColor(new Color(235, 180, 130));
        g2d.fillOval((int) (-1 - legSwing * 0.5), -10, 5, 4);
        g2d.fillOval((int) (-1 + legSwing * 0.5), 6, 5, 4);

        // Calcao azul (borda frontal vista de cima)
        g2d.setColor(new Color(20, 95, 225));
        g2d.fillRoundRect(0, -7, 5, 14, 4, 4);

        // Camisa de futebol amarela com detalhes verdes (Uniforme Camisa 10)
        g2d.setColor(new Color(255, 225, 15));
        g2d.fillRoundRect(-7, -8, 12, 16, 8, 8);

        // Gola e listras verdes nas mangas
        g2d.setColor(new Color(0, 165, 65));
        g2d.setStroke(new BasicStroke(1.6f));
        g2d.drawLine(-5, -7, 2, -7);
        g2d.drawLine(-5, 7, 2, 7);

        // Cabeca do jogador vista de cima
        g2d.setColor(new Color(235, 180, 130));
        g2d.fillOval(-4, -5, 9, 10);

        // Cabelo e faixa esportiva
        g2d.setColor(new Color(35, 25, 18));
        g2d.fillArc(-5, -5, 9, 10, 70, 220);
        g2d.setColor(specialDribbleActive ? new Color(0, 240, 255) : new Color(0, 175, 70));
        g2d.fillRect(-1, -4, 2, 8);

        g2d.setTransform(oldTransform);
    }

    /**
     * Animacao especial quando o jogador conquista a Bola de Ouro!
     */
    public void drawGoldenCelebration(Graphics2D g2d, int celebrationTick) {
        double pulse = 0.7 + 0.3 * Math.sin(celebrationTick * 0.25);

        // Holofote dourado gigante sobre o jogador
        int spotR = (int) (26 + 8 * pulse);
        g2d.setColor(new Color(255, 215, 0, 95));
        g2d.fillOval((int) x - spotR, (int) y - spotR, spotR * 2, spotR * 2);

        // Desenha o jogador comemorando e erguendo a Bola de Ouro
        draw(g2d, true);

        int liftY = (int) (y - 16 - Math.abs(Math.sin(celebrationTick * 0.2)) * 6);
        g2d.setColor(new Color(255, 215, 0));
        g2d.fillOval((int) x - 8, liftY - 8, 16, 16);
        g2d.setColor(Color.WHITE);
        g2d.drawOval((int) x - 8, liftY - 8, 16, 16);
        g2d.fillOval((int) x - 4, liftY - 5, 4, 4);
    }

    private void drawTackleAnimation(Graphics2D g2d) {
        // Bola escapando e estrelas girando ao sofrer desarme
        double radius = 10 + deathProgress * 12;
        g2d.setColor(new Color(255, 220, 20));
        int playerSize = (int) Math.max(4, 16 * (1.0 - deathProgress * 0.5));
        g2d.fillOval((int) x - playerSize / 2, (int) y - playerSize / 2, playerSize, playerSize);

        for (int i = 0; i < 4; i++) {
            double ang = deathProgress * 8.0 + i * (Math.PI / 2.0);
            int sx = (int) (x + Math.cos(ang) * radius);
            int sy = (int) (y + Math.sin(ang) * radius);
            g2d.setColor(i % 2 == 0 ? Color.WHITE : new Color(255, 80, 60));
            g2d.fillOval(sx - 3, sy - 3, 6, 6);
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRow(Maze maze) {
        return Math.max(0, Math.min(Maze.ROWS - 1, maze.pixelToRow(y)));
    }

    public int getCol(Maze maze) {
        int col = maze.pixelToCol(x);
        return ((col % Maze.COLS) + Maze.COLS) % Maze.COLS;
    }
}
