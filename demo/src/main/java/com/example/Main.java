package com.example;

import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // =================================================================================
    // PARÂMETROS DO ALGORITMO GENÉTICO
    // =================================================================================
    // O universo de todos os itens possíveis. A ordem aqui define a posição no cromossomo.
    private static final List<Item> UNIVERSO_ITENS = List.of(Item.values());
    // O número total de itens únicos, que define o tamanho do nosso cromossomo.
    private static final int TAMANHO_CROMOSSOMO = UNIVERSO_ITENS.size();
    // Gerador de números aleatórios para ser usado em todo o algoritmo.
    private static final Random random = new Random();

    // Pesos para a função de fitness, conforme especificado no trabalho.
    private static final double PESO_SUPORTE = 0.5;
    private static final double PESO_CONFIANCA = 0.5;


    // =================================================================================
    // CLASSES E ENUMS ANINHADOS
    // =================================================================================

    /**
     * Enum para representar cada item único possível em uma transação.
     * Usar um Enum torna o código mais seguro, legível e eficiente do que usar Strings soltas.
     */
    public enum Item {
        LEITE, PAO, MANTEIGA, CAFE, SUCO, BOLO
    }

    /**
     * Representa um indivíduo da população, ou seja, uma regra de associação X -> Y.
     * Cada regra é codificada como um cromossomo e possui valores de suporte, confiança e fitness.
     */
    public static class Regra {
        // O cromossomo que representa a regra, declarado como 'final' para garantir imutabilidade.
        // 0: item não participa.
        // 1: item pertence ao antecedente (X).
        // 2: item pertence ao consequente (Y).
        private final int[] cromossomo;
        private double suporte;
        private double confianca;
        private double fitness;

        /**
         * Construtor que gera uma Regra (cromossomo) aleatória e VÁLIDA.
         */
        public Regra() {
            int[] genesTemporarios; // Cria um array temporário não-final.
            do {
                genesTemporarios = new int[TAMANHO_CROMOSSOMO]; // Instancia o array temporário dentro do loop.
                for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                    // Preenche cada gene com 0, 1 ou 2 aleatoriamente.
                    genesTemporarios[i] = random.nextInt(3);
                }
            } while (!eRegraValida(genesTemporarios)); // Usa um método para validar o array temporário.

            // Após o loop, temos certeza que 'genesTemporarios' é válido.
            // Agora, e somente agora, atribuímos o array válido à nossa variável final.
            this.cromossomo = genesTemporarios;
        }

        /**
         * Construtor para criar uma Regra com um cromossomo específico (usado no crossover).
         * @param cromossomo O vetor de genes do filho.
         */
        public Regra(int[] cromossomo) {
            this.cromossomo = cromossomo;
        }

        /**
         * Verifica se um determinado cromossomo atende às três condições de validade.
         * @param cromossomo O array de genes a ser validado.
         * @return true se a regra for válida, false caso contrário.
         */
        private boolean eRegraValida(int[] cromossomo) {
            boolean temAntecedente = false;
            boolean temConsequente = false;
            for (int gene : cromossomo) {
                if (gene == 1) temAntecedente = true;
                if (gene == 2) temConsequente = true;
            }
            // A regra deve ter tanto antecedente quanto consequente.
            // A intersecção entre X e Y já é garantida pela codificação (um gene não pode ser 1 e 2 ao mesmo tempo).
            return temAntecedente && temConsequente;
        }

        /**
         * Extrai o conjunto de itens do antecedente (X) a partir do cromossomo.
         * @return Um Set<Item> contendo os itens do antecedente.
         */
        public Set<Item> getAntecedenteX() {
            Set<Item> antecedente = new HashSet<>();
            for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                if (cromossomo[i] == 1) antecedente.add(UNIVERSO_ITENS.get(i));
            }
            return antecedente;
        }

        /**
         * Extrai o conjunto de itens do consequente (Y) a partir do cromossomo.
         * @return Um Set<Item> contendo os itens do consequente.
         */
        public Set<Item> getConsequenteY() {
            Set<Item> consequente = new HashSet<>();
            for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                if (cromossomo[i] == 2) consequente.add(UNIVERSO_ITENS.get(i));
            }
            return consequente;
        }
        
        @Override
        public String toString() {
            // Cria uma representação em String do vetor do cromossomo para fácil visualização.
            StringJoiner sj = new StringJoiner(", ");
            for (int gene : cromossomo) {
                sj.add(String.valueOf(gene));
            }
            // Retorna uma string formatada com todas as informações da regra.
            return String.format("Regra: %s -> %s (Fitness: %.4f, Suporte: %.4f, Confianca: %.4f) | Cromossomo: [%s]",
                getAntecedenteX(), getConsequenteY(), this.fitness, this.suporte, this.confianca, sj.toString());
        }

        // Getters e Setters para os atributos de avaliação.
        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
        public void setSuporte(double suporte) { this.suporte = suporte; }
        public void setConfianca(double confianca) { this.confianca = confianca; }
    }


    // =================================================================================
    // LÓGICA DE AVALIAÇÃO (FITNESS)
    // =================================================================================

    /**
     * Avalia uma única regra, calculando seu suporte, confiança e fitness final.
     * Este método orquestra a avaliação completa de um indivíduo.
     * @param regra A regra a ser avaliada.
     * @param transacoes O conjunto de dados de todas as transações.
     */
    public static void avaliarRegra(Regra regra, List<Set<Item>> transacoes) {
        // 1. Calcula o suporte da regra.
        double suporte = calcularSuporte(regra, transacoes);
        regra.setSuporte(suporte);

        // 2. Calcula a confiança da regra.
        double confianca = calcularConfianca(regra, transacoes);
        regra.setConfianca(confianca);

        // 3. Calcula o fitness usando a combinação linear ponderada.
        double fitness = (PESO_SUPORTE * suporte) + (PESO_CONFIANCA * confianca);
        regra.setFitness(fitness);
    }

    /**
     * Calcula o suporte de uma regra.
     * Suporte é a proporção de transações que contêm TODOS os itens da regra (X e Y).
     * @param regra A regra a ser avaliada.
     * @param transacoes O conjunto de dados de todas as transações.
     * @return O valor do suporte (entre 0 e 1).
     */
    private static double calcularSuporte(Regra regra, List<Set<Item>> transacoes) {
        // Combina os itens do antecedente (X) e consequente (Y) em um único conjunto.
        Set<Item> itensDaRegra = new HashSet<>();
        itensDaRegra.addAll(regra.getAntecedenteX());
        itensDaRegra.addAll(regra.getConsequenteY());

        int contadorSuporte = 0;
        // Itera sobre cada transação para verificar se ela "suporta" a regra.
        for (Set<Item> transacao : transacoes) {
            // Se a transação contém todos os itens da regra, incrementa o contador.
            if (transacao.containsAll(itensDaRegra)) {
                contadorSuporte++;
            }
        }
        // Retorna a proporção: (transações que suportam a regra) / (total de transações).
        return (double) contadorSuporte / transacoes.size();
    }

    /**
     * Calcula a confiança de uma regra.
     * Confiança é a proporção de transações que contêm Y, DADO que elas já contêm X.
     * @param regra A regra a ser avaliada.
     * @param transacoes O conjunto de dados de todas as transações.
     * @return O valor da confiança (entre 0 e 1).
     */
    private static double calcularConfianca(Regra regra, List<Set<Item>> transacoes) {
        Set<Item> antecedenteX = regra.getAntecedenteX();
        Set<Item> consequenteY = regra.getConsequenteY();

        int contadorSuporteX = 0; // Conta transações que contêm o antecedente X.
        int contadorSuporteXY = 0; // Conta transações que contêm AMBOS, X e Y.

        // Itera sobre cada transação do conjunto de dados.
        for (Set<Item> transacao : transacoes) {
            // Primeiro, verifica se a transação contém o antecedente.
            if (transacao.containsAll(antecedenteX)) {
                contadorSuporteX++;
                // Se contém o antecedente, agora verifica se também contém o consequente.
                if (transacao.containsAll(consequenteY)) {
                    contadorSuporteXY++;
                }
            }
        }
        // Se nenhuma transação contém o antecedente, a confiança é 0 para evitar divisão por zero.
        if (contadorSuporteX == 0) {
            return 0.0;
        }
        // Retorna a proporção: (transações com X e Y) / (transações com X).
        return (double) contadorSuporteXY / contadorSuporteX;
    }


    // =================================================================================
    // PONTO DE ENTRADA DA APLICAÇÃO
    // =================================================================================

    public static void main(String[] args) {
        // ETAPA 1: DEFINIR O CONJUNTO DE DADOS
        // Estas são as 15 transações fornecidas no documento do trabalho.
        // Usamos um Set para cada transação para evitar duplicatas e facilitar as operações de conjunto.
        List<Set<Item>> transacoes = List.of(
            Set.of(Item.LEITE, Item.PAO, Item.MANTEIGA),
            Set.of(Item.PAO, Item.MANTEIGA),
            Set.of(Item.LEITE, Item.CAFE),
            Set.of(Item.PAO, Item.SUCO, Item.MANTEIGA),
            Set.of(Item.CAFE, Item.BOLO),
            Set.of(Item.LEITE, Item.PAO, Item.CAFE),
            Set.of(Item.MANTEIGA, Item.BOLO, Item.CAFE),
            Set.of(Item.PAO, Item.BOLO),
            Set.of(Item.SUCO, Item.CAFE),
            Set.of(Item.LEITE, Item.MANTEIGA, Item.BOLO),
            Set.of(Item.PAO, Item.CAFE, Item.SUCO),
            Set.of(Item.LEITE, Item.SUCO),
            Set.of(Item.PAO, Item.MANTEIGA, Item.CAFE),
            Set.of(Item.BOLO, Item.SUCO),
            Set.of(Item.LEITE, Item.PAO, Item.MANTEIGA, Item.CAFE)
        );

        System.out.println(">>> INICIANDO TESTE DA FUNÇÃO DE AVALIAÇÃO <<<");
        System.out.println("Total de Transações: " + transacoes.size());

        // ETAPA 2: TESTAR A AVALIAÇÃO COM UMA REGRA FIXA PARA VALIDAÇÃO
        // Criamos uma regra conhecida, {PÃO} -> {MANTEIGA}, para verificar se os cálculos estão corretos.
        // O cromossomo correspondente é [LEITE, PAO, MANTEIGA, CAFE, SUCO, BOLO] -> [0, 1, 2, 0, 0, 0].
        System.out.println("\n--- Teste 1: Regra Fixa {PÃO} -> {MANTEIGA} ---");
        Regra regraFixa = new Regra(new int[]{0, 1, 2, 0, 0, 0});
        avaliarRegra(regraFixa, transacoes); // Chama a função de avaliação.
        System.out.println(regraFixa); // Imprime o resultado completo da regra.

        // ETAPA 3: TESTAR A AVALIAÇÃO COM UMA REGRA ALEATÓRIA
        // Isso verifica se a criação aleatória e a avaliação funcionam em conjunto.
        System.out.println("\n--- Teste 2: Regra Aleatória Válida ---");
        Regra regraAleatoria = new Regra(); // Cria uma regra aleatória válida.
        avaliarRegra(regraAleatoria, transacoes); // Avalia a regra criada.
        System.out.println(regraAleatoria); // Imprime o resultado.
    }
}