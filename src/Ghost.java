import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ghost {

    public enum GhostType {
        RED("Zagueiro Vermelho", "Perseguidor Direto", new Color(235, 40, 50), "3", 1, 25),
        BLUE("Zagueiro Azul", "Previsor de Caminho", new Color(30, 165, 255), "4", 1, 2),
        PINK("Zagueiro Rosa", "Patrulheiro Tatico", new Color(255, 105, 195), "2", 29, 25),
        ORANGE("Zagueiro Laranja", "Alternador Misto", new Color(255, 150, 25), "5", 29, 2);

        public final String name;
        public final String role;
        public final Color color;
        public final String shirtNumber;
        public final int scatterRow;
        public final int scatterCol;

        GhostType(String name, String role, Color color, String shirtNumber, int scatterRow, int scatterCol) {
            this.name = name;
            this.role = role;
            this.color = color;
            this.shirtNumber = shirtNumber;
            this.scatterRow = scatterRow;
            this.scatterCol = scatterCol;
        }
    }

    public enum State {
        NORMAL,
        FRIGHTENED,
        EATEN
    }

    private final GhostType type;
    private final int spawnIndex;
    private double x;
    private double y;
    private Direction currentDir = Direction.LEFT;
    private State state = State.NORMAL;

    private boolean inHouse = false;
    private int releaseTimer = 0;
    private int frightenedTimer = 0;

    private int lastDecisionRow = -1;
    private int lastDecisionCol = -1;
    private int patrolWaypointIndex = 0;
    private int animTick = 0;
    private final Random random = new Random();

    private static final int[][] PATROL_WAYPOINTS = {
        {5, 6}, {5, 21}, {20, 21}, {20, 6}, {8, 13}, {26, 13}
    };

    public Ghost(GhostType type, Maze maze) {
        this(type, maze, type.ordinal());
    }

    public Ghost(GhostType type, Maze maze, int spawnIndex) {
        this.type = type;
        this.spawnIndex = spawnIndex;
        reset(maze);
    }

    public void reset(Maze maze) {
        this.state = State.NORMAL;
        this.frightenedTimer = 0;
        this.lastDecisionRow = -1;
        this.lastDecisionCol = -1;
        this.patrolWaypointIndex = spawnIndex % PATROL_WAYPOINTS.length;

        if (spawnIndex == 0) {
            // Primeiro zagueiro (Vermelho) ja inicia na entrada do campo pronto para marcar
            this.x = maze.tileCenterX(13);
            this.y = maze.tileCenterY(11);
            this.currentDir = Direction.LEFT;
            this.inHouse = false;
            this.releaseTimer = 0;
        } else {
            int houseCol = 12 + (spawnIndex % 4);
            this.x = maze.tileCenterX(houseCol);
            this.y = maze.tileCenterY(14);
            this.currentDir = Direction.UP;
            this.inHouse = true;
            this.releaseTimer = 40 * spawnIndex;
        }
    }

    public void enterFrightenedMode(int durationTicks) {
        if (state == State.EATEN) return;
        if (state != State.FRIGHTENED && !inHouse) {
            currentDir = currentDir.opposite();
            lastDecisionRow = -1;
            lastDecisionCol = -1;
        }
        state = State.FRIGHTENED;
        frightenedTimer = durationTicks;
    }

    public void onCaptured() {
        state = State.EATEN;
        frightenedTimer = 0;
        lastDecisionRow = -1;
        lastDecisionCol = -1;
    }

    public void update(Maze maze, Pacman pacman, Ghost redGhost, int level, boolean scatterWave) {
        animTick++;

        if (state == State.FRIGHTENED) {
            frightenedTimer--;
            if (frightenedTimer <= 0) {
                state = State.NORMAL;
                frightenedTimer = 0;
            }
        }

        // Zagueiro na area tecnica central aguardando entrada em campo
        if (inHouse) {
            if (releaseTimer > 0) {
                releaseTimer--;
                double baseCenterY = maze.tileCenterY(14);
                y = baseCenterY + Math.sin((animTick + spawnIndex * 12) * 0.18) * 3.0;
                return;
            }

            double exitX = maze.tileCenterX(13);
            double exitY = maze.tileCenterY(11);
            double exitSpeed = 2.1;

            if (Math.abs(x - exitX) > 1.8) {
                x += Math.signum(exitX - x) * exitSpeed;
                currentDir = (exitX > x) ? Direction.RIGHT : Direction.LEFT;
            } else {
                x = exitX;
                if (y > exitY + 1.8) {
                    y -= exitSpeed;
                    currentDir = Direction.UP;
                } else {
                    y = exitY;
                    inHouse = false;
                    lastDecisionRow = 11;
                    lastDecisionCol = 13;
                    currentDir = (spawnIndex % 2 == 0) ? Direction.LEFT : Direction.RIGHT;
                }
            }
            return;
        }

        int row = Math.max(0, Math.min(Maze.ROWS - 1, maze.pixelToRow(y)));
        int col = ((maze.pixelToCol(x) % Maze.COLS) + Maze.COLS) % Maze.COLS;

        double speed = computeSpeed(maze, row, col, level);

        double centerX = maze.tileCenterX(col);
        double centerY = maze.tileCenterY(row);

        // Se foi driblado (EATEN) e retornou ao ponto inicial na area central
        if (state == State.EATEN && row >= 13 && row <= 14 && col >= 12 && col <= 15) {
            x = maze.tileCenterX(13);
            y = maze.tileCenterY(14);
            state = State.NORMAL;
            inHouse = true;
            releaseTimer = 22;
            lastDecisionRow = -1;
            lastDecisionCol = -1;
            return;
        }

        double distToCenter = Math.hypot(x - centerX, y - centerY);
        boolean atNewTileCenter = (row != lastDecisionRow || col != lastDecisionCol) && distToCenter <= speed + 1.2;

        boolean canUseDoor = (state == State.EATEN);
        boolean blockedAhead = !maze.isWalkableForGhost(row + currentDir.dy, col + currentDir.dx, canUseDoor);

        if (atNewTileCenter || blockedAhead || currentDir == Direction.NONE) {
            x = centerX;
            y = centerY;
            lastDecisionRow = row;
            lastDecisionCol = col;
            currentDir = chooseNextDirection(maze, pacman, redGhost, row, col, scatterWave);
        }

        double nextX = x + currentDir.dx * speed;
        double nextY = y + currentDir.dy * speed;

        if (currentDir.dx != 0) {
            y += (centerY - y) * 0.5;
        } else if (currentDir.dy != 0) {
            x += (centerX - x) * 0.5;
        }

        double leftLimit = Maze.OFFSET_X + 2.0;
        double rightLimit = Maze.OFFSET_X + Maze.COLS * Maze.TILE_SIZE - 2.0;
        if (nextX < leftLimit) {
            x = rightLimit - 2.0;
            lastDecisionCol = -1;
        } else if (nextX > rightLimit) {
            x = leftLimit + 2.0;
            lastDecisionCol = -1;
        } else {
            x = nextX;
            y = nextY;
        }
    }

    private double computeSpeed(Maze maze, int row, int col, int level) {
        if (state == State.EATEN) {
            return 4.65;
        }
        if (state == State.FRIGHTENED) {
            return Math.min(2.05, 1.45 + (level - 1) * 0.05);
        }
        if (maze.isTunnel(row, col)) {
            return 1.55;
        }
        double base = 2.12 + Math.min(0.95, (level - 1) * 0.14);
        if (type == GhostType.RED && (maze.isGoldenBallUnlocked() || maze.getRemainingPellets() < 40)) {
            base += 0.18;
        }
        return base;
    }

    private Direction chooseNextDirection(Maze maze, Pacman pacman, Ghost redGhost,
                                          int row, int col, boolean scatterWave) {
        boolean canUseDoor = (state == State.EATEN);
        Direction forbidden = currentDir.opposite();

        List<Direction> validDirs = new ArrayList<>();
        Direction[] order = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        for (Direction d : order) {
            if (d == forbidden) continue;
            if (maze.isWalkableForGhost(row + d.dy, col + d.dx, canUseDoor)) {
                validDirs.add(d);
            }
        }

        if (validDirs.isEmpty()) {
            for (Direction d : order) {
                if (maze.isWalkableForGhost(row + d.dy, col + d.dx, canUseDoor)) {
                    validDirs.add(d);
                }
            }
        }
        if (validDirs.isEmpty()) {
            return Direction.UP;
        }
        if (validDirs.size() == 1) {
            return validDirs.get(0);
        }

        int pacRow = pacman.getRow(maze);
        int pacCol = pacman.getCol(maze);

        // 1. Quando capturado (driblado), retorna direto ao ponto inicial usando BFS
        if (state == State.EATEN) {
            return maze.findNextDirectionBFS(row, col, 14, 13, forbidden, true);
        }

        // 2. Quando vulneravel (durante Drible Especial do jogador), foge do jogador
        if (state == State.FRIGHTENED) {
            Direction bestFlee = validDirs.get(0);
            double maxScore = -Double.MAX_VALUE;
            for (Direction d : validDirs) {
                int nr = row + d.dy;
                int nc = ((col + d.dx) % Maze.COLS + Maze.COLS) % Maze.COLS;
                double dist = Math.hypot(nr - pacRow, nc - pacCol) + random.nextDouble() * 2.2;
                if (dist > maxScore) {
                    maxScore = dist;
                    bestFlee = d;
                }
            }
            return bestFlee;
        }

        // 3. Inteligencia Artificial especifica de cada Zagueiro
        int targetRow = pacRow;
        int targetCol = pacCol;
        Direction pacDir = pacman.getFacingDirection();

        switch (type) {
            case RED:
                // Zagueiro Vermelho: Persegue diretamente a posicao atual do jogador
                targetRow = pacRow;
                targetCol = pacCol;
                break;

            case BLUE:
                // Zagueiro Azul: Tenta prever o caminho do jogador (4 casas a frente da direcao atual)
                targetRow = pacRow + pacDir.dy * 4;
                targetCol = pacCol + pacDir.dx * 4;
                break;

            case PINK:
                // Zagueiro Rosa: Patrulha o labirinto percorrendo pontos estrategicos dos setores
                int[] wp = PATROL_WAYPOINTS[patrolWaypointIndex];
                if (Math.hypot(row - wp[0], col - wp[1]) <= 3.0) {
                    patrolWaypointIndex = (patrolWaypointIndex + 1) % PATROL_WAYPOINTS.length;
                    wp = PATROL_WAYPOINTS[patrolWaypointIndex];
                }
                // Se o jogador entrar muito perto do setor do patrulheiro, fecha o espaco
                if (Math.hypot(row - pacRow, col - pacCol) < 5.0) {
                    targetRow = pacRow;
                    targetCol = pacCol;
                } else {
                    targetRow = wp[0];
                    targetCol = wp[1];
                }
                break;

            case ORANGE:
            default:
                // Zagueiro Laranja: Alterna entre perseguir o jogador e patrulhar o labirinto
                boolean chasePhase = !scatterWave && ((animTick / 210) % 2 == 0);
                if (chasePhase && Math.hypot(row - pacRow, col - pacCol) > 5.5) {
                    targetRow = pacRow;
                    targetCol = pacCol;
                } else {
                    int[] owp = PATROL_WAYPOINTS[(patrolWaypointIndex + 2) % PATROL_WAYPOINTS.length];
                    if (Math.hypot(row - owp[0], col - owp[1]) <= 3.0) {
                        patrolWaypointIndex = (patrolWaypointIndex + 1) % PATROL_WAYPOINTS.length;
                    }
                    targetRow = owp[0];
                    targetCol = owp[1];
                }
                break;
        }

        targetRow = Math.max(1, Math.min(Maze.ROWS - 2, targetRow));
        targetCol = ((targetCol % Maze.COLS) + Maze.COLS) % Maze.COLS;

        return maze.findNextDirectionBFS(row, col, targetRow, targetCol, forbidden, false);
    }

    public void draw(Graphics2D g2d) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);

        if (state == State.EATEN) {
            // Zagueiro driblado voltando rapidamente ao ponto inicial
            g2d.setColor(new Color(120, 235, 255, 95));
            g2d.fillOval(cx - 8, cy - 8, 16, 16);
            drawDefenderEyes(g2d, cx, cy, currentDir);
            return;
        }

        boolean flashingWhite = false;
        Color shirtColor;
        if (state == State.FRIGHTENED) {
            flashingWhite = (frightenedTimer < 120) && ((frightenedTimer / 10) % 2 == 0);
            shirtColor = flashingWhite ? new Color(225, 240, 255) : new Color(30, 80, 235);
        } else {
            shirtColor = type.color;
        }

        // Sombra no gramado
        g2d.setColor(new Color(0, 0, 0, 85));
        g2d.fillOval(cx - 9, cy - 7, 18, 16);

        // Chuteiras animadas do zagueiro
        int stride = (int) (Math.sin(animTick * 0.35 + spawnIndex) * 3.5);
        g2d.setColor(new Color(20, 22, 28));
        g2d.fillRoundRect(cx - 7, cy - 9 + stride, 5, 6, 2, 2);
        g2d.fillRoundRect(cx + 2, cy + 3 - stride, 5, 6, 2, 2);

        // Ombros / Uniforme do Zagueiro visto de cima
        g2d.setColor(shirtColor);
        g2d.fillRoundRect(cx - 10, cy - 8, 20, 16, 10, 10);

        // Listras esportivas brancas nos ombros da camisa
        g2d.setColor(flashingWhite ? new Color(30, 80, 235) : new Color(255, 255, 255, 195));
        g2d.setStroke(new BasicStroke(1.6f));
        g2d.drawLine(cx - 8, cy - 5, cx - 4, cy - 5);
        g2d.drawLine(cx + 4, cy - 5, cx + 8, cy - 5);

        // Cabeca do zagueiro
        g2d.setColor(state == State.FRIGHTENED ? new Color(185, 215, 255) : new Color(225, 170, 125));
        g2d.fillOval(cx - 6, cy - 6, 12, 12);

        if (state == State.FRIGHTENED) {
            // Estrelinhas girando indicando zagueiro vulneravel ao drible
            g2d.setColor(new Color(255, 235, 50));
            for (int i = 0; i < 3; i++) {
                double ang = animTick * 0.18 + i * (Math.PI * 2 / 3);
                int sx = cx + (int) (Math.cos(ang) * 9);
                int sy = cy + (int) (Math.sin(ang) * 9);
                g2d.fillOval(sx - 2, sy - 2, 4, 4);
            }
            // Expressao tonta
            g2d.setColor(new Color(20, 40, 140));
            g2d.fillOval(cx - 4, cy - 2, 3, 3);
            g2d.fillOval(cx + 1, cy - 2, 3, 3);
        } else {
            // Cabelo / Topo da cabeca e olhos focados na direcao
            g2d.setColor(new Color(30, 24, 20));
            g2d.fillArc(cx - 6, cy - 6, 12, 12, 0, 180);
            drawDefenderEyes(g2d, cx, cy, currentDir);

            // Numero da camisa discreto
            g2d.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2d.setColor(Color.WHITE);
            g2d.drawString(type.shirtNumber, cx - 2, cy + 8);
        }
    }

    private void drawDefenderEyes(Graphics2D g2d, int cx, int cy, Direction dir) {
        int ox = dir.dx * 2;
        int oy = dir.dy * 2;

        g2d.setColor(Color.WHITE);
        g2d.fillOval(cx - 5 + ox, cy - 3 + oy, 4, 5);
        g2d.fillOval(cx + 1 + ox, cy - 3 + oy, 4, 5);

        g2d.setColor(new Color(15, 25, 45));
        g2d.fillOval(cx - 4 + ox * 2, cy - 2 + oy * 2, 2, 3);
        g2d.fillOval(cx + 2 + ox * 2, cy - 2 + oy * 2, 2, 3);
    }

    public GhostType getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public int getFrightenedTimer() {
        return frightenedTimer;
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
