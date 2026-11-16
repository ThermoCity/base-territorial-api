# base-territorial-api — ThermoCity

API da feature **Base Territorial de Presidente Prudente** (Épico 1). Guarda no PostgreSQL os dados preparados no QGIS
e responde às perguntas da camada **ONDE** do gêmeo digital.

| PBI | O que a API entrega |
|---|---|
| #3 Setores + Censo 2022 | 420 setores com população total, 60–69 e 70+ (`/setores`) |
| #4 IPVS 2022 (Seade) | grupo de vulnerabilidade 1–6 por setor; "Não classificado" vira `SEM_DADO` |
| #5 UBS e refúgios | 36 UBS (CNES) e 155 possíveis refúgios (OpenStreetMap) com distância em metros |
| #6 Cadastro sintético | pessoas 60+ **fictícias** distribuídas pelos setores na proporção do Censo |
| #11 Mapa de calor | classe de calor 1–5 por setor (Landsat, 08/01/2024) no bloco `calor` e em `/setores/geojson` |

## Como rodar

Requisitos: **JDK 25** e acesso ao PostgreSQL. As credenciais ficam só em variáveis de ambiente:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/base_territorial"
$env:DB_USERNAME="rmXXXXX"
$env:DB_PASSWORD="sua_senha"
.\gradlew.bat bootRun
```

A API usa as tabelas oficiais `TMC_SETOR`, `TMC_SERVICO`, `TMC_CALOR_SETOR` e `TMC_PESSOA_SINTETICA` (sequência
`TMC_SEQ_PESSOA_SINTETICA`). Quem cria e carrega essas tabelas é o repositório **thermocity-banco-de-dados**
(`sql/01_criar_tabelas.sql` e `carga/carregar.py`); rode-o antes de subir a API.

A API só **confere** o esquema (`ddl-auto=validate`): se faltar uma tabela ou coluna, ela não sobe e o log mostra
qual (ex.: `Schema-validation: missing column [ndvi_medio] in table [tmc_calor_setor]`).

A carga inicial por CSV fica desligada. Para um banco local vazio, dá para ligar com `DDL_AUTO=update` e
`CARGA_INICIAL=true`, que cria as tabelas e importa os CSVs de `src/main/resources/dados`.

Testes (usam H2 em memória, não precisam do PostgreSQL): `.\gradlew.bat test`

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/setores` | todos os setores (sem geometria) |
| GET | `/setores/{codigo}` | setor com Censo, IPVS e calor — 404 se não existir |
| GET | `/setores/geojson` | polígonos dos setores (GeoJSON) com `situacao`, `distrito`, `bairro`, `pop60mais`, `pop70mais`, `ipvsGrupo`, `classeCalor` e `lstVeraoC`, para o mapa do painel |
| GET | `/servicos` | todas as UBS e os refúgios sugeríveis (`manter` e `potencial`) com coordenada, para o mapa do painel |
| GET | `/setores/{codigo}/servicos?raioMetros=1500` | UBS e refúgio mais próximos + serviços no raio |
| GET | `/refugios/proximo?lat=&lon=&incluirPotenciais=false` | refúgio mais próximo de um ponto — 400 fora do município |
| POST | `/admin/importacoes/setores` · `/ipvs` · `/servicos` · `/calor` | importa CSV (campo `arquivo`) — 422 se houver linha inválida, e nada é gravado |
| POST | `/admin/importacoes/padrao` | reimporta os quatro CSVs que acompanham o projeto |
| POST | `/admin/sinteticos` `{ "quantidade": 5000, "semente": 42 }` | gera o cadastro fictício (substitui o anterior) |
| GET | `/pessoas?setor=` · `/pessoas/{id}` | consulta as pessoas fictícias |
| POST | `/simulacoes/niveis` `{ "icMaxPorDia": [35.4, 35.8, 35.2], "icMaxDiasAnteriores": [], "dataInicio": "2026-12-01" }` | gêmeo digital: nível de calor (NC1–NC5) de cada dia do cenário pelos limiares ativos de `TMC_LIMIAR_NIVEL_CALOR` — mesmas regras do modo real (1 dia acima do limiar ou 3 dias seguidos); 400 se não houver limiares ativos |
| GET | `/simulacoes/modelos?acrescimos=1,3&duracaoMinima=3` | gêmeo digital: ondas de calor reais da A707 (episódios de dias seguidos em NC4/NC5 em `TMC_RESUMO_DIARIO_CLIMA`, com 2 dias de entrada e 2 de saída, até 15 dias) e as mesmas ondas com acréscimo no IC; dias acima do máximo histórico vêm com `extrapolacao: true`. Cache de 1 h |
| POST | `/simulacoes` `{ "icMaxPorDia": [...], "agentesPorUbs": 2, ... }` | cria e roda um cenário de 1 a 15 dias: grava em `TMC_SIMULACAO` / `TMC_SIMULACAO_DIA` (nunca nas tabelas reais) e devolve 201 com o id e `Location`. Opcionais: `nome`, `modeloId`, `dataInicio`, `refugios` (`TODOS`, `PUBLICOS`, `NENHUM`), `contatosPorAgenteDia` (15), `fatorK` (0,10 a 0,50, padrão 0,25 — estimativa), `distrito` |
| GET | `/simulacoes?page=0&size=20` | simulações já feitas, das mais recentes para as mais antigas |
| GET | `/simulacoes/{id}?dia=` | cenário, nível de cada dia (com extrapolação) e resultado por setor; 404 se não existir. Os totais da busca ativa e os setores são preenchidos pelo motor dia a dia (PBI 107) |

Exemplos prontos em [`docs/requests.http`](docs/requests.http). Erros sempre no formato
`{ status, erro, mensagem, detalhes[], timestamp }`.

## Regras importantes

- **Refúgio "confirmado"** = revisão `manter` (equipamento público). Com `incluirPotenciais=true` entram também os
  `potencial` (templos, shoppings). `revisar` e `remover` nunca são sugeridos.
- **Distâncias** em linha reta (haversine). Distância a pé real fica para uma próxima versão.
- **Setores suprimidos pelo IBGE** (21) e **sem registro** (9) ficam sem população e não recebem pessoas sintéticas.
- **Cadastro sintético:** todos os registros têm `sintetico=true`, telefones `+55 18 0000-XXXX` (não discáveis) e
  consentimento LGPD simulado. As taxas de "mora sozinho", "mobilidade reduzida" e "condição crônica" em
  `application.properties` são **PROVISÓRIAS** — trocar por fonte oficial antes de apresentar números.
  O Censo por setor não tem faixa 80+, então a idade dentro de 70+ é sorteada com peso decrescente (70–95).

- **Calor por setor (tabela `TMC_CALOR_SETOR`):** a `classe` (1 = 20% mais frescos, 5 = 20% mais quentes) é o dado a usar.
  `temperaturaSuperficieC` é a temperatura do **chão e dos telhados** às 10h20 de 08/01/2024, não do ar; os níveis
  NC1–NC5 continuam vindo da estação do INMET. `temperaturaArEstimadaC` vem do modelo da Unesp e ficou ~9 °C acima
  das medições de campo, então serve só para comparar setores. `percentualConstrucoes` e `percentualVegetacaoDensa`
  são médias de janelas de 990 m (variáveis C1 e C3 do modelo), não a área exata dentro do setor.
  Setor sem pixel válido (nuvem) fica com status `SEM_DADO`; nesta cena não houve nenhum.
- Se o banco já tiver setores mas a tabela de calor estiver vazia, a aplicação importa só o `calor.csv` ao subir.

## Fontes dos dados

- IBGE — Censo Demográfico 2022, malha de setores e agregados por setor (município 3541406)
- Fundação Seade — IPVS 2022
- Ministério da Saúde — API de dados abertos do CNES (UBS)
- OpenStreetMap (Overpass API) — possíveis refúgios, revisados manualmente
- USGS/NASA — Landsat 8 Collection 2 Level-2, cena LC08_L2SP_222075_20240108_20240122_02_T1 (temperatura de superfície)
- Amorim, Dubreuil e Cardoso (2015), Revista Brasileira de Climatologia v.16 — modelo de temperatura do ar da Unesp

## Atualizar os dados

Os CSVs em `src/main/resources/dados` saem do GeoPackage `prudente_setores_final.gpkg` (QGIS). Separador `;`,
UTF-8, geometria em WKT (EPSG:4674). Ordem de importação: setores → ipvs → servicos → calor.
O `calor.csv` sai de `landsat/setores_calor.gpkg` (feature Mapa de Calor por Setor).

## CORS (chamadas do painel pelo navegador)

O painel do gêmeo digital roda em outro domínio, então a API só aceita chamadas do navegador vindas das origens
em `CORS_ORIGENS` (separadas por vírgula). Padrão: `http://localhost:5173` (o `npm run dev` do painel).
No Azure, configure `CORS_ORIGENS` nas variáveis de ambiente do App Service com a URL onde o painel for publicado.
