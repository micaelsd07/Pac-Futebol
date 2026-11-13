import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Menu {

    public enum SubScreen {
        MAIN,
        HOW_TO_PLAY,
        CREDITS
    }

    public enum MenuAction {
        NONE,
        START_GAME,
        EXIT_GAME
    }

    private static final String[] BUTTON_LABELS = {
        "JOGAR",
        "COMO JOGAR",
        "CREDITOS",
        "SAIR"
    };

    private final Rectangle[] buttonBounds = new Rectangle[BUTTON_LABELS.length];
    private final Rectangle backButtonBounds = new Rectangle(330, 608, 240, 48);

    private int selectedIndex = 0;
    private boolean backButtonHovered = false;
    private SubScreen currentSubScreen = SubScreen.MAIN;

    private int animTick = 0;
    private double chaseX = -120.0;
    private boolean chaseReversed = false;
    private final double[] starX = new double[45];
    private final double[] starY = new double[45];
    private final double[] starSpeed = new double[45];

    public Menu() {
        int btnWidth = 280;
        int btnHeight = 52;
        int startX = (900 - btnWidth) / 2;
        int startY = 345;
        int gap = 65;

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttonBounds[i] = new Rectangle(startX, startY + i * gap, btnWidth, btnHeight);
        }

        for (int i = 0; i < starX.length; i++) {
            starX[i] = (i * 73) % 900;
            starY[i] = (i * 59) % 700;
            starSpeed[i] = 0.3 + (i % 5) * 0.18;
        }
    }

    public SubScreen getCurrentSubScreen() {
        return currentSubScreen;
    }

    public void setSubScreen(SubScreen subScreen) {
        this.currentSubScreen = subScreen;
    }

    public void update() {
        animTick++;

        for (int i = 0; i < starX.length; i++) {
            starY[i] += starSpeed[i];
            if (starY[i] > 700) {
                starY[i] = 0;
                starX[i] = (starX[i] + 137) % 900;
            }
        }

        if (!chaseReversed) {
            chaseX += 2.6;
            if (chaseX > 980) {
                chaseReversed = true;
            }
        } else {
            chaseX -= 2.6;
            if (chaseX < -240) {
                chaseReversed = false;
            }
        }
    }

    public void moveSelectionUp(SoundManager sound) {
        if (currentSubScreen == SubScreen.MAIN) {
            selectedIndex = (selectedIndex - 1 + BUTTON_LABELS.length) % BUTTON_LABELS.length;
            sound.playMenuClickSound();
        }
    }

    public void moveSelectionDown(SoundManager sound) {
        if (currentSubScreen == SubScreen.MAIN) {
            selectedIndex = (selectedIndex + 1) % BUTTON_LABELS.length;
            sound.playMenuClickSound();
        }
    }

    public MenuAction confirmSelection(SoundManager sound) {
        sound.playMenuClickSound();
        if (currentSubScreen != SubScreen.MAIN) {
            currentSubScreen = SubScreen.MAIN;
            return MenuAction.NONE;
        }
        return triggerButton(selectedIndex);
    }

    public void handleMouseMoved(int mx, int my, SoundManager sound) {
        if (currentSubScreen == SubScreen.MAIN) {
            for (int i = 0; i < buttonBounds.length; i++) {
                if (buttonBounds[i].contains(mx, my)) {
                    if (selectedIndex != i) {
                        selectedIndex = i;
                        sound.playMenuClickSound();
                    }
                    break;
                }
            }
        } else {
            backButtonHovered = backButtonBounds.contains(mx, my);
        }
    }

    public MenuAction handleMouseClicked(int mx, int my, SoundManager sound) {
        if (currentSubScreen == SubScreen.MAIN) {
            for (int i = 0; i < buttonBounds.length; i++) {
                if (buttonBounds[i].contains(mx, my)) {
                    selectedIndex = i;
                    sound.playMenuClickSound();
                    return triggerButton(i);
                }
            }
        } else {
            if (backButtonBounds.contains(mx, my)) {
                sound.playMenuClickSound();
                currentSubScreen = SubScreen.MAIN;
            }
        }
        return MenuAction.NONE;
    }

    private MenuAction triggerButton(int index) {
        switch (index) {
            case 0:
                return MenuAction.START_GAME;
            case 1:
                currentSubScreen = SubScreen.HOW_TO_PLAY;
                return MenuAction.NONE;
            case 2:
                currentSubScreen = SubScreen.CREDITS;
                return MenuAction.NONE;
            case 3:
                return MenuAction.EXIT_GAME;
            default:
                return MenuAction.NONE;
        }
    }

    public void draw(Graphics2D g2d, int width, int height, int highScore) {
        drawStadiumBackground(g2d, width, height);

        if (currentSubScreen == SubScreen.MAIN) {
            drawMainScreen(g2d, width, height, highScore);
        } else if (currentSubScreen == SubScreen.HOW_TO_PLAY) {
            drawHowToPlayScreen(g2d, width, height);
        } else if (currentSubScreen == SubScreen.CREDITS) {
            drawCreditsScreen(g2d, width, height);
        }
    }

    private void drawStadiumBackground(Graphics2D g2d, int width, int height) {
        // Gradiente verde-escuro noturno de estadio
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(6, 26, 12),
                0, height, new Color(10, 44, 20)
        );
        g2d.setPaint(bgGrad);
        g2d.fillRect(0, 0, width, height);

        // Linhas sutis do gramado e marcacao de campo
        g2d.setColor(new Color(80, 220, 110, 22));
        g2d.setStroke(new BasicStroke(1.2f));
        for (int y = 0; y < height; y += 38) {
            g2d.drawLine(0, y, width, y);
        }
        g2d.drawOval(width / 2 - 190, height / 2 - 190, 380, 380);

        // Luzes dos refletores e particulas douradas
        for (int i = 0; i < starX.length; i++) {
            int alpha = 70 + (int) (55 * Math.sin(animTick * 0.05 + i));
            Color c = (i % 3 == 0)
                    ? new Color(255, 220, 40, alpha)
                    : (i % 3 == 1 ? new Color(60, 255, 130, alpha) : new Color(255, 255, 255, alpha));
            g2d.setColor(c);
            int size = (i % 2 == 0) ? 3 : 4;
            g2d.fillOval((int) starX[i], (int) starY[i], size, size);
        }

        // Moldura esportiva iluminada
        g2d.setColor(new Color(40, 225, 100, 55));
        g2d.setStroke(new BasicStroke(6.0f));
        g2d.drawRoundRect(16, 16, width - 32, height - 32, 24, 24);

        g2d.setColor(new Color(255, 215, 35));
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(20, 20, width - 40, height - 40, 20, 20);
    }

    private void drawMainScreen(Graphics2D g2d, int width, int height, int highScore) {
        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        String hsText = String.format("RECORDE DO ESTADIO: %06d PTS", highScore);
        FontMetrics hsFm = g2d.getFontMetrics();
        g2d.setColor(new Color(100, 255, 150));
        g2d.drawString(hsText, (width - hsFm.stringWidth(hsText)) / 2, 54);

        drawStylizedTitle(g2d, width);
        drawAnimatedSoccerChase(g2d, width, 272);

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            Rectangle rect = buttonBounds[i];
            boolean selected = (i == selectedIndex);
            drawNeonButton(g2d, rect, BUTTON_LABELS[i], selected);
        }

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String footer = "SETAS / WASD: Mover  |  ESPACO: Drible Especial  |  ENTER / CLIQUE: Selecionar";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(new Color(180, 235, 195));
        g2d.drawString(footer, (width - fm.stringWidth(footer)) / 2, height - 34);
    }

    private void drawStylizedTitle(Graphics2D g2d, int width) {
        String title = "PAC-FUTEBOL";
        Font titleFont = new Font("SansSerif", Font.BOLD, 70);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (width - fm.stringWidth(title)) / 2;
        int ty = 148;

        double pulse = 0.65 + 0.35 * Math.sin(animTick * 0.08);

        int boxW = fm.stringWidth(title) + 84;
        int boxX = (width - boxW) / 2;
        g2d.setColor(new Color(8, 34, 18, 225));
        g2d.fillRoundRect(boxX, 74, boxW, 100, 26, 26);

        g2d.setColor(new Color(50, 235, 110, (int) (130 * pulse)));
        g2d.setStroke(new BasicStroke(4.5f));
        g2d.drawRoundRect(boxX, 74, boxW, 100, 26, 26);

        g2d.setColor(new Color(255, 220, 40));
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(boxX + 5, 79, boxW - 10, 90, 20, 20);

        g2d.setColor(new Color(0, 120, 45, 200));
        g2d.drawString(title, tx + 5, ty + 5);

        g2d.setColor(new Color(215, 140, 0, 220));
        g2d.drawString(title, tx + 3, ty + 3);

        g2d.setColor(new Color(255, 238, 35));
        g2d.drawString(title, tx, ty);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        String sub = "LABIRINTO ARCADE 2D  -  EM BUSCA DA BOLA DE OURO";
        FontMetrics sfm = g2d.getFontMetrics();
        g2d.setColor(new Color(110, 255, 160));
        g2d.drawString(sub, (width - sfm.stringWidth(sub)) / 2, 204);
    }

    private void drawAnimatedSoccerChase(Graphics2D g2d, int width, int centerY) {
        // Corredor de gramado no menu
        g2d.setColor(new Color(16, 68, 30));
        g2d.fillRoundRect(86, centerY - 26, width - 172, 52, 14, 14);

        g2d.setColor(new Color(60, 230, 115, 140));
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.drawRoundRect(86, centerY - 26, width - 172, 52, 14, 14);

        // Mini bolas de futebol na pista do menu
        for (int px = 125; px < width - 120; px += 36) {
            boolean collected = (!chaseReversed && px < chaseX) || (chaseReversed && px > chaseX);
            if (!collected) {
                g2d.setColor(Color.WHITE);
                g2d.fillOval(px - 4, centerY - 4, 8, 8);
                g2d.setColor(Color.BLACK);
                g2d.fillRect(px - 1, centerY - 1, 2, 2);
            }
        }

        // Jogador Camisa 10 driblando a bola no menu
        int px = (int) chaseX;
        int ballOffset = chaseReversed ? -14 : 14;
        int dribbleY = (int) (Math.sin(animTick * 0.3) * 4);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(px + ballOffset - 4, centerY + dribbleY - 4, 9, 9);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(px + ballOffset - 1, centerY + dribbleY - 1, 2, 2);

        g2d.setColor(new Color(255, 225, 15));
        g2d.fillRoundRect(px - 10, centerY - 11, 20, 22, 10, 10);
        g2d.setColor(new Color(0, 165, 65));
        g2d.drawRoundRect(px - 10, centerY - 11, 20, 22, 10, 10);
        g2d.setColor(new Color(235, 180, 130));
        g2d.fillOval(px - 5, centerY - 5, 10, 10);

        // 4 Zagueiros perseguindo (Vermelho, Azul, Rosa, Laranja)
        Color[] defColors = {
            new Color(235, 40, 50),
            new Color(30, 165, 255),
            new Color(255, 105, 195),
            new Color(255, 150, 25)
        };

        for (int i = 0; i < 4; i++) {
            int gx = (int) (chaseX - (i + 1) * 46);
            int gy = centerY;
            Color c = chaseReversed ? new Color(30, 80, 235) : defColors[i];

            g2d.setColor(c);
            g2d.fillRoundRect(gx - 10, gy - 10, 20, 20, 9, 9);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(gx - 8, gy - 6, gx - 4, gy - 6);
            g2d.drawLine(gx + 4, gy - 6, gx + 8, gy - 6);

            g2d.setColor(new Color(225, 170, 125));
            g2d.fillOval(gx - 5, gy - 5, 10, 10);
        }
    }

    private void drawNeonButton(Graphics2D g2d, Rectangle rect, String text, boolean selected) {
        double pulse = 0.7 + 0.3 * Math.sin(animTick * 0.12);

        if (selected) {
            g2d.setColor(new Color(50, 235, 110, (int) (90 * pulse)));
            g2d.fillRoundRect(rect.x - 5, rect.y - 5, rect.width + 10, rect.height + 10, 20, 20);

            GradientPaint gp = new GradientPaint(
                    rect.x, rect.y, new Color(18, 78, 36),
                    rect.x, rect.y + rect.height, new Color(10, 44, 20)
            );
            g2d.setPaint(gp);
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);

            g2d.setColor(new Color(255, 230, 40));
            g2d.setStroke(new BasicStroke(2.6f));
            g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);

            // Mini bola de ouro ao lado do botao selecionado
            g2d.setColor(new Color(255, 215, 20));
            g2d.fillOval(rect.x + 18, rect.y + rect.height / 2 - 8, 16, 16);
            g2d.setColor(new Color(160, 100, 0));
            g2d.drawOval(rect.x + 18, rect.y + rect.height / 2 - 8, 16, 16);
        } else {
            g2d.setColor(new Color(10, 32, 18, 215));
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);

            g2d.setColor(new Color(45, 165, 85));
            g2d.setStroke(new BasicStroke(1.8f));
            g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 16, 16);
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 21));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();

        g2d.setColor(selected ? new Color(255, 240, 80) : new Color(210, 245, 220));
        g2d.drawString(text, tx, ty);
    }

    private void drawHowToPlayScreen(Graphics2D g2d, int width, int height) {
        drawSubScreenPanel(g2d, width, height, "COMO JOGAR");

        int leftX = 105;
        int rightX = 480;

        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(new Color(255, 230, 50));
        g2d.drawString("CONTROLES DO JOGADOR", leftX, 172);

        String[][] controls = {
            {"SETAS / WASD", "Correr e driblar pelo labirinto"},
            {"ESPACO", "Ativar Drible Especial (quando disponivel)"},
            {"ESC", "Pausar ou retomar a partida"},
            {"R", "Reiniciar a partida imediatamente"},
            {"M", "Ativar / silenciar efeitos sonoros"}
        };

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        int y = 202;
        for (String[] row : controls) {
            g2d.setColor(new Color(80, 255, 140));
            g2d.drawString(row[0], leftX, y);
            g2d.setColor(new Color(225, 245, 230));
            g2d.drawString("- " + row[1], leftX + 115, y);
            y += 28;
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(new Color(255, 230, 50));
        g2d.drawString("A BOLA DE OURO & REGRAS", leftX, 368);

        String[] rules = {
            "• Colete todas as Mini Bolas, Estrelas e Moedas do labirinto",
            "  para desbloquear a lendaria BOLA DE OURO no centro!",
            "• Quando a Bola de Ouro surgir, chegue ate ela para",
            "  conquistar o trofeu e avancar para o proximo estadio!",
            "• Bolas Especiais ou a tecla ESPACO ativam o Drible Especial:",
            "  os zagueiros ficam vulneraveis e podem ser driblados!",
            "• Sao 5 estadios diferentes com mais zagueiros e velocidade."
        };
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2d.setColor(new Color(220, 240, 225));
        y = 396;
        for (String line : rules) {
            g2d.drawString(line, leftX, y);
            y += 24;
        }

        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(new Color(255, 230, 50));
        g2d.drawString("INTELIGENCIA DOS ZAGUEIROS", rightX, 172);

        Ghost.GhostType[] types = Ghost.GhostType.values();
        String[] descriptions = {
            "Persegue diretamente a posicao do jogador.",
            "Preve o caminho 4 casas a frente do jogador.",
            "Patrulha os corredores estrategicos do estadio.",
            "Alterna entre perseguicao direta e patrulha."
        };

        y = 210;
        for (int i = 0; i < types.length; i++) {
            Ghost.GhostType gt = types[i];
            g2d.setColor(gt.color);
            g2d.fillRoundRect(rightX, y - 16, 24, 22, 8, 8);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.setColor(gt.color);
            g2d.drawString(gt.name.toUpperCase(), rightX + 34, y - 3);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2d.setColor(new Color(210, 235, 220));
            g2d.drawString(descriptions[i], rightX + 34, y + 15);
            y += 64;
        }

        drawNeonButton(g2d, backButtonBounds, "VOLTAR (ESC)", true);
    }

    private void drawCreditsScreen(Graphics2D g2d, int width, int height) {
        drawSubScreenPanel(g2d, width, height, "CREDITOS");

        String[] lines = {
            "PAC-FUTEBOL: EM BUSCA DA BOLA DE OURO",
            "",
            "Jogabilidade classica de labirinto estilo PAC-MAN",
            "com tematica completa de Futebol Arcade 2D.",
            "",
            "RECURSOS IMPLEMENTADOS:",
            "• Jogador Camisa 10 visto de cima com animacao de drible",
            "• 5 Labirintos de Estadios com arquibancadas e gramado",
            "• Zagueiros com IA propria (Perseguidor, Previsor, Patrulheiro, Misto)",
            "• Coleta de Mini Bolas, Estrelas Douradas, Moedas e Bolas Especiais",
            "• Desbloqueio e comemoracao especial da BOLA DE OURO",
            "",
            "Desenvolvido 100% em Java Swing e Java 2D (Graphics2D)."
        };

        int y = 182;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == 0 || line.startsWith("RECURSOS")) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 19));
                g2d.setColor(new Color(255, 232, 45));
            } else if (line.startsWith("•")) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
                g2d.setColor(new Color(90, 255, 150));
            } else {
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
                g2d.setColor(new Color(220, 240, 225));
            }
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(line, (width - fm.stringWidth(line)) / 2, y);
            y += 28;
        }

        drawNeonButton(g2d, backButtonBounds, "VOLTAR (ESC)", backButtonHovered || true);
    }

    private void drawSubScreenPanel(Graphics2D g2d, int width, int height, String header) {
        g2d.setColor(new Color(8, 28, 16, 238));
        g2d.fillRoundRect(75, 65, width - 150, height - 130, 26, 26);

        g2d.setColor(new Color(60, 235, 115));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(75, 65, width - 150, height - 130, 26, 26);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 34));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(new Color(255, 232, 35));
        g2d.drawString(header, (width - fm.stringWidth(header)) / 2, 116);

        g2d.setColor(new Color(60, 235, 115, 120));
        g2d.drawLine(120, 132, width - 120, 132);
    }
}
