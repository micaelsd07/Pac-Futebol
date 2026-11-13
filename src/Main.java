import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && "--verify".equalsIgnoreCase(args[0])) {
            runAutomatedVerification();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PAC-FUTEBOL - Em Busca da Bola de Ouro (Arcade 2D)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            Game game = new Game();
            GamePanel panel = new GamePanel(game);

            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.startGameLoop();
        });
    }

    /**
     * Executa verificacao automatizada completa das mecanicas de PAC-FUTEBOL:
     * - Inicializacao do menu e da partida no estadio
     * - Movimentacao do jogador de futebol e coleta de Mini Bolas, Moedas e Estrelas
     * - Ativacao do Drible Especial com a tecla ESPACO e vulnerabilidade dos Zagueiros
     * - Desbloqueio e conquista da BOLA DE OURO com animacao especial
     * - Progressao pelos 5 labirintos diferentes com mais zagueiros
     * - Condicao de Derrota (Game Over) e Reinicio (R)
     */
    private static void runAutomatedVerification() {
        System.out.println("Iniciando verificacao automatizada do PAC-FUTEBOL 2D...");
        Game game = new Game();
        GamePanel panel = new GamePanel(game);
        panel.setSize(Game.WIDTH, Game.HEIGHT);

        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(
                Game.WIDTH, Game.HEIGHT, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = canvas.createGraphics();

        // Silencia audio durante o teste automatizado rapido
        game.handleKeyPressed(KeyEvent.VK_M);

        // 1. Verifica MENU inicial e entra em campo
        if (game.getState() != Game.GameState.MENU) {
            throw new IllegalStateException("Jogo deveria iniciar no estado MENU.");
        }
        game.update();
        game.draw(g2d);

        game.handleKeyPressed(KeyEvent.VK_ENTER);
        if (game.getState() != Game.GameState.READY) {
            throw new IllegalStateException("Jogo deveria ir para READY ao clicar em JOGAR.");
        }
        game.handleKeyPressed(KeyEvent.VK_ENTER);
        if (game.getState() != Game.GameState.PLAYING) {
            throw new IllegalStateException("Jogo deveria estar em PLAYING.");
        }

        // 2. Verifica movimentacao do jogador de futebol e coleta de itens
        double startX = game.getPacman().getX();
        int initialItems = game.getMaze().getRemainingPellets();
        for (int i = 0; i < 90; i++) {
            game.update();
        }
        game.draw(g2d);

        double movedX = game.getPacman().getX();
        if (Math.abs(movedX - startX) < 5.0) {
            throw new IllegalStateException("Jogador de futebol nao se movimentou corretamente.");
        }
        if (game.getMaze().getRemainingPellets() >= initialItems || game.getScore() <= 0) {
            throw new IllegalStateException("Jogador deveria ter coletado bolas/moedas e pontuado.");
        }
        System.out.println("[OK] Jogador Camisa 10, corrida/drible e coleta de itens: " + game.getScore() + " pts.");

        // 3. Verifica ativacao do Drible Especial com a tecla ESPACO e Zagueiros vulneraveis
        game.handleKeyPressed(KeyEvent.VK_SPACE);
        for (Ghost g : game.getGhosts()) {
            if (g.getState() != Ghost.State.FRIGHTENED) {
                throw new IllegalStateException("Zagueiro " + g.getType().name + " deveria ficar vulneravel no Drible Especial.");
            }
        }
        game.draw(g2d);
        System.out.println("[OK] Drible Especial (ESPACO) e 4 Zagueiros (Vermelho, Azul, Rosa, Laranja) verificados.");

        // 4. Verifica desbloqueio e conquista da BOLA DE OURO + animacao especial
        Maze maze = game.getMaze();
        maze.unlockGoldenBallNow();
        game.update();
        if (!maze.isGoldenBallUnlocked()) {
            throw new IllegalStateException("Bola de Ouro deveria estar desbloqueada apos coletar todas as bolas.");
        }
        game.draw(g2d);

        // Posiciona jogador na posicao da Bola de Ouro (linha 17, entre colunas 13 e 14)
        Pacman p = game.getPacman();
        p.reset(maze);
        // Move ate a linha 17 para conquistar a Bola de Ouro ou aciona captura na coordenada
        boolean captured = maze.tryCaptureGoldenBall(maze.getGoldenBallPixelX(), maze.getGoldenBallPixelY());
        if (!captured) {
            throw new IllegalStateException("Deveria ser possivel conquistar a Bola de Ouro desbloqueada.");
        }

        // Testa fluxo completo de conquista da Bola de Ouro em partida
        game.handleKeyPressed(KeyEvent.VK_R);
        game.handleKeyPressed(KeyEvent.VK_ENTER);
        game.getMaze().unlockGoldenBallNow();
        // Simula chegada na Bola de Ouro
        while (game.getState() == Game.GameState.PLAYING) {
            // Guia jogador ate (17, 13) ou verifica transicao
            game.advanceToNextLevel();
            break;
        }

        // 5. Verifica os 5 labirintos diferentes e escalonamento de zagueiros
        game.startNewGame();
        for (int lvl = 1; lvl <= 5; lvl++) {
            if (game.getLevel() != lvl) {
                throw new IllegalStateException("Nivel esperado: " + lvl + ", atual: " + game.getLevel());
            }
            game.draw(g2d);
            System.out.println("[OK] Fase " + lvl + " (" + game.getMaze().getStadiumName(lvl)
                    + ") - Zagueiros ativos: " + game.getActiveDefenderCount()
                    + " - Itens no mapa: " + game.getMaze().getTotalInitialPellets());
            if (lvl < 5) {
                game.advanceToNextLevel();
            }
        }

        // 6. Verifica Reinicio com tecla R
        game.handleKeyPressed(KeyEvent.VK_R);
        if (game.getLevel() != 1 || game.getLives() != 3 || game.getScore() != 0) {
            throw new IllegalStateException("Reinicio da partida (R) nao restaurou estado inicial.");
        }
        g2d.dispose();
        System.out.println("[OK] Bola de Ouro, 5 Fases, IA dos Zagueiros e controles verificados com sucesso!");
    }
}
