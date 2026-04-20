FinFlow - Ecossistema de Gestão Financeira
O FinFlow é um projeto pessoal de gestão financeira que combina processamento robusto em Java com interfaces modernas. Ele oferece duas formas de uso: uma aplicação desktop com visual inspirado no design da Apple e um dashboard web interativo.

Funcionalidades
Interface Desktop (Swing)
Visual limpo e moderno: Cards arredondados, sombras suaves e tipografia bem hierarquizada, com estética próxima ao design da Apple.

Gráficos dinâmicos: Visualização de despesas por categoria via Donut Charts e visão mensal em gráfico de barras.

Categorias inteligentes: O sistema adapta as categorias disponíveis conforme o tipo de transação — Entrada, Saída ou Investimento.

Dashboard Web (Full-Stack)
Servidor próprio em Java: O backend atua como servidor HTTP e expõe uma API REST sem dependências externas.

Interface responsiva: Dashboard com suporte a Dark Mode, funciona bem em diferentes tamanhos de tela.

Dados em tempo real: Comunicação direta com o arquivo de persistência local (finances.csv).

Inteligência Financeira
Simulador CDI: Cálculo de rendimento com base em juros compostos reais.
Estimativa de cashback: Retorno automático sobre despesas registradas.
Persistência eficiente: Leitura e escrita em CSV usando Java NIO para boa performance.

Tecnologias
Back-end e Desktop
Java SE (JDK 17+): Toda a lógica de negócio e cálculos financeiros.
Swing e AWT: Interface desktop com customização avançada de componentes.
Java HttpServer: Servidor web embutido para servir a API e o front-end.
Front-end Web
Vanilla JavaScript: Manipulação do DOM e consumo da API.
Chart.js: Gráficos interativos e responsivos.
CSS moderno: Layout com Flexbox e Grid, usando variáveis CSS para suporte a temas.

Como Executar
Pré-requisitos
Java JDK 17 ou superior instalado.

1. Clonar o repositório
bash
git clone https://github.com/seu-usuario/finflow.git
cd finflow
2. Rodar a versão desktop
bash
javac *.java
java FinanceWindow
3. Rodar a versão web
bash
java FinanceServer
Depois é só acessar http://localhost:8080 no navegador.

Estrutura do Projeto
FinanceWindow.java — Interface desktop principal (Swing)
FinanceServer.java — Servidor HTTP e endpoints da API
FinanceService.java — Lógica, cálculos e regras de negócio
FinanceFileHandler.java — Gerenciamento da persistência em CSV
/frontend — Contém o index.html, style.css e app.js do dashboard

finances.csv — Arquivo de dados

Autor
Desenvolvido por Vitor.

Este projeto cobre conceitos de POO, desenvolvimento full-stack com Java puro, design de interfaces e manipulação de dados.
