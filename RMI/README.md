# Jogo Halma - Implementação Distribuída com Java RMI

Este módulo contém a implementação do jogo **Halma** utilizando **Java RMI (Remote Method Invocation)**. Diferente da versão com Sockets puros, esta abordagem abstrai a comunicação de rede através de chamadas de métodos em objetos remotos, facilitando a interação entre o cliente e o servidor.

Projeto desenvolvido para a disciplina de **Programação Paralela e Distribuída (PPD) - 2025.2**.

## Sobre a Implementação RMI

Nesta versão, a comunicação não é baseada em troca de mensagens de texto cruas (strings), mas sim na invocação de métodos definidos em interfaces partilhadas.

## Arquitetura

O sistema segue o padrão **Cliente-Servidor** com **Callbacks**, permitindo que o servidor notifique os clientes (push) sobre eventos do jogo como o movimento do oponente ou mensagens de chat.

  * **Servidor de Matchmaking (`IServerOperations`):** O ponto de entrada onde os clientes se conectam e aguardam por um oponente.
  * **Sessão de Jogo (`IGameSession`):** Uma vez pareados, os clientes recebem uma referência para um objeto de sessão exclusivo, onde enviam seus movimentos e mensagens.
  * **Callback do Cliente (`IClientCallback`):** O cliente envia uma referência de si mesmo para o servidor. Isso permite que o servidor chame métodos no cliente para atualizar o tabuleiro e o chat em tempo real.

## Requisitos

  * **Java JDK:** Versão 21 ou superior (Recomendado).
  * **Bibliotecas:** Java Swing (nativa do JDK) para a interface gráfica.

## Estrutura dos Pacotes

  * `br.com.victorsfec.halma.common`: Contém as interfaces RMI (`IClientCallback`, `IGameSession`, `IServerOperations`) conhecidas tanto pelo cliente quanto pelo servidor.
  * `br.com.victorsfec.halma.server`: Contém a implementação do servidor (`HalmaServerRMI`), a lógica da sessão (`GameSessionRMI`) e o registro do serviço.
  * `br.com.victorsfec.halma.client`: Contém a interface gráfica (`GameFrame`), a lógica do cliente RMI (`HalmaClientRMI`) e a implementação do callback.
  * `br.com.victorsfec.halma.game`: Contém a lógica do tabuleiro e das peças (compartilhada).

## Funcionalidades Específicas (RMI)

  * **Conexão Transparente:** Utiliza `Naming.lookup` para encontrar o servidor de matchmaking.
  * **Interatividade em Tempo Real:** Graças à implementação de `ClientCallbackImpl`, o servidor consegue atualizar a tela do jogador instantaneamente quando o oponente faz uma jogada ou envia uma mensagem, sem necessidade de *polling*.
  * **Tratamento de Falhas:** O cliente RMI possui lógica para detectar `RemoteException` e tentar reconectar ou notificar o usuário caso o servidor caia.
  * **Validação Remota:** O cliente solicita ao servidor a lista de movimentos válidos (`sendGetValidMoves`) antes de permitir a jogada, garantindo que a lógica do jogo seja centralizada e segura.

## Regras do Jogo

As regras são idênticas à implementação base:

  * O objetivo é mover todas as peças para o canto oposto.
  * Movimentos podem ser passos simples ou saltos (incluindo cadeias de saltos).
  * O jogo detecta vitória, derrota e desistência.