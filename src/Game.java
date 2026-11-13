import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class Game {

    public enum GameState {
        MENU,
        READY,
        PLAYING,
        GOLDEN_CELEBRATION,
        LIFE_LOST,
        PAUSED,
        LEVEL_COMPLETE,
        GAME_OVER
    }

    public static final int WIDTH = 900;
    public static final int HEIGHT = 700;

    private final Maze maze;
    private final Pacman pacman;
    private final Ghost[] allDefenders;
    private final Menu menu;
    private final SoundManager sound;
    private final VisualEffect effects;

    private GameState state = GameState.MENU;

    private int score = 0;
    private int highScore = 5000;
    private int lives = 3;
    private int level = 1;

    private long matchTicks = 0;
    private int stateTimer = 0;
    private int hitStopTimer = 0;

    // Modo de Drible Especial (ativado por Bolas Especiais ou tecla ESPACO)
    private int powerPelletTimer = 0;
    private int powerPelletMaxDuration = 420;
    private int specialDribbleCharges = 1;
    private int specialDribbleMeter = 0;
    private int ghostEatCombo = 0;

    private boolean goldenBallAnnounced = false;
    private int aiWaveTicks = 0;

    // Botoes interativos na barra lateral do estadio
    private final Rectangle sidebarDribbleBtn = new Rectangle(644, 488, 224, 34);
    private final Rectangle sidebarPauseBtn = new Rectangle(644, 528, 224, 34);
    private final Rectangle sidebarRestartBtn = new Rectangle(644, 568, 224, 34);
    private final Rectangle sidebarSoundBtn = new Rectangle(644, 608, 108, 34);
    private final Rectangle sidebarMenuBtn = new Rectangle(760, 608, 108, 34);

    // Botoes nas telas sobrepostas
    private final Rectangle overlayPrimaryBtn = new Rectangle(195, 415, 240, 48);
    private final Rectangle overlayRestartBtn = new Rectangle(195, 475, 240, 48);
    private final Rectangle overlayMenuBtn = new Rectangle(195, 535, 240, 48);
    private int overlayHoveredBtn = 0;

    public Game() {
        this.maze = new Maze();
        this.pacman = new Pacman(maze);
        this.allDefenders = new Ghost[] {
            new Ghost(Ghost.GhostType.RED, maze, 0),
            new Ghost(Ghost.GhostType.BLUE, maze, 1),
            new Ghost(Ghost.GhostType.PINK, maze, 2),
            new Ghost(Ghost.GhostType.ORANGE, maze, 3),
            new Ghost(Ghost.GhostType.RED, maze, 4),
            new Ghost(Ghost.GhostType.BLUE, maze, 5),
            new Ghost(Ghost.GhostType.ORANGE, maze, 6)
        };
        this.menu = new Menu();
        this.sound = new SoundManager();
        this.effects = new VisualEffect();
    }

    public int getActiveDefenderCount() {
        if (level <= 1) return 4;
        if (level <= 3) return 5;
        if (level == 4) return 6;
        return 7;
    }

    public void startNewGame() {
        this.score = 0;
        this.lives = 3;
        this.level = 1;
        this.matchTicks = 0;
        this.powerPelletTimer = 0;
        this.specialDribbleCharges = 1;
        this.specialDribbleMeter = 0;
        this.ghostEatCombo = 0;
        this.goldenBallAnnounced = false;
        this.aiWaveTicks = 0;
        this.effects.clear();
        this.maze.resetMaze(this.level);
        resetPositions();
        this.state = GameState.READY;
        this.stateTimer = 70;
        this.sound.playStartGameSound();
    }

    public void advanceToNextLevel() {
        this.level++;
        this.score += 500 * (level - 1);
        if (score > highScore) {
            highScore = score;
        }
        this.powerPelletTimer = 0;
        this.specialDribbleCharges = Math.min(2, this.specialDribbleCharges + 1);
        this.ghostEatCombo = 0;
        this.goldenBallAnnounced = false;
        this.aiWaveTicks = 0;
        this.effects.clear();
        this.maze.resetMaze(this.level);
        resetPositions();
        this.state = GameState.READY;
        this.stateTimer = 70;
        this.sound.playStartGameSound();
    }

    private void resetPositions() {
        pacman.reset(maze);
        for (Ghost g : allDefenders) {
            g.reset(maze);
        }
        powerPelletTimer = 0;
        ghostEatCombo = 0;
    }

    public void activateSpecialDribble() {
        ghostEatCombo = 0;
        powerPelletMaxDuration = Math.max(180, 430 - (level - 1) * 42);
        powerPelletTimer = powerPelletMaxDuration;
        int activeCount = getActiveDefenderCount();
        for (int i = 0; i < activeCount; i++) {
            allDefenders[i].enterFrightenedMode(powerPelletMaxDuration);
        }
        effects.spawnPowerPelletBurst(pacman.getX(), pacman.getY());
        effects.spawnFloatingScore(pacman.getX(), pacman.getY() - 10, "DRIBLE ESPECIAL!", new Color(60, 255, 150));
        sound.playPowerPelletSound();
    }

    public boolean tryUseManualSpecialDribble() {
        if (state != GameState.PLAYING) return false;
        if (specialDribbleCharges <= 0) return false;
        specialDribbleCharges--;
        activateSpecialDribble();
        return true;
    }

    public void update() {
        if (state == GameState.MENU) {
            menu.update();
            return;
        }

        effects.update();

        if (hitStopTimer > 0) {
            hitStopTimer--;
            return;
        }

        if (state == GameState.READY) {
            stateTimer--;
            if (stateTimer <= 0) {
                state = GameState.PLAYING;
            }
            return;
        }

        if (state == GameState.GOLDEN_CELEBRATION) {
            maze.update();
            if (stateTimer % 16 == 0) {
                double fx = Maze.OFFSET_X + 80 + ((stateTimer * 37) % (maze.getWidthPixels() - 160));
                double fy = Maze.OFFSET_Y + 90 + ((stateTimer * 53) % (maze.getHeightPixels() - 180));
                effects.spawnGoldenCelebrationFirework(fx, fy);
            }
            stateTimer--;
            if (stateTimer <= 0) {
                state = GameState.LEVEL_COMPLETE;
                overlayHoveredBtn = 0;
            }
            return;
        }

        if (state == GameState.LIFE_LOST) {
            boolean finished = pacman.updateDeathAnimation();
            if (finished) {
                stateTimer--;
                if (stateTimer <= 0) {
                    if (lives <= 0) {
                        state = GameState.GAME_OVER;
                        overlayHoveredBtn = 0;
                        sound.playGameOverSound();
                    } else {
                        resetPositions();
                        state = GameState.READY;
                        stateTimer = 55;
                    }
                }
            }
            return;
        }

        if (state == GameState.PAUSED || state == GameState.LEVEL_COMPLETE || state == GameState.GAME_OVER) {
            maze.update();
            return;
        }

        // Estado PLAYING
        matchTicks++;
        aiWaveTicks++;
        maze.update();

        if (powerPelletTimer > 0) {
            powerPelletTimer--;
            if (powerPelletTimer == 0) {
                ghostEatCombo = 0;
            }
        }

        pacman.update(maze, level);

        // Coleta itens pelo labirinto: Mini Bolas, Bolas Especiais, Estrelas Douradas e Moedas
        int pacRow = pacman.getRow(maze);
        int pacCol = pacman.getCol(maze);
        int collected = maze.collectAt(pacRow, pacCol);

        if (collected == Maze.MINI_BALL) {
            addScore(10);
            chargeDribbleMeter(4);
            effects.spawnPelletSpark(pacman.getX(), pacman.getY(), Color.WHITE);
            sound.playPelletSound();
        } else if (collected == Maze.COIN) {
            addScore(30);
            chargeDribbleMeter(14);
            effects.spawnPelletSpark(pacman.getX(), pacman.getY(), new Color(255, 215, 30));
            effects.spawnFloatingScore(pacman.getX(), pacman.getY() - 6, "+30 MOEDA", new Color(255, 225, 60));
            sound.playCoinOrStarSound();
        } else if (collected == Maze.GOLD_STAR) {
            addScore(50);
            chargeDribbleMeter(25);
            effects.spawnPelletSpark(pacman.getX(), pacman.getY(), new Color(255, 235, 80));
            effects.spawnFloatingScore(pacman.getX(), pacman.getY() - 6, "+50 ESTRELA!", new Color(255, 240, 90));
            sound.playCoinOrStarSound();
        } else if (collected == Maze.SPECIAL_BALL) {
            addScore(50);
            if (powerPelletTimer > 0 && specialDribbleCharges < 2) {
                specialDribbleCharges++;
            }
            activateSpecialDribble();
        }

        // Verifica se todas as bolas foram coletadas para anunciar e liberar a BOLA DE OURO
        if (maze.isGoldenBallUnlocked() && !goldenBallAnnounced) {
            goldenBallAnnounced = true;
            effects.spawnGoldenBallUnlockEffect(maze.getGoldenBallPixelX(), maze.getGoldenBallPixelY());
            sound.playGoldenBallUnlockedSound();
        }

        // Verifica se o jogador chegou ate a BOLA DE OURO desbloqueada
        if (maze.tryCaptureGoldenBall(pacman.getX(), pacman.getY())) {
            addScore(1000);
            effects.spawnGoldenCelebrationFirework(pacman.getX(), pacman.getY());
            effects.spawnFloatingScore(pacman.getX(), pacman.getY() - 12, "BOLA DE OURO! +1000", new Color(255, 230, 40));
            sound.playVictorySound();
            state = GameState.GOLDEN_CELEBRATION;
            stateTimer = 85;
            return;
        }

        boolean scatterWave = isScatterWave();
        int activeCount = getActiveDefenderCount();
        Ghost redDefender = allDefenders[0];
        for (int i = 0; i < activeCount; i++) {
            allDefenders[i].update(maze, pacman, redDefender, level, scatterWave);
        }

        checkDefenderCollisions(activeCount);
    }

    private void chargeDribbleMeter(int amount) {
        if (specialDribbleCharges >= 2) {
            specialDribbleMeter = 0;
            return;
        }
        specialDribbleMeter += amount;
        if (specialDribbleMeter >= 100) {
            specialDribbleMeter -= 100;
            specialDribbleCharges = Math.min(2, specialDribbleCharges + 1);
            effects.spawnFloatingScore(pacman.getX(), pacman.getY() - 12, "+1 DRIBLE (ESPACO)", new Color(80, 255, 160));
        }
    }

    private boolean isScatterWave() {
        int cycle = aiWaveTicks % 1200;
        int scatterLimit = Math.max(80, 230 - (level - 1) * 30);
        return cycle < scatterLimit;
    }

    private void checkDefenderCollisions(int activeCount) {
        for (int i = 0; i < activeCount; i++) {
            Ghost g = allDefenders[i];
            double dist = Math.hypot(pacman.getX() - g.getX(), pacman.getY() - g.getY());
            if (dist < 15.5) {
                if (g.getState() == Ghost.State.FRIGHTENED) {
                    ghostEatCombo++;
                    int pts = 200 * (1 << Math.min(3, ghostEatCombo - 1));
                    addScore(pts);
                    g.onCaptured();
                    effects.spawnGhostEatenEffect(g.getX(), g.getY(), pts, g.getType().color);
                    sound.playEatGhostSound();
                    hitStopTimer = 10;
                } else if (g.getState() == Ghost.State.NORMAL) {
                    lives--;
                    pacman.startDeathAnimation();
                    effects.spawnDeathExplosion(pacman.getX(), pacman.getY());
                    sound.playLoseLifeSound();
                    state = GameState.LIFE_LOST;
                    stateTimer = 35;
                    break;
                }
            }
        }
    }

    private void addScore(int pts) {
        score += pts;
        if (score > highScore) {
            highScore = score;
        }
    }

    public void handleKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_M) {
            sound.toggleMute();
            return;
        }

        if (state == GameState.MENU) {
            if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                menu.moveSelectionUp(sound);
            } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                menu.moveSelectionDown(sound);
            } else if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                Menu.MenuAction action = menu.confirmSelection(sound);
                processMenuAction(action);
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                if (menu.getCurrentSubScreen() != Menu.SubScreen.MAIN) {
                    sound.playMenuClickSound();
                    menu.setSubScreen(Menu.SubScreen.MAIN);
                }
            }
            return;
        }

        // R reinicia a partida imediatamente
        if (keyCode == KeyEvent.VK_R) {
            startNewGame();
            return;
        }

        if (state == GameState.GOLDEN_CELEBRATION) {
            if (keyCode == KeyEvent.VK_ENTER) {
                state = GameState.LEVEL_COMPLETE;
                overlayHoveredBtn = 0;
            }
            return;
        }

        if (state == GameState.PAUSED) {
            if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) {
                sound.playMenuClickSound();
                if (overlayHoveredBtn == 0 || keyCode == KeyEvent.VK_ESCAPE) {
                    state = GameState.PLAYING;
                } else if (overlayHoveredBtn == 1) {
                    startNewGame();
                } else if (overlayHoveredBtn == 2) {
                    state = GameState.MENU;
                }
            } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                overlayHoveredBtn = (overlayHoveredBtn + 2) % 3;
                sound.playMenuClickSound();
            } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                overlayHoveredBtn = (overlayHoveredBtn + 1) % 3;
                sound.playMenuClickSound();
            }
            return;
        }

        if (state == GameState.LEVEL_COMPLETE) {
            if (keyCode == KeyEvent.VK_ENTER) {
                if (overlayHoveredBtn == 0) {
                    advanceToNextLevel();
                } else if (overlayHoveredBtn == 1) {
                    startNewGame();
                } else {
                    state = GameState.MENU;
                }
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                state = GameState.MENU;
            } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                overlayHoveredBtn = (overlayHoveredBtn + 2) % 3;
                sound.playMenuClickSound();
            } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                overlayHoveredBtn = (overlayHoveredBtn + 1) % 3;
                sound.playMenuClickSound();
            }
            return;
        }

        if (state == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.VK_ENTER) {
                if (overlayHoveredBtn == 2) {
                    state = GameState.MENU;
                } else {
                    startNewGame();
                }
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                state = GameState.MENU;
            } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
                overlayHoveredBtn = (overlayHoveredBtn == 0) ? 2 : 0;
                sound.playMenuClickSound();
            } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
                overlayHoveredBtn = (overlayHoveredBtn == 0) ? 2 : 0;
                sound.playMenuClickSound();
            }
            return;
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            if (state == GameState.PLAYING || state == GameState.READY) {
                sound.playMenuClickSound();
                state = GameState.PAUSED;
                overlayHoveredBtn = 0;
            }
            return;
        }

        if (keyCode == KeyEvent.VK_ENTER && state == GameState.READY) {
            state = GameState.PLAYING;
            return;
        }

        // ESPACO: Ativa Drible Especial quando disponivel
        if (keyCode == KeyEvent.VK_SPACE) {
            if (state == GameState.READY) {
                state = GameState.PLAYING;
            }
            tryUseManualSpecialDribble();
            return;
        }

        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                pacman.setRequestedDirection(Direction.UP);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                pacman.setRequestedDirection(Direction.DOWN);
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                pacman.setRequestedDirection(Direction.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                pacman.setRequestedDirection(Direction.RIGHT);
                break;
            default:
                break;
        }
    }

    public void handleMouseMoved(int mx, int my) {
        if (state == GameState.MENU) {
            menu.handleMouseMoved(mx, my, sound);
            return;
        }

        if (state == GameState.PAUSED || state == GameState.LEVEL_COMPLETE || state == GameState.GAME_OVER) {
            if (overlayPrimaryBtn.contains(mx, my)) {
                overlayHoveredBtn = 0;
            } else if (overlayRestartBtn.contains(mx, my) && state != GameState.GAME_OVER) {
                overlayHoveredBtn = 1;
            } else if (overlayMenuBtn.contains(mx, my)) {
                overlayHoveredBtn = 2;
            }
        }
    }

    public void handleMouseClicked(int mx, int my) {
        if (state == GameState.MENU) {
            Menu.MenuAction action = menu.handleMouseClicked(mx, my, sound);
            processMenuAction(action);
            return;
        }

        if (sidebarDribbleBtn.contains(mx, my)) {
            tryUseManualSpecialDribble();
            return;
        }
        if (sidebarPauseBtn.contains(mx, my)) {
            sound.playMenuClickSound();
            if (state == GameState.PLAYING || state == GameState.READY) {
                state = GameState.PAUSED;
                overlayHoveredBtn = 0;
            } else if (state == GameState.PAUSED) {
                state = GameState.PLAYING;
            }
            return;
        }
        if (sidebarRestartBtn.contains(mx, my)) {
            startNewGame();
            return;
        }
        if (sidebarSoundBtn.contains(mx, my)) {
            sound.toggleMute();
            sound.playMenuClickSound();
            return;
        }
        if (sidebarMenuBtn.contains(mx, my)) {
            sound.playMenuClickSound();
            state = GameState.MENU;
            return;
        }

        if (state == GameState.PAUSED) {
            if (overlayPrimaryBtn.contains(mx, my)) {
                sound.playMenuClickSound();
                state = GameState.PLAYING;
            } else if (overlayRestartBtn.contains(mx, my)) {
                startNewGame();
            } else if (overlayMenuBtn.contains(mx, my)) {
                sound.playMenuClickSound();
                state = GameState.MENU;
            }
        } else if (state == GameState.LEVEL_COMPLETE) {
            if (overlayPrimaryBtn.contains(mx, my)) {
                advanceToNextLevel();
            } else if (overlayRestartBtn.contains(mx, my)) {
                startNewGame();
            } else if (overlayMenuBtn.contains(mx, my)) {
                sound.playMenuClickSound();
                state = GameState.MENU;
            }
        } else if (state == GameState.GAME_OVER) {
            if (overlayPrimaryBtn.contains(mx, my) || overlayRestartBtn.contains(mx, my)) {
                startNewGame();
            } else if (overlayMenuBtn.contains(mx, my)) {
                sound.playMenuClickSound();
                state = GameState.MENU;
            }
        }
    }

    private void processMenuAction(Menu.MenuAction action) {
        if (action == Menu.MenuAction.START_GAME) {
            startNewGame();
        } else if (action == Menu.MenuAction.EXIT_GAME) {
            System.exit(0);
        }
    }

    public void draw(Graphics2D g2d) {
        if (state == GameState.MENU) {
            menu.draw(g2d, WIDTH, HEIGHT, highScore);
            return;
        }

        g2d.setColor(new Color(6, 20, 10));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        boolean victoryFlash = (state == GameState.LEVEL_COMPLETE || state == GameState.GOLDEN_CELEBRATION);
        maze.draw(g2d, level, victoryFlash);

        boolean energized = powerPelletTimer > 0;
        if (state == GameState.GOLDEN_CELEBRATION) {
            pacman.drawGoldenCelebration(g2d, stateTimer);
        } else {
            pacman.draw(g2d, energized);
        }

        if (state != GameState.LIFE_LOST && state != GameState.GOLDEN_CELEBRATION) {
            int activeCount = getActiveDefenderCount();
            for (int i = 0; i < activeCount; i++) {
                allDefenders[i].draw(g2d);
            }
        }

        effects.draw(g2d);

        // Banner dinamico quando a Bola de Ouro estiver liberada
        if (state == GameState.PLAYING && maze.isGoldenBallUnlocked() && !maze.isGoldenBallCaptured()) {
            drawGoldenBallUnlockedBanner(g2d);
        }

        if (state == GameState.GOLDEN_CELEBRATION) {
            drawCelebrationBanner(g2d);
        }

        drawArcadeSidebar(g2d);

        if (state == GameState.READY) {
            drawReadyBanner(g2d);
        }

        if (state == GameState.PAUSED) {
            drawPauseOverlay(g2d);
        } else if (state == GameState.LEVEL_COMPLETE) {
            drawVictoryOverlay(g2d);
        } else if (state == GameState.GAME_OVER) {
            drawGameOverOverlay(g2d);
        }
    }

    private void drawGoldenBallUnlockedBanner(Graphics2D g2d) {
        int cx = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        int by = Maze.OFFSET_Y + 6;
        g2d.setColor(new Color(15, 45, 20, 230));
        g2d.fillRoundRect(cx - 175, by, 350, 26, 10, 10);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(cx - 175, by, 350, 26, 10, 10);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
        String txt = "BOLA DE OURO DISPONIVEL NO CENTRO! CONQUISTE-A!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(txt, cx - fm.stringWidth(txt) / 2, by + 18);
    }

    private void drawCelebrationBanner(Graphics2D g2d) {
        int cx = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        int cy = Maze.OFFSET_Y + 11 * Maze.TILE_SIZE;

        g2d.setColor(new Color(10, 35, 18, 235));
        g2d.fillRoundRect(cx - 190, cy - 28, 380, 56, 16, 16);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(2.8f));
        g2d.drawRoundRect(cx - 190, cy - 28, 380, 56, 16, 16);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        String msg = "GOLACO! BOLA DE OURO CONQUISTADA!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 7);
    }

    private void drawArcadeSidebar(Graphics2D g2d) {
        int sx = 632;
        int sy = 22;
        int sw = 248;
        int sh = 655;

        GradientPaint panelGrad = new GradientPaint(
                sx, sy, new Color(10, 36, 18),
                sx, sy + sh, new Color(6, 20, 10)
        );
        g2d.setPaint(panelGrad);
        g2d.fillRoundRect(sx, sy, sw, sh, 16, 16);

        g2d.setColor(new Color(50, 220, 100, 85));
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.drawRoundRect(sx, sy, sw, sh, 16, 16);

        g2d.setColor(new Color(255, 215, 35));
        g2d.setStroke(new BasicStroke(1.6f));
        g2d.drawRoundRect(sx, sy, sw, sh, 16, 16);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.setColor(new Color(255, 232, 25));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "PAC-FUTEBOL";
        g2d.drawString(title, sx + (sw - fm.stringWidth(title)) / 2, sy + 30);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(140, 240, 170));
        String stadName = maze.getStadiumName(level);
        fm = g2d.getFontMetrics();
        g2d.drawString(stadName, sx + (sw - fm.stringWidth(stadName)) / 2, sy + 44);

        // Placar e Recorde
        drawMetricBox(g2d, sx + 14, sy + 52, sw - 28, 48, "PLACAR (PONTOS)", String.format("%06d", score), new Color(255, 235, 60));
        drawMetricBox(g2d, sx + 14, sy + 106, sw - 28, 46, "RECORDE DO ESTADIO", String.format("%06d", highScore), new Color(90, 255, 150));

        // Fase e Cronometro
        int halfW = (sw - 34) / 2;
        drawMetricBox(g2d, sx + 14, sy + 158, halfW, 46, "FASE", "FASE " + level, new Color(255, 220, 70));
        drawMetricBox(g2d, sx + 20 + halfW, sy + 158, halfW, 46, "CRONOMETRO", formatTime(matchTicks), new Color(255, 190, 90));

        // Vidas com icones de Camisa 10 + Bola
        g2d.setColor(new Color(14, 44, 22));
        g2d.fillRoundRect(sx + 14, sy + 210, sw - 28, 48, 12, 12);
        g2d.setColor(new Color(45, 165, 85));
        g2d.drawRoundRect(sx + 14, sy + 210, sw - 28, 48, 12, 12);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(170, 240, 190));
        g2d.drawString("VIDAS DO JOGADOR (" + lives + ")", sx + 24, sy + 225);

        for (int i = 0; i < Math.max(0, lives); i++) {
            int px = sx + 28 + i * 38;
            int py = sy + 232;
            g2d.setColor(new Color(255, 225, 15));
            g2d.fillRoundRect(px, py, 16, 16, 6, 6);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(px + 18, py + 5, 8, 8);
        }

        // Progresso para desbloquear a BOLA DE OURO + Carga de Drible Especial
        g2d.setColor(new Color(14, 44, 22));
        g2d.fillRoundRect(sx + 14, sy + 264, sw - 28, 76, 12, 12);
        g2d.setColor(new Color(45, 165, 85));
        g2d.drawRoundRect(sx + 14, sy + 264, sw - 28, 76, 12, 12);

        int totalItems = Math.max(1, maze.getTotalInitialPellets());
        int collectedItems = totalItems - maze.getRemainingPellets();
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        if (maze.isGoldenBallUnlocked()) {
            g2d.setColor(new Color(255, 225, 40));
            g2d.drawString("BOLA DE OURO: DESBLOQUEADA!", sx + 24, sy + 280);
        } else {
            g2d.setColor(new Color(170, 240, 190));
            g2d.drawString("BOLAS P/ BOLA DE OURO: " + collectedItems + "/" + totalItems, sx + 24, sy + 280);
        }

        int barX = sx + 24;
        int barW = sw - 48;
        g2d.setColor(new Color(6, 18, 10));
        g2d.fillRoundRect(barX, sy + 285, barW, 9, 5, 5);
        int fillW = (int) ((double) collectedItems / totalItems * barW);
        g2d.setColor(new Color(255, 215, 30));
        g2d.fillRoundRect(barX, sy + 285, fillW, 9, 5, 5);

        // Status do Drible Especial (Tecla ESPACO)
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        if (powerPelletTimer > 0) {
            g2d.setColor(new Color(80, 255, 160));
            g2d.drawString("DRIBLE ESPECIAL ATIVO!", sx + 24, sy + 312);
            int pFill = (int) ((double) powerPelletTimer / Math.max(1, powerPelletMaxDuration) * barW);
            g2d.setColor(new Color(50, 255, 140));
            g2d.fillRoundRect(barX, sy + 317, pFill, 8, 4, 4);
        } else {
            g2d.setColor(specialDribbleCharges > 0 ? new Color(100, 255, 160) : new Color(160, 200, 175));
            g2d.drawString("CARGAS DE DRIBLE (ESPACO): " + specialDribbleCharges + "  [" + specialDribbleMeter + "%]", sx + 24, sy + 312);
            g2d.setColor(new Color(6, 18, 10));
            g2d.fillRoundRect(barX, sy + 317, barW, 8, 4, 4);
            int mFill = (int) ((specialDribbleCharges > 0 ? 1.0 : (specialDribbleMeter / 100.0)) * barW);
            g2d.setColor(specialDribbleCharges > 0 ? new Color(60, 245, 130) : new Color(255, 200, 50));
            g2d.fillRoundRect(barX, sy + 317, mFill, 8, 4, 4);
        }

        // Lista dos Zagueiros em Campo
        g2d.setColor(new Color(14, 44, 22));
        g2d.fillRoundRect(sx + 14, sy + 346, sw - 28, 112, 12, 12);
        g2d.setColor(new Color(45, 165, 85));
        g2d.drawRoundRect(sx + 14, sy + 346, sw - 28, 112, 12, 12);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(170, 240, 190));
        g2d.drawString("ZAGUEIROS EM CAMPO (" + getActiveDefenderCount() + ")", sx + 24, sy + 362);

        for (int i = 0; i < 4; i++) {
            Ghost g = allDefenders[i];
            int gy = sy + 380 + i * 19;
            g2d.setColor(g.getType().color);
            g2d.fillRoundRect(sx + 24, gy - 9, 10, 10, 4, 4);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2d.setColor(g.getType().color);
            String shortName = g.getType().name.replace("Zagueiro ", "Zag. ");
            g2d.drawString(shortName, sx + 38, gy);

            String stText;
            Color stColor;
            if (g.getState() == Ghost.State.FRIGHTENED) {
                stText = "VULNERAVEL";
                stColor = new Color(110, 225, 255);
            } else if (g.getState() == Ghost.State.EATEN) {
                stText = "DRIBLADO";
                stColor = new Color(150, 255, 170);
            } else {
                stText = g.getType().role;
                stColor = new Color(210, 235, 220);
            }
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2d.setColor(stColor);
            g2d.drawString(stText, sx + 122, gy);
        }

        // Botoes de acao
        Color dribbleColor = (specialDribbleCharges > 0) ? new Color(60, 245, 130) : new Color(90, 130, 105);
        drawSidebarButton(g2d, sidebarDribbleBtn, "DRIBLE ESPECIAL (ESPACO) [" + specialDribbleCharges + "]", dribbleColor);

        String pauseLabel = (state == GameState.PAUSED) ? "CONTINUAR (ESC)" : "PAUSAR (ESC)";
        drawSidebarButton(g2d, sidebarPauseBtn, pauseLabel, new Color(60, 200, 255));
        drawSidebarButton(g2d, sidebarRestartBtn, "REINICIAR PARTIDA (R)", new Color(255, 210, 40));
        drawSidebarButton(g2d, sidebarSoundBtn, sound.isMuted() ? "SOM: OFF" : "SOM: ON", new Color(120, 240, 160));
        drawSidebarButton(g2d, sidebarMenuBtn, "MENU", new Color(255, 130, 180));
    }

    private void drawMetricBox(Graphics2D g2d, int x, int y, int w, int h, String label, String value, Color valueColor) {
        g2d.setColor(new Color(14, 44, 22));
        g2d.fillRoundRect(x, y, w, h, 12, 12);
        g2d.setColor(new Color(45, 165, 85));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(x, y, w, h, 12, 12);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2d.setColor(new Color(170, 240, 190));
        g2d.drawString(label, x + 10, y + 14);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.setColor(valueColor);
        g2d.drawString(value, x + 10, y + 36);
    }

    private void drawSidebarButton(Graphics2D g2d, Rectangle rect, String text, Color accent) {
        g2d.setColor(new Color(14, 48, 24));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        g2d.setColor(accent);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, tx, ty);
    }

    private void drawReadyBanner(Graphics2D g2d) {
        int cx = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        int cy = Maze.OFFSET_Y + 17 * Maze.TILE_SIZE + 10;

        g2d.setColor(new Color(8, 34, 16, 230));
        g2d.fillRoundRect(cx - 125, cy - 22, 250, 36, 12, 12);
        g2d.setColor(new Color(255, 225, 35));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(cx - 125, cy - 22, 250, 36, 12, 12);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        String msg = "FASE " + level + ": APITO INICIAL!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 2);
    }

    private void drawPauseOverlay(Graphics2D g2d) {
        drawModalFrame(g2d, "PARTIDA PAUSADA", new Color(80, 245, 140));

        int centerX = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.setColor(new Color(220, 245, 225));
        String sub = "Pressione ESC ou ENTER para voltar ao gramado";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(sub, centerX - fm.stringWidth(sub) / 2, 340);

        drawOverlayButton(g2d, overlayPrimaryBtn, "CONTINUAR (ENTER)", overlayHoveredBtn == 0, new Color(80, 245, 140));
        drawOverlayButton(g2d, overlayRestartBtn, "REINICIAR (R)", overlayHoveredBtn == 1, new Color(255, 220, 40));
        drawOverlayButton(g2d, overlayMenuBtn, "MENU PRINCIPAL", overlayHoveredBtn == 2, new Color(60, 215, 255));
    }

    private void drawVictoryOverlay(Graphics2D g2d) {
        drawModalFrame(g2d, "BOLA DE OURO CONQUISTADA!", new Color(255, 215, 30));

        int centerX = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        g2d.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2d.setColor(new Color(255, 235, 80));
        String s1 = "Fase " + level + " (" + maze.getStadiumName(level) + ") Vencida!";
        String s2 = "Placar: " + score + " pts  |  Tempo: " + formatTime(matchTicks);

        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(s1, centerX - fm.stringWidth(s1) / 2, 322);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2d.setColor(new Color(215, 245, 225));
        fm = g2d.getFontMetrics();
        g2d.drawString(s2, centerX - fm.stringWidth(s2) / 2, 354);

        String s3 = "Proximo Estadio: Fase " + (level + 1) + " com mais zagueiros e velocidade!";
        fm = g2d.getFontMetrics();
        g2d.setColor(new Color(110, 255, 165));
        g2d.drawString(s3, centerX - fm.stringWidth(s3) / 2, 385);

        drawOverlayButton(g2d, overlayPrimaryBtn, "PROXIMA FASE (ENTER)", overlayHoveredBtn == 0, new Color(80, 255, 150));
        drawOverlayButton(g2d, overlayRestartBtn, "REINICIAR DO ZERO (R)", overlayHoveredBtn == 1, new Color(255, 220, 40));
        drawOverlayButton(g2d, overlayMenuBtn, "MENU PRINCIPAL (ESC)", overlayHoveredBtn == 2, new Color(60, 215, 255));
    }

    private void drawGameOverOverlay(Graphics2D g2d) {
        drawModalFrame(g2d, "FIM DE JOGO (GAME OVER)", new Color(255, 70, 75));

        int centerX = Maze.OFFSET_X + maze.getWidthPixels() / 2;
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.setColor(new Color(255, 235, 70));
        String s1 = "Placar Final: " + score + " pts";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(s1, centerX - fm.stringWidth(s1) / 2, 325);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.setColor(new Color(215, 240, 225));
        String s2 = "Estadio Alcancado: Fase " + level + "   |   Recorde: " + highScore + " pts";
        fm = g2d.getFontMetrics();
        g2d.drawString(s2, centerX - fm.stringWidth(s2) / 2, 358);

        String s3 = "Tempo em Campo: " + formatTime(matchTicks);
        fm = g2d.getFontMetrics();
        g2d.drawString(s3, centerX - fm.stringWidth(s3) / 2, 385);

        drawOverlayButton(g2d, overlayPrimaryBtn, "JOGAR NOVAMENTE (R)", overlayHoveredBtn == 0, new Color(255, 225, 45));
        drawOverlayButton(g2d, overlayMenuBtn, "MENU PRINCIPAL (ESC)", overlayHoveredBtn == 2, new Color(80, 240, 145));
    }

    private void drawModalFrame(Graphics2D g2d, String title, Color neonColor) {
        int mx = Maze.OFFSET_X;
        int my = Maze.OFFSET_Y;
        int mw = maze.getWidthPixels();
        int mh = maze.getHeightPixels();

        g2d.setColor(new Color(4, 16, 8, 215));
        g2d.fillRoundRect(mx, my, mw, mh, 12, 12);

        int boxW = 445;
        int boxH = 380;
        int boxX = mx + (mw - boxW) / 2;
        int boxY = 230;

        g2d.setColor(new Color(8, 32, 16, 245));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2d.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), 80));
        g2d.setStroke(new BasicStroke(6.0f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2d.setColor(neonColor);
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 25));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(neonColor);
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 50);

        g2d.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), 110));
        g2d.drawLine(boxX + 35, boxY + 66, boxX + boxW - 35, boxY + 66);
    }

    private void drawOverlayButton(Graphics2D g2d, Rectangle rect, String text, boolean selected, Color accent) {
        if (selected) {
            g2d.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 65));
            g2d.fillRoundRect(rect.x - 4, rect.y - 4, rect.width + 8, rect.height + 8, 16, 16);
            g2d.setColor(new Color(18, 68, 34));
        } else {
            g2d.setColor(new Color(12, 40, 20));
        }
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);

        g2d.setColor(selected ? accent : new Color(50, 165, 90));
        g2d.setStroke(new BasicStroke(selected ? 2.4f : 1.5f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.setColor(selected ? Color.WHITE : new Color(210, 240, 220));
        g2d.drawString(text, tx, ty);
    }

    private String formatTime(long ticks) {
        long totalSeconds = ticks / 60;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (ticks % 60) / 6;
        return String.format("%02d:%02d.%d", minutes, seconds, tenths);
    }

    public GameState getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public int getLevel() {
        return level;
    }

    public Pacman getPacman() {
        return pacman;
    }

    public Ghost[] getGhosts() {
        return Arrays.copyOf(allDefenders, getActiveDefenderCount());
    }

    public Maze getMaze() {
        return maze;
    }
}
