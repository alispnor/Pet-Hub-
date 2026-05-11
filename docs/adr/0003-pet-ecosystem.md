# ADR 0003 — Pet Hub como parte do "Ali's Pet Ecosystem"

**Status:** Aceita
**Data:** 2026-05-11
**Decisão por:** Ali

## Contexto

Pet Hub não é um projeto isolado. Ele é o segundo aplicativo de um ecossistema que já tem o **Pet Diary** — prontuário digital de pets (vacinas, consultas, comportamento, crescimento, histórico médico), já existente.

A pergunta arquitetural é: como Pet Hub e Pet Diary se relacionam?

Três cenários possíveis:

1. **Apps totalmente independentes.** Sem integração. Cada um tem seus próprios usuários, dados de pet, autenticação.
2. **Apps integrados via API REST.** Pet Hub consulta Pet Diary para enriquecer recomendações; Pet Diary consulta Pet Hub para sugerir produtos relevantes.
3. **Apps unificados.** Um login único, banco compartilhado, deploy unificado.

## Decisão

**Cenário 1 agora, evoluir para Cenário 2 quando ambos estiverem maduros.**

- **Pet Hub e Pet Diary continuam como repositórios separados** com bancos de dados separados, autenticação separada, e ciclos de deploy independentes.
- **Cadastro do pet é duplicado:** o cliente cadastra o pet no Pet Hub para recomendações personalizadas, e no Pet Diary para o prontuário. Sem sincronização automática nesta fase.
- **Posicionar como ecossistema desde o início no README e nos materiais de marketing**, mesmo sem integração técnica — porque a narrativa de produto é forte e prepara o terreno para a integração futura.

## Integração futura (não nesta fase)

Quando Pet Hub estiver maduro (após Fase 5 ou 6), avaliar a integração via uma das opções:

### Opção A — REST API com OAuth/OIDC entre apps

Pet Hub pede autorização ao usuário para consultar o Pet Diary (estilo "Sign in with Pet Diary"), e busca o perfil completo do pet (raça, idade, condições médicas, comportamento) para gerar recomendações ultra-personalizadas.

- ✅ Padrão da indústria, bem compreendido.
- ✅ Usuário controla explicitamente o que é compartilhado.
- ❌ Latência de chamada cross-app no checkout/recomendação.
- ❌ Cada app precisa entender o modelo do outro.

### Opção B — Eventos (Kafka cross-cluster)

Pet Diary publica eventos `PetProfileUpdated` num tópico que Pet Hub consome (com permissão do usuário). Pet Hub mantém uma cópia denormalizada do perfil para uso em recomendações.

- ✅ Baixa latência no momento da recomendação (dados já locais).
- ✅ Desacoplamento total — Pet Hub funciona mesmo se Pet Diary estiver offline.
- ❌ Eventual consistency (dados podem estar até alguns segundos atrasados).
- ❌ Complexidade operacional maior (cross-cluster Kafka, schemas compartilhados, ACLs).

A decisão entre A e B fica para um ADR futuro, quando estivermos perto da integração real. Pet Hub é projetado de forma a suportar qualquer das duas opções (perfil de pet local pode ser sobrescrito por dados externos).

## Razões para começar separado

- **Disciplina de boundary.** Forçar separação inicial evita acoplamento acidental que custa caro para desfazer.
- **Foco.** Cada app pode evoluir no seu ritmo sem coordenação cross-team (mesmo sendo dev solo, contextualmente).
- **Narrativa de portfolio.** Ter dois apps relacionados mas independentes mostra capacidade de pensar em produto sistemicamente.
- **Realismo.** Empresas reais começam com apps separados e integram depois. Replicar essa progressão é mais educacional que partir de um sistema já unificado.

## Consequências

- ✅ Pet Hub pode lançar sem depender de Pet Diary estar pronto/disponível.
- ✅ ADR-0002 (arquitetura evolutiva) se aplica também aqui: começar simples, integrar depois.
- ✅ README e marketing já comunicam o ecossistema, criando expectativa positiva.
- ⚠️ Duplicação de cadastro de pet enquanto integração não existe. Aceitável no curto prazo.
- ⚠️ Decisão de Opção A vs B fica em aberto — não bloqueia desenvolvimento atual, mas precisa ser feita antes da integração real.

## Alternativas consideradas

**Unificar imediatamente (Cenário 3)** rejeitado:
- Pet Diary já existe e funciona. Reescrever para unificar é caro e arriscado.
- Acoplamento prematuro entre dois domínios distintos (saúde vs. comércio) é uma má aposta.

**Apenas mencionar a relação no README sem nunca integrar (Cenário 1 permanente)** rejeitado:
- O valor real do ecossistema só aparece com integração. Sem ela, é só marketing.
- Demonstrar a integração no portfolio (Fase 10+, com IA) é um diferencial técnico forte.

## Referências

- Pet Diary: repositório irmão, projeto pessoal anterior do Ali.
- README de Pet Hub: tem bloco "Ecossistema" que comunica essa decisão ao usuário final.
