package br.com.thermocity.baseterritorialapi.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor simples de CSV separado por ponto e vírgula, com suporte a campos entre aspas
 * (necessário porque a geometria em WKT contém vírgulas).
 */
public final class CsvLeitor {

    public static final char SEPARADOR = ';';

    /** Uma linha do arquivo: número da linha (contando o cabeçalho como 1) e os valores por coluna. */
    public record Linha(int numero, Map<String, String> valores) {
        /** Valor da coluna, sem espaços nas pontas; vazio vira null. */
        public String valor(String coluna) {
            String v = valores.get(coluna);
            if (v == null) return null;
            v = v.strip();
            return v.isEmpty() ? null : v;
        }
    }

    private CsvLeitor() {
    }

    public static List<Linha> ler(InputStream entrada, List<String> colunasObrigatorias) throws IOException {
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linhaCabecalho = leitor.readLine();
            if (linhaCabecalho == null) {
                throw new IOException("Arquivo vazio");
            }
            if (linhaCabecalho.startsWith("﻿")) {
                linhaCabecalho = linhaCabecalho.substring(1);
            }
            List<String> cabecalho = dividir(linhaCabecalho).stream().map(String::strip).toList();
            List<String> faltando = colunasObrigatorias.stream().filter(c -> !cabecalho.contains(c)).toList();
            if (!faltando.isEmpty()) {
                throw new IOException("Colunas obrigatórias ausentes no cabeçalho: " + String.join(", ", faltando));
            }
            List<Linha> linhas = new ArrayList<>();
            String texto;
            int numero = 1;
            while ((texto = leitor.readLine()) != null) {
                numero++;
                if (texto.isBlank()) continue;
                List<String> campos = dividir(texto);
                Map<String, String> valores = new HashMap<>();
                for (int i = 0; i < cabecalho.size(); i++) {
                    valores.put(cabecalho.get(i), i < campos.size() ? campos.get(i) : null);
                }
                linhas.add(new Linha(numero, valores));
            }
            return linhas;
        }
    }

    static List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == SEPARADOR) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}
