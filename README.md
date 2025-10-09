# PPD
Projetos criados para a cadeira de Programação Paralela Distribuída 2025.2

## Requisitos de Versão do Java

Para garantir a compatibilidade e o correto funcionamento deste projeto, por favor, utilize as seguintes versões do Java:

* **Compilação e Execução:** É recomendado o uso do **JDK 21 (Java Development Kit 21)** ou superior. 
O projeto foi testado e confirmado como funcional nesta versão. Versões anteriores podem não funcionar corretamente.

## Inspiração

Este projeto foi inspirado pelo trabalho de Felipe Torquato em seu jogo Halma. A ideia inicial e a estrutura base foram desenvolvidas a partir da análise de sua implementação.

- **Repositório Original:** [FelipeTorquato/halma-game](https://github.com/FelipeTorquato/halma-game)

Agradeço ao autor por disponibilizar seu código e servir de inspiração para este projeto.

# Diferenças detalhadas

1. Regras do Jogo e Lógica do Tabuleiro (Board.java)
    - 15 peças por jogador.
    - O cliente solicita uma lista de movimentos válidos ao servidor (GET_VALID_MOVES) e destaca-os na interface antes do jogador mover a peça.
    - Distingue entre movimentos de passo único e pulos. Durante um salto em cadeia, o jogador só pode realizar outros saltos, não movimentos de passo único.

2. Design da Interface e Experiência do Usuário (UI/UX)
    - Design gráfico com gradientes, sombras e efeitos de brilho nas peças para dar uma aparência 3D.
    - A peça selecionada é destacada em vermelho, e todos os movimentos válidos são exibidos com um círculo verde, melhorando muito a jogabilidade.
    - Layout com placar de movimentos, nomes dos jogadores, e um chat mais bem estruturado.
    - O cliente apresenta uma janela de conexão mais completa, pedindo Nome do Jogador, Endereço IP e Porta.
    - Apresenta uma tela de resultados muito similar, mantendo a funcionalidade.

3. Estrutura de Código e Funcionalidades do Sistema
    - Protocolo mais específicos como SET_NAME, GET_VALID_MOVES, VALID_MOVES_LIST e UPDATE_SCORE, tornando a comunicação mais explícita e funcional.
    - Os jogadores definem seus próprios nomes, que são exibidos na interface e no chat, tornando o jogo mais pessoal.
    - O cliente primeiro solicita os movimentos válidos (sendGetValidMoves) e só depois envia o movimento escolhido, o que cria uma interação mais inteligente.
    - Implementa uma tentativa de reconexão automática se a conexão com o servidor for perdida no meio do jogo.
    - O servidor transmite o placar de movimentos em tempo real para ambos os jogadores após cada jogada válida (broadcastScoreUpdate).
    - Utiliza recursos mais modernos de Java, como Streams, para processar listas de movimentos.
