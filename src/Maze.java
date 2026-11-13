import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayDeque;
import java.util.Queue;

public class Maze {

    public static final int COLS = 28;
    public static final int ROWS = 31;
    public static final int TILE_SIZE = 21;

    public static final int OFFSET_X = 22;
    public static final int OFFSET_Y = 24;

    public static final int EMPTY = 0;
    public static final int WALL = 1;
    public static final int MINI_BALL = 2;
    public static final int SPECIAL_BALL = 3;
    public static final int GHOST_DOOR = 4;
    public static final int GOLD_STAR = 5;
    public static final int COIN = 6;

    // Compatibilidade com nomes anteriores
    public static final int PELLET = MINI_BALL;
    public static final int POWER_PELLET = SPECIAL_BALL;

    // 5 Labirintos distintos com complexidade crescente e menos rotas de fuga nos niveis superiores
    private static final String[][] STADIUM_MAPS = {
        // FASE 1: Arena Classica Verde-Ouro (Amplas rotas de escape)
        {
            "############################",
            "#*...........##...........*#",
            "#.####.#####.##.#####.####.#",
            "#o####.#####.##.#####.####o#",
            "#.####.#####.##.#####.####.#",
            "#.....$......*.......$.....#",
            "#.####.##.########.##.####.#",
            "#.####.##.########.##.####.#",
            "#....$.##....##....##.$....#",
            "######.##### ## #####.######",
            "######.##### ## #####.######",
            "######.##   *  *   ##.######",
            "######.## ###==### ##.######",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "######.## ######## ##.######",
            "######.##    *     ##.######",
            "######.## ######## ##.######",
            "######.## ######## ##.######",
            "#.....$......##......$.....#",
            "#.####.#####.##.#####.####.#",
            "#.####.#####.##.#####.####.#",
            "#o..##...$...  ...$...##..o#",
            "###.##.##.########.##.##.###",
            "###.##.##.########.##.##.###",
            "#*.....##....##....##.....*#",
            "#.##########.##.##########.#",
            "#.##########.##.##########.#",
            "#............$$............#",
            "############################"
        },
        // FASE 2: Estadio Olimpico Azul-Prata (Corredores laterais mais longos)
        {
            "############################",
            "#o....$......##......$....o#",
            "#.##########.##.##########.#",
            "#.#........*.##.*........#.#",
            "#.#.######.#.##.#.######.#.#",
            "#*..######........######..*#",
            "###.######.######.######.###",
            "#...$......######......$...#",
            "#.#######.##....##.#######.#",
            "#.#######.##### ##.#######.#",
            "#......##.##### ##.##......#",
            "######.##   $  $   ##.######",
            "######.## ###==### ##.######",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "######.## ######## ##.######",
            "######.##    *     ##.######",
            "######.##.########.##.######",
            "#......##.########.##......#",
            "#.####.....$....$.....####.#",
            "#.####.#####.##.#####.####.#",
            "#*.....#####.##.#####.....*#",
            "#o.###.......  .......###.o#",
            "#..###.##.########.##.###..#",
            "##.....##.########.##.....##",
            "##.######....##....######.##",
            "#..######.##.##.##.######..#",
            "#.#.......##.##.##.......#.#",
            "#*..$........##........$..*#",
            "############################"
        },
        // FASE 3: Caldeirao Rubro-Negro (Chicanes no meio-campo)
        {
            "############################",
            "#*....######.##.######....*#",
            "#.###.######.##.######.###.#",
            "#o###........$.........###o#",
            "#.###.####.######.####.###.#",
            "#.....####.######.####.....#",
            "#####.##.....##.....##.#####",
            "#.....##.###.##.###.##.....#",
            "#.###$...###....###...$###.#",
            "#.######.##### ##.######...#",
            "#......#.##### ##.#......###",
            "######.#    *  *    #.######",
            "######.## ###==### ##.######",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "######.## ######## ##.######",
            "######.##    $     ##.######",
            "######.#####.##.#####.######",
            "#*.....#####.##.#####.....*#",
            "#.####.......##.......####.#",
            "#.####.#####.##.#####.####.#",
            "#.$....##..........##....$.#",
            "#o.######....  ....######.o#",
            "#..######.########.######..#",
            "##........########........##",
            "##.#####.$...##...$.#####.##",
            "#..#####.###.##.###.#####..#",
            "#.######.###.##.###.######.#",
            "#*...........$$...........*#",
            "############################"
        },
        // FASE 4: Arena Champions Ouro-Purpura (Anel interno complexo, menos atalhos)
        {
            "############################",
            "#o..$.######.##.######.$..o#",
            "#.###.######.##.######.###.#",
            "#.###...*....##....*...###.#",
            "#.#######.##.##.##.#######.#",
            "#.........##....##.........#",
            "#.####.#####.##.#####.####.#",
            "#.####.#####.##.#####.####.#",
            "#.$..#.......##.......#..$.#",
            "####.#.####### #######.#.###",
            "####.#.####### #######.#.###",
            "######.##   $  $   ##.######",
            "######.## ###==### ##.######",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "######.## ######## ##.######",
            "######.##    *     ##.######",
            "######.##.########.##.######",
            "####...##.########.##...####",
            "#*...####....##....####...*#",
            "#.#######.##.##.##.#######.#",
            "#.......#.##.##.##.#.......#",
            "#o.####.#....  ....#.####.o#",
            "#..####.############.####..#",
            "#.$.....############.....$.#",
            "#.#####......##......#####.#",
            "#.#####.####.##.####.#####.#",
            "#.......####.##.####.......#",
            "#*..........$..$..........*#",
            "############################"
        },
        // FASE 5: Templo da Final (Corredores estreitos e alta pressao)
        {
            "############################",
            "#*..########.##.########..*#",
            "#.#........#.##.#........#.#",
            "#o#.######.#.##.#.######.#o#",
            "#.#.######...$$...######.#.#",
            "#...########.##.########...#",
            "###.......##.##.##.......###",
            "#.#######.##....##.#######.#",
            "#.#...$...########...$...#.#",
            "#.#.#####.##### ##.#####.#.#",
            "#...#####.##### ##.#####...#",
            "######.##   *  *   ##.######",
            "######.## ###==### ##.######",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "######.## ######## ##.######",
            "######.##    $     ##.######",
            "######.#####.##.#####.######",
            "#......#####.##.#####......#",
            "#.####...$...##...$...####.#",
            "#.#########.####.#########.#",
            "#.........#.####.#.........#",
            "#o.######....  ....######.o#",
            "#..######.########.######..#",
            "##.*......########......*.##",
            "####.####....##....####.####",
            "#....####.##.##.##.####....#",
            "#.#######.##.##.##.#######.#",
            "#*..........$..$..........*#",
            "############################"
        }
    };

    private static final String[] STADIUM_NAMES = {
        "Arena Classica Verde-Ouro",
        "Estadio Olimpico Azul",
        "Caldeirao Rubro-Negro",
        "Arena Real Champions",
        "Templo da Grande Final"
    };

    private final int[][] grid = new int[ROWS][COLS];
    private int currentLayoutIndex = 0;

    // Contadores de bolas para desbloquear a Bola de Ouro
    private int remainingMiniBalls = 0;
    private int totalInitialMiniBalls = 0;
    private int animTick = 0;

    // Grande objetivo da fase: A BOLA DE OURO
    private boolean goldenBallUnlocked = false;
    private boolean goldenBallCaptured = false;
    private final int goldenBallRow = 17;
    private final int goldenBallCol = 13;

    public Maze() {
        resetMaze(1);
    }

    public void resetMaze() {
        resetMaze(currentLayoutIndex + 1);
    }

    public void resetMaze(int level) {
        this.currentLayoutIndex = Math.max(0, (level - 1) % STADIUM_MAPS.length);
        String[] selectedMap = STADIUM_MAPS[currentLayoutIndex];

        remainingMiniBalls = 0;
        goldenBallUnlocked = false;
        goldenBallCaptured = false;

        for (int r = 0; r < ROWS; r++) {
            String rowStr = selectedMap[r];
            for (int c = 0; c < COLS; c++) {
                char ch = rowStr.charAt(c);
                switch (ch) {
                    case '#':
                        grid[r][c] = WALL;
                        break;
                    case '.':
                        grid[r][c] = MINI_BALL;
                        remainingMiniBalls++;
                        break;
                    case 'o':
                        grid[r][c] = SPECIAL_BALL;
                        remainingMiniBalls++;
                        break;
                    case '*':
                        grid[r][c] = GOLD_STAR;
                        remainingMiniBalls++;
                        break;
                    case '$':
                        grid[r][c] = COIN;
                        remainingMiniBalls++;
                        break;
                    case '=':
                        grid[r][c] = GHOST_DOOR;
                        break;
                    default:
                        grid[r][c] = EMPTY;
                        break;
                }
            }
        }
        totalInitialMiniBalls = remainingMiniBalls;
    }

    public String getStadiumName(int level) {
        int idx = Math.max(0, (level - 1) % STADIUM_NAMES.length);
        return STADIUM_NAMES[idx];
    }

    public void update() {
        animTick++;
        if (!goldenBallUnlocked && remainingMiniBalls <= 0) {
            goldenBallUnlocked = true;
        }
    }

    public int getTile(int row, int col) {
        if (row < 0 || row >= ROWS) {
            return WALL;
        }
        int wrappedCol = ((col % COLS) + COLS) % COLS;
        return grid[row][wrappedCol];
    }

    public void setTile(int row, int col, int value) {
        if (row < 0 || row >= ROWS) return;
        int wrappedCol = ((col % COLS) + COLS) % COLS;
        grid[row][wrappedCol] = value;
    }

    public boolean isWalkableForPacman(int row, int col) {
        int tile = getTile(row, col);
        return tile != WALL && tile != GHOST_DOOR;
    }

    public boolean isWalkableForGhost(int row, int col, boolean canUseDoor) {
        int tile = getTile(row, col);
        if (tile == WALL) return false;
        if (tile == GHOST_DOOR) return canUseDoor;
        return true;
    }

    public boolean isTunnel(int row, int col) {
        return row == 14 && (col <= 5 || col >= COLS - 6);
    }

    public boolean isInsideGhostHouse(int row, int col) {
        return row >= 12 && row <= 15 && col >= 11 && col <= 16;
    }

    public int collectAt(int row, int col) {
        int tile = getTile(row, col);
        if (tile == MINI_BALL || tile == SPECIAL_BALL || tile == GOLD_STAR || tile == COIN) {
            setTile(row, col, EMPTY);
            remainingMiniBalls = Math.max(0, remainingMiniBalls - 1);
            if (remainingMiniBalls == 0) {
                goldenBallUnlocked = true;
            }
            return tile;
        }
        return EMPTY;
    }

    /**
     * Verifica se o jogador alcancou a Bola de Ouro apos desbloquea-la.
     */
    public boolean tryCaptureGoldenBall(double pacX, double pacY) {
        if (!goldenBallUnlocked || goldenBallCaptured) return false;
        double gx = getGoldenBallPixelX();
        double gy = getGoldenBallPixelY();
        double dist = Math.hypot(pacX - gx, pacY - gy);
        if (dist < TILE_SIZE * 0.95) {
            goldenBallCaptured = true;
            return true;
        }
        return false;
    }

    public void unlockGoldenBallNow() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == MINI_BALL || grid[r][c] == SPECIAL_BALL
                        || grid[r][c] == GOLD_STAR || grid[r][c] == COIN) {
                    grid[r][c] = EMPTY;
                }
            }
        }
        remainingMiniBalls = 0;
        goldenBallUnlocked = true;
    }

    public double getGoldenBallPixelX() {
        return tileToPixelX(goldenBallCol) + TILE_SIZE;
    }

    public double getGoldenBallPixelY() {
        return tileToPixelY(goldenBallRow) + TILE_SIZE / 2.0;
    }

    public boolean isGoldenBallUnlocked() {
        return goldenBallUnlocked;
    }

    public boolean isGoldenBallCaptured() {
        return goldenBallCaptured;
    }

    public int getRemainingPellets() {
        return remainingMiniBalls;
    }

    public int getTotalInitialPellets() {
        return totalInitialMiniBalls;
    }

    public int tileToPixelX(int col) {
        return OFFSET_X + col * TILE_SIZE;
    }

    public int tileToPixelY(int row) {
        return OFFSET_Y + row * TILE_SIZE;
    }

    public double tileCenterX(int col) {
        return OFFSET_X + col * TILE_SIZE + TILE_SIZE / 2.0;
    }

    public double tileCenterY(int row) {
        return OFFSET_Y + row * TILE_SIZE + TILE_SIZE / 2.0;
    }

    public int pixelToCol(double x) {
        return (int) Math.floor((x - OFFSET_X) / TILE_SIZE);
    }

    public int pixelToRow(double y) {
        return (int) Math.floor((y - OFFSET_Y) / TILE_SIZE);
    }

    public int getWidthPixels() {
        return COLS * TILE_SIZE;
    }

    public int getHeightPixels() {
        return ROWS * TILE_SIZE;
    }

    public Direction findNextDirectionBFS(int startRow, int startCol, int targetRow, int targetCol,
                                          Direction forbiddenDir, boolean canUseDoor) {
        startCol = ((startCol % COLS) + COLS) % COLS;
        targetCol = ((targetCol % COLS) + COLS) % COLS;
        targetRow = Math.max(1, Math.min(ROWS - 2, targetRow));

        Direction[] dirs = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};
        boolean[][] visited = new boolean[ROWS][COLS];
        Direction[][] firstStep = new Direction[ROWS][COLS];
        Queue<Point> queue = new ArrayDeque<>();

        visited[startRow][startCol] = true;

        for (Direction d : dirs) {
            if (d == forbiddenDir) continue;
            int nr = startRow + d.dy;
            int nc = ((startCol + d.dx) % COLS + COLS) % COLS;
            if (isWalkableForGhost(nr, nc, canUseDoor) && !visited[nr][nc]) {
                visited[nr][nc] = true;
                firstStep[nr][nc] = d;
                if (nr == targetRow && nc == targetCol) {
                    return d;
                }
                queue.add(new Point(nc, nr));
            }
        }

        Point bestFallback = null;
        double bestDist = Double.MAX_VALUE;

        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            int r = curr.y;
            int c = curr.x;

            double dist = Math.hypot(r - targetRow, c - targetCol);
            if (dist < bestDist) {
                bestDist = dist;
                bestFallback = curr;
            }

            if (r == targetRow && c == targetCol) {
                return firstStep[r][c];
            }

            for (Direction d : dirs) {
                int nr = r + d.dy;
                int nc = ((c + d.dx) % COLS + COLS) % COLS;
                if (nr >= 0 && nr < ROWS && isWalkableForGhost(nr, nc, canUseDoor) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    firstStep[nr][nc] = firstStep[r][c];
                    queue.add(new Point(nc, nr));
                }
            }
        }

        if (bestFallback != null && firstStep[bestFallback.y][bestFallback.x] != null) {
            return firstStep[bestFallback.y][bestFallback.x];
        }

        for (Direction d : dirs) {
            int nr = startRow + d.dy;
            int nc = ((startCol + d.dx) % COLS + COLS) % COLS;
            if (isWalkableForGhost(nr, nc, canUseDoor)) {
                return d;
            }
        }
        return Direction.UP;
    }

    public void draw(Graphics2D g2d, int level, boolean victoryFlash) {
        int mw = getWidthPixels();
        int mh = getHeightPixels();

        // 1. Gramado verde escuro com faixas de corte de estadio
        for (int r = 0; r < ROWS; r++) {
            Color grassStripe = (r % 2 == 0) ? new Color(14, 62, 26) : new Color(18, 74, 32);
            g2d.setColor(grassStripe);
            g2d.fillRect(OFFSET_X, tileToPixelY(r), mw, TILE_SIZE);
        }

        // Linhas de cal sutis do campo de futebol no fundo dos corredores
        g2d.setColor(new Color(220, 255, 220, 38));
        g2d.setStroke(new BasicStroke(1.5f));
        int midY = OFFSET_Y + mh / 2;
        int midX = OFFSET_X + mw / 2;
        g2d.drawLine(OFFSET_X + 12, midY, OFFSET_X + mw - 12, midY);
        g2d.drawOval(midX - 68, midY - 68, 136, 136);
        g2d.drawRect(midX - 110, OFFSET_Y + 12, 220, 72);
        g2d.drawRect(midX - 110, OFFSET_Y + mh - 84, 220, 72);

        // Cores esportivas de cada estadio por fase
        Color[] borderPrimary = {
            new Color(40, 215, 95),   // Fase 1: Verde Esmeralda & Ouro
            new Color(40, 175, 255),  // Fase 2: Azul Olimpico
            new Color(245, 65, 75),   // Fase 3: Vermelho Arena
            new Color(185, 95, 255),  // Fase 4: Roxo Champions
            new Color(255, 205, 45)   // Fase 5: Ouro da Copa
        };
        Color[] seatAccent = {
            new Color(255, 215, 40),
            new Color(220, 240, 255),
            new Color(255, 200, 60),
            new Color(255, 215, 70),
            new Color(50, 235, 130)
        };

        int themeIdx = Math.max(0, (level - 1) % borderPrimary.length);
        Color primarySportColor = (victoryFlash && (animTick / 6) % 2 == 0)
                ? new Color(255, 250, 160)
                : borderPrimary[themeIdx];
        Color crowdColor = seatAccent[themeIdx];

        // 2. Preenchimento das paredes com textura de arquibancada e decoracao esportiva
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != WALL) continue;
                int px = tileToPixelX(c);
                int py = tileToPixelY(r);

                // Base escura de concreto da arquibancada
                g2d.setColor(new Color(18, 28, 42));
                g2d.fillRect(px, py, TILE_SIZE, TILE_SIZE);

                // Degraus horizontais da arquibancada
                g2d.setColor(new Color(32, 46, 68));
                g2d.fillRect(px + 1, py + 3, TILE_SIZE - 2, 3);
                g2d.fillRect(px + 1, py + 10, TILE_SIZE - 2, 3);
                g2d.fillRect(px + 1, py + 16, TILE_SIZE - 2, 3);

                // Torcedores / assentos coloridos animados sutilmente na arquibancada
                if ((r + c) % 2 == 0) {
                    int seatShift = ((animTick / 18) + r + c) % 2;
                    g2d.setColor( new Color(crowdColor.getRed(), crowdColor.getGreen(), crowdColor.getBlue(), 145));
                    g2d.fillOval(px + 4 + seatShift, py + 3, 3, 3);
                    g2d.setColor(new Color(primarySportColor.getRed(), primarySportColor.getGreen(), primarySportColor.getBlue(), 150));
                    g2d.fillOval(px + 12 - seatShift, py + 10, 3, 3);
                } else {
                    g2d.setColor(new Color(255, 255, 255, 110));
                    g2d.fillOval(px + 8, py + 6, 3, 3);
                }
            }
        }

        // 3. Placas publicitarias / bordas iluminadas do estadio ao redor dos corredores
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != WALL) continue;

                int px = tileToPixelX(c);
                int py = tileToPixelY(r);

                boolean openUp = r > 0 && grid[r - 1][c] != WALL;
                boolean openDown = r < ROWS - 1 && grid[r + 1][c] != WALL;
                boolean openLeft = c > 0 && grid[r][c - 1] != WALL;
                boolean openRight = c < COLS - 1 && grid[r][c + 1] != WALL;

                if (!openUp && !openDown && !openLeft && !openRight) continue;

                // Iluminacao de refletor nas bordas
                g2d.setColor(new Color(primarySportColor.getRed(), primarySportColor.getGreen(), primarySportColor.getBlue(), 95));
                g2d.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawWallEdges(g2d, px, py, openUp, openDown, openLeft, openRight, 2);

                // Borda esportiva principal
                g2d.setColor(primarySportColor);
                g2d.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawWallEdges(g2d, px, py, openUp, openDown, openLeft, openRight, 2);

                // Faixa branca estilo linha de estadio
                g2d.setColor(new Color(240, 255, 245, 200));
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawWallEdges(g2d, px, py, openUp, openDown, openLeft, openRight, 4);
            }
        }

        // 4. Trave / Entrada da Area Tecnica dos Zagueiros e Itens Coletaveis
        double pulse = 0.65 + 0.35 * Math.sin(animTick * 0.14);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int tile = grid[r][c];
                int px = tileToPixelX(c);
                int py = tileToPixelY(r);
                int cx = px + TILE_SIZE / 2;
                int cy = py + TILE_SIZE / 2;

                if (tile == GHOST_DOOR) {
                    // Linha da trave / portao da defesa
                    g2d.setColor(new Color(255, 255, 255, 210));
                    g2d.fillRect(px, cy - 2, TILE_SIZE, 4);
                    g2d.setColor(new Color(255, 60, 60));
                    g2d.fillRect(px + 3, cy - 2, 4, 4);
                    g2d.fillRect(px + 13, cy - 2, 4, 4);
                } else if (tile == MINI_BALL) {
                    drawMiniSoccerBall(g2d, cx, cy);
                } else if (tile == SPECIAL_BALL) {
                    drawSpecialEnergyBall(g2d, cx, cy, pulse);
                } else if (tile == GOLD_STAR) {
                    drawGoldenStar(g2d, cx, cy, pulse);
                } else if (tile == COIN) {
                    drawGoldenCoin(g2d, cx, cy);
                }
            }
        }

        // 5. Pedestal da BOLA DE OURO (ou a propria Bola de Ouro quando desbloqueada)
        drawGoldenBallPedestal(g2d, pulse);

        // 6. Refletores nos 4 cantos do estadio e moldura externa
        g2d.setColor(new Color(255, 255, 220, 40));
        g2d.fillOval(OFFSET_X - 20, OFFSET_Y - 20, 75, 75);
        g2d.fillOval(OFFSET_X + mw - 55, OFFSET_Y - 20, 75, 75);
        g2d.fillOval(OFFSET_X - 20, OFFSET_Y + mh - 55, 75, 75);
        g2d.fillOval(OFFSET_X + mw - 55, OFFSET_Y + mh - 55, 75, 75);

        g2d.setColor(primarySportColor);
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(OFFSET_X - 2, OFFSET_Y - 2, mw + 4, mh + 4, 10, 10);
    }

    private void drawMiniSoccerBall(Graphics2D g2d, int cx, int cy) {
        // Sombra no gramado
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillOval(cx - 3, cy - 2, 7, 7);

        // Corpo branco da mini bola
        g2d.setColor(new Color(245, 250, 255));
        g2d.fillOval(cx - 4, cy - 4, 8, 8);

        // Gomos pretos classicos
        g2d.setColor(new Color(25, 30, 38));
        g2d.fillRect(cx - 1, cy - 1, 2, 2);
        g2d.fillRect(cx - 3, cy - 3, 2, 1);
        g2d.fillRect(cx + 1, cy - 3, 2, 1);
        g2d.fillRect(cx - 3, cy + 2, 2, 1);
        g2d.fillRect(cx + 1, cy + 2, 2, 1);
    }

    private void drawSpecialEnergyBall(Graphics2D g2d, int cx, int cy, double pulse) {
        int glowR = (int) (10 + 3 * pulse);
        g2d.setColor(new Color(50, 255, 160, (int) (125 * pulse)));
        g2d.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

        // Bola especial dourada/ciano de drible
        g2d.setColor(new Color(255, 235, 60));
        g2d.fillOval(cx - 7, cy - 7, 14, 14);

        g2d.setColor(new Color(0, 160, 255));
        g2d.fillOval(cx - 3, cy - 3, 6, 6);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(cx - 7, cy - 7, 14, 14);
    }

    private void drawGoldenStar(Graphics2D g2d, int cx, int cy, double pulse) {
        g2d.setColor(new Color(255, 215, 0, (int) (90 * pulse)));
        g2d.fillOval(cx - 8, cy - 8, 16, 16);

        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5.0;
            double r = (i % 2 == 0) ? 7.0 : 3.2;
            star.addPoint((int) Math.round(cx + Math.cos(angle) * r),
                          (int) Math.round(cy + Math.sin(angle) * r));
        }
        g2d.setColor(new Color(255, 225, 35));
        g2d.fillPolygon(star);
        g2d.setColor(new Color(255, 255, 200));
        g2d.fillOval(cx - 1, cy - 1, 3, 3);
    }

    private void drawGoldenCoin(Graphics2D g2d, int cx, int cy) {
        // Efeito de rotacao horizontal da moeda
        int width = Math.max(3, (int) (8 * Math.abs(Math.cos(animTick * 0.12))));
        g2d.setColor(new Color(235, 165, 10));
        g2d.fillOval(cx - width / 2 - 1, cy - 5, width + 2, 10);

        g2d.setColor(new Color(255, 225, 60));
        g2d.fillOval(cx - width / 2, cy - 4, width, 8);
    }

    private void drawGoldenBallPedestal(Graphics2D g2d, double pulse) {
        int gx = (int) getGoldenBallPixelX();
        int gy = (int) getGoldenBallPixelY();

        if (!goldenBallUnlocked) {
            // Mostra marca no gramado onde a Bola de Ouro vai surgir
            g2d.setColor(new Color(255, 215, 0, 55));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(gx - 10, gy - 10, 20, 20);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 8));
            String lockTxt = "OURO";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(lockTxt, gx - fm.stringWidth(lockTxt) / 2, gy + 3);
            return;
        }

        if (goldenBallCaptured) return;

        // Holofote e raios dourados girando sob a Bola de Ouro
        int glowR = (int) (18 + 5 * pulse);
        g2d.setColor(new Color(255, 215, 0, 95));
        g2d.fillOval(gx - glowR, gy - glowR, glowR * 2, glowR * 2);

        g2d.setColor(new Color(255, 240, 120, 160));
        g2d.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < 8; i++) {
            double ang = animTick * 0.06 + i * (Math.PI / 4.0);
            int x2 = (int) (gx + Math.cos(ang) * (15 + 3 * pulse));
            int y2 = (int) (gy + Math.sin(ang) * (15 + 3 * pulse));
            g2d.drawLine(gx, gy, x2, y2);
        }

        // A BOLA DE OURO
        int bob = (int) (Math.sin(animTick * 0.16) * 2.5);
        int by = gy + bob;

        g2d.setColor(new Color(215, 145, 0));
        g2d.fillOval(gx - 10, by - 10, 20, 20);

        g2d.setColor(new Color(255, 215, 25));
        g2d.fillOval(gx - 9, by - 9, 18, 18);

        // Detalhes de gomos dourados escuros
        g2d.setColor(new Color(165, 105, 0));
        g2d.fillPolygon(
                new int[]{gx, gx - 4, gx - 2, gx + 2, gx + 4},
                new int[]{by - 4, by - 1, by + 3, by + 3, by - 1},
                5
        );
        g2d.drawOval(gx - 9, by - 9, 18, 18);

        // Brilho especular intenso
        g2d.setColor(Color.WHITE);
        g2d.fillOval(gx - 5, by - 6, 4, 4);
    }

    private void drawWallEdges(Graphics2D g2d, int px, int py,
                               boolean openUp, boolean openDown, boolean openLeft, boolean openRight, int inset) {
        int x1 = px + (openLeft ? inset : 0);
        int x2 = px + TILE_SIZE - (openRight ? inset : 0);
        int y1 = py + (openUp ? inset : 0);
        int y2 = py + TILE_SIZE - (openDown ? inset : 0);

        if (openUp) {
            g2d.drawLine(x1, py + inset, x2, py + inset);
        }
        if (openDown) {
            g2d.drawLine(x1, py + TILE_SIZE - inset, x2, py + TILE_SIZE - inset);
        }
        if (openLeft) {
            g2d.drawLine(px + inset, y1, px + inset, y2);
        }
        if (openRight) {
            g2d.drawLine(px + TILE_SIZE - inset, y1, px + TILE_SIZE - inset, y2);
        }
    }
}
