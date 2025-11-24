# PPD

Projetos criados para a cadeira de Programação Paralela Distribuída 2025.2

## Requisitos de Versão do Java

Para garantir a compatibilidade e o correto funcionamento deste projeto, por favor, utilize as seguintes versões do Java:

  * **Compilação e Execução:** É recomendado o uso do **JDK 21 (Java Development Kit 21)** ou superior. O projeto foi testado e confirmado como funcional nesta versão. Versões anteriores podem não funcionar corretamente.

## Inspiração

Este projeto foi inspirado pelo trabalho de Felipe Torquato em seu jogo Halma. A ideia inicial e a estrutura base foram desenvolvidas a partir da análise de sua implementação.

  * **Repositório Original:** [FelipeTorquato/halma-game](https://github.com/FelipeTorquato/halma-game)

Agradeço ao autor por disponibilizar seu código e servir de inspiração para este projeto.

## Diferenças detalhadas

## Regras do Jogo e Lógica do Tabuleiro (Board.java)

  * 15 peças por jogador.
  * O cliente solicita uma lista de movimentos válidos ao servidor (`GET_VALID_MOVES`) e destaca-os na interface antes do jogador mover a peça.
  * Distingue entre movimentos de passo único e pulos. Durante um salto em cadeia, o jogador só pode realizar outros saltos, não movimentos de passo único.

## Design da Interface e Experiência do Usuário (UI/UX)

  * Design gráfico com gradientes, sombras e efeitos de brilho nas peças para dar uma aparência 3D.
  * A peça selecionada é destacada em vermelho, e todos os movimentos válidos são exibidos com um círculo verde, melhorando muito a jogabilidade.
  * Layout com placar de movimentos, nomes dos jogadores, e um chat mais bem estruturado.
  * O cliente apresenta uma janela de conexão mais completa, pedindo Nome do Jogador, Endereço IP e Porta.
  * Apresenta uma tela de resultados muito similar, mantendo a funcionalidade.

## Estrutura de Código e Funcionalidades do Sistema

  * Protocolo mais específicos como `SET_NAME`, `GET_VALID_MOVES`, `VALID_MOVES_LIST` e `UPDATE_SCORE`, tornando a comunicação mais explícita e funcional.
  * Os jogadores definem seus próprios nomes, que são exibidos na interface e no chat, tornando o jogo mais pessoal.
  * O cliente primeiro solicita os movimentos válidos (`sendGetValidMoves`) e só depois envia o movimento escolhido, o que cria uma interação mais inteligente.
  * Implementa uma tentativa de reconexão automática se a conexão com o servidor for perdida no meio do jogo.
  * O servidor transmite o placar de movimentos em tempo real para ambos os jogadores após cada jogada válida (`broadcastScoreUpdate`).
  * Utiliza recursos mais modernos de Java, como Streams, para processar listas de movimentos.

## Comparação de Arquiteturas: Sockets vs. RMI

Este projeto explora duas abordagens fundamentais de sistemas distribuídos para implementar o mesmo jogo (Halma). Abaixo apresenta-se uma análise comparativa das decisões de design, complexidade e funcionamento de cada versão.

## 1. Resumo Técnico

| Característica | Implementação com Sockets | Implementação com RMI |
| :--- | :--- | :--- |
| **Nível de Abstração** | Baixo (Transporte TCP direto) | Alto (Chamada de Procedimento Remoto) |
| **Protocolo de Dados** | Texto Personalizado (`String`) | Serialização Java Nativa (`Serializable`) |
| **Comunicação** | Troca de Mensagens (Message Passing) | Invocação de Métodos (Method Invocation) |
| **Gestão de Estado** | `ClientHandler` processa streams | Objetos distribuídos (`GameSessionRMI`) |
| **Notificação (Push)** | Loop de leitura contínua na thread do cliente | Callbacks via interface `IClientCallback` |
| **Definição de API** | Classe estática `Protocol.java` | Interfaces `IGameSession` e `IServerOperations` |

## 2. Análise da Implementação com Sockets

Nesta abordagem, a comunicação é "manual". O programador é responsável por formatar, enviar, receber e analisar cada byte de dados.

  * **Protocolo:** Foi definido um protocolo baseado em texto, utilizando o delimitador `:` (ex: `MOVE:0:0:1:1`).
  * **Fluxo:** O servidor mantém uma thread `ClientHandler` para cada jogador, que fica num loop infinito (`while ((inputLine = in.readLine()) != null)`) à espera de comandos.
  * **Parsing:** Cada mensagem recebida deve ser dividida (`split`) e interpretada num `switch/case` gigante tanto no cliente como no servidor.
  * **Vantagem:** Maior controlo sobre o tráfego de rede e independência de linguagem (o cliente poderia ser escrito em C++ ou Python, desde que respeitasse o protocolo de texto).
  * **Desvantagem:** Código mais verboso e propenso a erros de formatação de string.

**Exemplo de Envio de Movimento (Sockets):**

```java
// É necessário concatenar strings manualmente
out.println(Protocol.MOVE + Protocol.SEPARATOR + startRow + 
            Protocol.SEPARATOR + startCol + 
            Protocol.SEPARATOR + endRow + 
            Protocol.SEPARATOR + endCol);
```

## 3. Análise da Implementação com RMI

Nesta abordagem, a rede torna-se transparente. O cliente invoca métodos num objeto local (stub) como se o servidor estivesse na mesma máquina virtual.

  * **Interfaces:** O contrato entre cliente e servidor é estritamente tipado através de interfaces Java (`IGameSession`, `IClientCallback`).
  * **Fluxo:** O servidor exporta objetos remotos. Quando o cliente chama um método, o RMI encarrega-se de serializar os parâmetros, enviá-los, executar no servidor e devolver o resultado ou exceção.
  * **Callbacks:** Para que o servidor envie dados ao cliente (ex: o oponente jogou), o cliente exporta a si mesmo (`ClientCallbackImpl`) e passa a sua referência ao servidor, permitindo comunicação bidirecional real.
  * **Vantagem:** Desenvolvimento mais rápido, código mais limpo e segurança de tipos (type-safety) em tempo de compilação.
  * **Desvantagem:** Acoplamento forte à tecnologia Java e complexidade na configuração inicial do registo (`LocateRegistry`).

**Exemplo de Envio de Movimento (RMI):**

```java
// Chamada direta de método, sem manipulação de strings
gameSessionStub.sendMove(gameFrame.getPlayerId(), startRow, startCol, endRow, endCol);
```

## 4. Conclusão da Comparação

A implementação com **Sockets** exigiu a criação de uma "camada de aplicação" própria (o protocolo definido em `Protocol.java`), dando total controlo sobre os bytes trocados, mas exigindo mais código para gestão de threads e *parsing*.

A implementação com **RMI** abstraiu toda a camada de rede. O foco do desenvolvimento deslocou-se para a definição correta das interfaces (`IGameSession`). Embora a configuração inicial do RMI seja ligeiramente mais complexa, a lógica de jogo resultante em `GameSessionRMI.java` é mais limpa do que a manipulação de strings encontrada em `GameSession.java`, pois lida diretamente com objetos e exceções.