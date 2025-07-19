package com.example;

import java.util.*;
import java.util.stream.Collectors;

public class Main {

    // =================================================================================
    // PARÂMETROS GLOBAIS E CONSTANTES DO ALGORITMO GENÉTICO
    // =================================================================================
    // O universo de todos os itens possíveis. A ordem aqui define a posição no cromossomo.
    private static final List<Item> UNIVERSO_ITENS = List.of(Item.values());
    // O número total de itens únicos, que define o tamanho do nosso cromossomo.
    private static final int TAMANHO_CROMOSSOMO = UNIVERSO_ITENS.size();
    // Gerador de números aleatórios para ser usado em todo o algoritmo.
    private static final Random random = new Random();

    // Parâmetros de execução do AG, conforme especificado no documento do trabalho.
    private static final int TAMANHO_POPULACAO = 100;
    private static final int NUMERO_GERACOES = 100;
    private static final int TAMANHO_TORNEIO = 3;
    private static final double TAXA_DE_CROSSOVER = 0.8; // 80% de chance de aplicar o crossover.
    private static final double TAXA_DE_MUTACAO = 0.05;  // 5% de chance para cada gene individualmente sofrer mutação.
    private static final int NUMERO_ELITES = 1; // Número de melhores indivíduos a serem passados diretamente para a próxima geração.

    // Pesos para a função de fitness, conforme a fórmula: fitness = 0.5 * suporte + 0.5 * confianca
    private static final double PESO_SUPORTE = 0.5;
    private static final double PESO_CONFIANCA = 0.5;


    // =================================================================================
    // CLASSES E ENUMS ANINHADOS (ESTRUTURAS DE DADOS)
    // =================================================================================
    public enum Item {
        LEITE, PAO, MANTEIGA, CAFE, SUCO, BOLO
    }

    public static class Regra implements Comparable<Regra> {
        private final int[] cromossomo;
        private double suporte;
        private double confianca;
        private double fitness;

        public Regra() {
            int[] genesTemporarios;
            do {
                genesTemporarios = new int[TAMANHO_CROMOSSOMO];
                for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                    genesTemporarios[i] = random.nextInt(3);
                }
            } while (!eRegraValida(genesTemporarios));
            this.cromossomo = genesTemporarios;
        }

        public Regra(int[] cromossomo) {
            this.cromossomo = cromossomo;
        }

        public static boolean eRegraValida(int[] cromossomo) {
            boolean temAntecedente = false;
            boolean temConsequente = false;
            for (int gene : cromossomo) {
                if (gene == 1) temAntecedente = true;
                if (gene == 2) temConsequente = true;
            }
            return temAntecedente && temConsequente;
        }

        public Set<Item> getAntecedenteX() {
            Set<Item> antecedente = new HashSet<>();
            for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                if (cromossomo[i] == 1) antecedente.add(UNIVERSO_ITENS.get(i));
            }
            return antecedente;
        }

        public Set<Item> getConsequenteY() {
            Set<Item> consequente = new HashSet<>();
            for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
                if (cromossomo[i] == 2) consequente.add(UNIVERSO_ITENS.get(i));
            }
            return consequente;
        }

        @Override
        public String toString() {
            return String.format("Regra: %s -> %s (Fitness: %.4f, Suporte: %.4f, Confianca: %.4f)",
                getAntecedenteX(), getConsequenteY(), this.fitness, this.suporte, this.confianca);
        }

        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
        public double getSuporte() { return suporte; }
        public void setSuporte(double suporte) { this.suporte = suporte; }
        public double getConfianca() { return confianca; }
        public void setConfianca(double confianca) { this.confianca = confianca; }
        public int[] getCromossomo() { return Arrays.copyOf(cromossomo, cromossomo.length); }

        @Override
        public int compareTo(Regra outra) {
            return Double.compare(outra.fitness, this.fitness);
        }
    }


    // =================================================================================
    // LÓGICA DE AVALIAÇÃO (FUNÇÃO DE FITNESS)
    // =================================================================================
    public static void avaliarPopulacao(List<Regra> populacao, List<Set<Item>> transacoes) {
        for (Regra regra : populacao) {
            double suporte = calcularSuporte(regra, transacoes);
            double confianca = calcularConfianca(regra, transacoes);
            double fitness = (PESO_SUPORTE * suporte) + (PESO_CONFIANCA * confianca);
            regra.setSuporte(suporte);
            regra.setConfianca(confianca);
            regra.setFitness(fitness);
        }
    }

    private static double calcularSuporte(Regra regra, List<Set<Item>> transacoes) {
        Set<Item> itensDaRegra = new HashSet<>(regra.getAntecedenteX());
        itensDaRegra.addAll(regra.getConsequenteY());
        long contadorSuporte = transacoes.stream().filter(t -> t.containsAll(itensDaRegra)).count();
        return (double) contadorSuporte / transacoes.size();
    }

    private static double calcularConfianca(Regra regra, List<Set<Item>> transacoes) {
        Set<Item> antecedenteX = regra.getAntecedenteX();
        Set<Item> consequenteY = regra.getConsequenteY();
        long contadorSuporteX = transacoes.stream().filter(t -> t.containsAll(antecedenteX)).count();
        if (contadorSuporteX == 0) return 0.0;
        long contadorSuporteXY = transacoes.stream()
            .filter(t -> t.containsAll(antecedenteX) && t.containsAll(consequenteY))
            .count();
        return (double) contadorSuporteXY / contadorSuporteX;
    }


    // =================================================================================
    // OPERADORES GENÉTICOS
    // =================================================================================
    public static List<Regra> inicializarPopulacao() {
        List<Regra> populacao = new ArrayList<>();
        for (int i = 0; i < TAMANHO_POPULACAO; i++) {
            populacao.add(new Regra());
        }
        return populacao;
    }

    private static Regra selecaoPorTorneio(List<Regra> populacao) {
        Regra melhorDoTorneio = null;
        for (int i = 0; i < TAMANHO_TORNEIO; i++) {
            Regra competidor = populacao.get(random.nextInt(populacao.size()));
            if (melhorDoTorneio == null || competidor.getFitness() > melhorDoTorneio.getFitness()) {
                melhorDoTorneio = competidor;
            }
        }
        return melhorDoTorneio;
    }

    private static List<Regra> crossover(Regra pai1, Regra pai2) {
        int[] cromossomoPai1 = pai1.getCromossomo();
        int[] cromossomoPai2 = pai2.getCromossomo();
        int[] cromossomoFilho1 = new int[TAMANHO_CROMOSSOMO];
        int[] cromossomoFilho2 = new int[TAMANHO_CROMOSSOMO];
        int pontoDeCorte = random.nextInt(TAMANHO_CROMOSSOMO - 1) + 1;
        System.arraycopy(cromossomoPai1, 0, cromossomoFilho1, 0, pontoDeCorte);
        System.arraycopy(cromossomoPai2, pontoDeCorte, cromossomoFilho1, pontoDeCorte, TAMANHO_CROMOSSOMO - pontoDeCorte);
        System.arraycopy(cromossomoPai2, 0, cromossomoFilho2, 0, pontoDeCorte);
        System.arraycopy(cromossomoPai1, pontoDeCorte, cromossomoFilho2, pontoDeCorte, TAMANHO_CROMOSSOMO - pontoDeCorte);
        return List.of(new Regra(cromossomoFilho1), new Regra(cromossomoFilho2));
    }

    private static void mutacao(Regra regra) {
        int[] cromossomo = regra.getCromossomo();
        for (int i = 0; i < TAMANHO_CROMOSSOMO; i++) {
            if (random.nextDouble() < TAXA_DE_MUTACAO) {
                cromossomo[i] = random.nextInt(3);
            }
        }
    }


    // =================================================================================
    // PONTO DE ENTRADA E EXECUÇÃO PRINCIPAL DO ALGORITMO
    // =================================================================================
    public static void main(String[] args) {
        System.out.println(">>> INICIANDO EXECUÇÃO DO ALGORITMO GENÉTICO <<<");

        // ETAPA 1: Carregar o conjunto de dados.
        List<Set<Item>> transacoes = List.of(
            Set.of(Item.LEITE, Item.PAO, Item.MANTEIGA), Set.of(Item.PAO, Item.MANTEIGA),
            Set.of(Item.LEITE, Item.CAFE), Set.of(Item.PAO, Item.SUCO, Item.MANTEIGA),
            Set.of(Item.CAFE, Item.BOLO), Set.of(Item.LEITE, Item.PAO, Item.CAFE),
            Set.of(Item.MANTEIGA, Item.BOLO, Item.CAFE), Set.of(Item.PAO, Item.BOLO),
            Set.of(Item.SUCO, Item.CAFE), Set.of(Item.LEITE, Item.MANTEIGA, Item.BOLO),
            Set.of(Item.PAO, Item.CAFE, Item.SUCO), Set.of(Item.LEITE, Item.SUCO),
            Set.of(Item.PAO, Item.MANTEIGA, Item.CAFE), Set.of(Item.BOLO, Item.SUCO),
            Set.of(Item.LEITE, Item.PAO, Item.MANTEIGA, Item.CAFE)
        );

        // Estruturas para armazenar os dados para os gráficos
        List<Double> melhoresFitnessPorGeracao = new ArrayList<>();
        List<Double> mediaFitnessPorGeracao = new ArrayList<>();
        List<Double> suporteMelhorIndividuo = new ArrayList<>();
        List<Double> confiancaMelhorIndividuo = new ArrayList<>();
        
        // ETAPA 2: Criar a população inicial.
        System.out.println("\n[Geração 0] Inicializando população de " + TAMANHO_POPULACAO + " regras...");
        List<Regra> populacao = inicializarPopulacao();
        avaliarPopulacao(populacao, transacoes);

        // ETAPA 3: Loop de Evolução (ciclo principal do AG).
        for (int g = 1; g <= NUMERO_GERACOES; g++) {
            List<Regra> novaPopulacao = new ArrayList<>();
            
            // **IMPLEMENTAÇÃO DO ELITISMO**
            // Ordena a população atual para encontrar o melhor indivíduo.
            Collections.sort(populacao);
            // Adiciona o(s) melhor(es) indivíduo(s) diretamente à nova população.
            for (int i = 0; i < NUMERO_ELITES; i++) {
                novaPopulacao.add(new Regra(populacao.get(i).getCromossomo()));
            }

            // Repete até que a nova população atinja o tamanho desejado.
            while (novaPopulacao.size() < TAMANHO_POPULACAO) {
                Regra pai1 = selecaoPorTorneio(populacao);
                Regra pai2 = selecaoPorTorneio(populacao);

                List<Regra> filhos;
                if (random.nextDouble() < TAXA_DE_CROSSOVER) {
                    filhos = crossover(pai1, pai2);
                } else {
                    filhos = List.of(new Regra(pai1.getCromossomo()), new Regra(pai2.getCromossomo()));
                }
                
                for(Regra filho : filhos) {
                    mutacao(filho);
                    if (Regra.eRegraValida(filho.getCromossomo())) {
                        novaPopulacao.add(filho);
                        if(novaPopulacao.size() == TAMANHO_POPULACAO) break;
                    }
                }
            }
            
            populacao = novaPopulacao;
            avaliarPopulacao(populacao, transacoes);

            // Coleta de dados da geração atual para os gráficos.
            Collections.sort(populacao);
            Regra melhorDaGeracao = populacao.get(0);
            double mediaFitness = populacao.stream().mapToDouble(Regra::getFitness).average().orElse(0.0);
            
            melhoresFitnessPorGeracao.add(melhorDaGeracao.getFitness());
            mediaFitnessPorGeracao.add(mediaFitness);
            suporteMelhorIndividuo.add(melhorDaGeracao.getSuporte());
            confiancaMelhorIndividuo.add(melhorDaGeracao.getConfianca());

            if (g % 10 == 0 || g == 1) { 
                System.out.printf("[Geração %3d] Melhor Fitness: %.4f | Média Fitness: %.4f | Melhor Regra: %s%n", 
                    g, melhorDaGeracao.getFitness(), mediaFitness, melhorDaGeracao);
            }
        }

        // ETAPA 4: Apresentar os resultados finais.
        System.out.println("\n>>> EXECUÇÃO FINALIZADA <<<");
        Collections.sort(populacao);
        Regra melhorRegraEncontrada = populacao.get(0);
        
        System.out.println("\nMelhor regra encontrada após " + NUMERO_GERACOES + " gerações:");
        System.out.println(melhorRegraEncontrada);
        
        System.out.println("\n--- DADOS PARA GRÁFICOS ---");
        System.out.println("\n// Figura 1: Evolução do melhor e da média de fitness por geração.");
        System.out.println("dados_melhor_fitness = " + melhoresFitnessPorGeracao);
        System.out.println("dados_media_fitness = " + mediaFitnessPorGeracao);
        
        System.out.println("\n// Figura 2: Evolução de suporte e confiança do melhor indivíduo.");
        System.out.println("dados_suporte_melhor = " + suporteMelhorIndividuo);
        System.out.println("dados_confianca_melhor = " + confiancaMelhorIndividuo);

        System.out.println("\n// Figura 3: Histograma de distribuição de fitness na população final.");
        List<Double> fitnessPopulacaoFinal = populacao.stream().map(Regra::getFitness).collect(Collectors.toList());
        System.out.println("dados_fitness_populacao_final = " + fitnessPopulacaoFinal);
    }
}