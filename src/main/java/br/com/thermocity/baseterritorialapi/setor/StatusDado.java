package br.com.thermocity.baseterritorialapi.setor;

/**
 * Situação de um dado vindo de fonte oficial.
 * OK: valor disponível. SUPRIMIDO: omitido pelo IBGE por sigilo.
 * SEM_REGISTRO: setor ausente na tabela da fonte. SEM_DADO: fonte não classificou o setor.
 * Em nenhum caso o valor ausente é tratado como zero.
 */
public enum StatusDado {
    OK, SUPRIMIDO, SEM_REGISTRO, SEM_DADO;

    public static StatusDado deTexto(String texto) {
        if (texto == null) return SEM_REGISTRO;
        return switch (texto.strip().toLowerCase()) {
            case "ok" -> OK;
            case "suprimido" -> SUPRIMIDO;
            case "sem_dado" -> SEM_DADO;
            default -> SEM_REGISTRO;
        };
    }
}
