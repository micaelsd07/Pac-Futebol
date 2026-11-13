# PAC-FUTEBOL: Em Busca da Bola de Ouro (Java 2D Arcade)

Jogo completo no estilo clássico **Pac-Man** (visão 2D superior, labirinto fechado, corredores estreitos, perseguição e coleta), totalmente tematizado como **Futebol Arcade**, desenvolvido exclusivamente em **Java Swing** e **Java 2D (`Graphics2D`)**.

## Destaques da Temática de Futebol

- **Personagem Principal (Camisa 10):** Visto de cima com uniforme amarelo e verde, chuteiras animadas na corrida e uma bola de futebol nos pés com animação contínua de drible.
- **Labirinto Estádio de Futebol:** Gramado verde escuro com faixas de corte e marcações de cal, paredes com detalhes de arquibancada, torcida e iluminação esportiva de refletores.
- **4 Tipos de Zagueiros com IA Própria (+ Reforços por Fase):**
  - **Zagueiro Vermelho:** Persegue diretamente a posição atual do jogador.
  - **Zagueiro Azul:** Tenta prever o caminho do jogador (4 casas à frente).
  - **Zagueiro Rosa:** Patrulha os setores estratégicos do labirinto.
  - **Zagueiro Laranja:** Alterna dinamicamente entre perseguir e patrulhar.
- **Itens Coletáveis:** Mini Bolas de Futebol (`10 pts`), Moedas (`30 pts`), Estrelas Douradas (`50 pts`) e Bolas Especiais (`50 pts` + ativa o Modo de Drible Especial).
- **A Bola de Ouro:** Ao coletar todas as bolas do labirinto, a **Bola de Ouro** surge iluminada no centro do estádio. Chegue até ela para exibir a animação especial de comemoração e avançar de fase!
- **5 Labirintos Diferentes:**
  1. *Fase 1 - Arena Clássica Verde-Ouro* (4 Zagueiros)
  2. *Fase 2 - Estádio Olímpico Azul* (5 Zagueiros)
  3. *Fase 3 - Caldeirão Rubro-Negro* (5 Zagueiros)
  4. *Fase 4 - Arena Real Champions* (6 Zagueiros)
  5. *Fase 5 - Templo da Grande Final* (7 Zagueiros)

## Como Compilar e Executar

```powershell
cd C:\Users\Aluno\.gemini\antigravity\scratch\pacman-java
mkdir -Force bin
javac -encoding UTF-8 -d bin src\*.java
java -cp bin Main
```

Ou dê dois cliques em `executar.bat`.

## Controles

| Tecla | Ação |
| :--- | :--- |
| **Setas Direcionais** ou **WASD** | Movimentar e driblar pelos corredores |
| **ESPAÇO** | Ativar **Drible Especial** quando disponível |
| **ENTER** | Iniciar partida, confirmar opção ou avançar de fase |
| **ESC** | Pausar / Retomar o jogo ou voltar ao menu |
| **R** | Reiniciar a partida imediatamente |
| **M** | Ativar / Silenciar o áudio |
