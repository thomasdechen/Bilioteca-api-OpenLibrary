package biblioteca.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormatacaoDatas {
    private static final DateTimeFormatter FORMATO_BRASILEIRO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Formata a data para o padrão brasileiro.
     * Caso o livro tenha apenas o ano de publicação, exibe somente o ano na interface.
     * Obs: No banco de dados, o padrão será sempre o ISO.
     */

    public static String formatarParaExibicao(LocalDate data) {
        if (data == null) {
            return "";
        }

        if (data.getMonthValue() == 1 && data.getDayOfMonth() == 1) {
            return String.valueOf(data.getYear());
        }

        return data.format(FORMATO_BRASILEIRO);
    }

    /**
     * Analisa uma string de data que pode estar no formato brasileiro, formato ISO(aaaa-mm-dd), ou apenas o ano
     */
    public static LocalDate analisarEntradaUsuario(String textoData) {
        if (textoData == null || textoData.trim().isEmpty()) {
            return null;
        }

        textoData = textoData.trim();

        // Se for apenas um ano (4 dígitos)
        if (textoData.matches("\\d{4}")) {
            int ano = Integer.parseInt(textoData);
            return LocalDate.of(ano, 1, 1);
        }

        // Se estiver no formato brasileiro (dd/MM/yyyy)
        if (textoData.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            return LocalDate.parse(textoData, FORMATO_BRASILEIRO);
        }

        // Se estiver no formato ISO (yyyy-MM-dd)
        if (textoData.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(textoData);
        }

        throw new IllegalArgumentException("Formato de data inválido. Use DD/MM/AAAA, AAAA/MM/DD ou AAAA.");
    }
}
