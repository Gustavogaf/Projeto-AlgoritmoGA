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


    // =================================================================================
    // CLASSES E ENUMS ANINHADOS
    // =================================================================================

    /**
     * [cite_start]Enum para representar cada item único possível em uma transação. [cite: 5]
     * Usar um Enum torna o código mais seguro, legível e eficiente do que usar Strings soltas.
     */
    public enum Item {
        LEITE, PAO, MANTEIGA, CAFE, SUCO, BOLO
    }

    /**
     * [cite_start]Representa um indivíduo da população, ou seja, uma regra de associação X -> Y. [cite: 1]
     * Cada regra é codificada como um cromossomo e possui valores de suporte, confiança e fitness.
     */
    public static class Regra {
        // O cromossomo que representa a regra.
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
            } while (!eRegraValida(genesTemporarios)); // Usa um método estático para validar o array temporário.

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
         * [cite_start]Verifica se a regra atende às três condições de validade. [cite: 54]
         * @return true se a regra for válida, false caso contrário.
         */
        private boolean eRegraValida(int[] cromossomo) {
            boolean temAntecedente = false;
            boolean temConsequente = false;
            for (int gene : cromossomo) {
                if (gene == 1) {
                    temAntecedente = true;
                }
                if (gene == 2) {
                    temConsequente = true;
                }
            }
            // A regra deve ter tanto antecedente quanto consequente.
            // A intersecção entre X e Y deve ser vazia (já garantido pela codificação 1 e 2).
            return temAntecedente && temConsequente;
        }

        /**
         * Extrai o conjunto de itens do antecedente (X) a partir do cromossomo.
         * @return Um Set<Item> contendo os itens do antecedente.
         */
        public Set<Item> getAntecedenteX() {
            Set<Item> antecedente = new HashSet<>();
            for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                if (cromossomo[i] == 1) {
                    antecedente.add(UNIVERSO_ITENS.get(i));
                }
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
                if (cromossomo[i] == 2) {
                    consequente.add(UNIVERSO_ITENS.get(i));
                }
            }
            return consequente;
        }

        @Override
        public String toString() {
            return String.format("Regra: %s -> %s (Fitness: %.4f)",
                getAntecedenteX(), getConsequenteY(), this.fitness);
        }

        // Getters e Setters para os atributos de avaliação.
        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
        public void setSuporte(double suporte) { this.suporte = suporte; }
        public void setConfianca(double confianca) { this.confianca = confianca; }
    }


    // =================================================================================
    // PONTO DE ENTRADA DA APLICAÇÃO
    // =================================================================================

    public static void main(String[] args) {
        System.out.println("Estrutura inicial do Algoritmo Genético criada.");
        System.out.println("Universo de Itens: " + UNIVERSO_ITENS);
        System.out.println("Tamanho do Cromossomo: " + TAMANHO_CROMOSSOMO);

        System.out.println("\nGerando 5 regras aleatórias válidas para teste:");
        for (int i = 0; i < 5; i++) {
            Regra teste = new Regra();
            System.out.println(teste.getAntecedenteX() + " -> " + teste.getConsequenteY());
        }
    }
}