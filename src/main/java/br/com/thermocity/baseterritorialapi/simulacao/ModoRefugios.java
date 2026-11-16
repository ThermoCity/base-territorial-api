package br.com.thermocity.baseterritorialapi.simulacao;

/** Quais refúgios a simulação considera para a distância de 800 m. */
public enum ModoRefugios {
    /** Públicos confirmados e potenciais. */
    TODOS,
    /** Só os públicos confirmados (revisão "manter"). */
    PUBLICOS,
    /** Nenhum refúgio: todos os idosos da lista ficam sem refúgio próximo. */
    NENHUM
}
