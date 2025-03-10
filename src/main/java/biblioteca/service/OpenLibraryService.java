package biblioteca.service;

import biblioteca.model.Livro;
import biblioteca.util.FormatacaoDatas;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OpenLibraryService {
    private static final String BASE_URL = "https://openlibrary.org/api/books?bibkeys=ISBN:";
    // Cliente HTTP reutilizável
    private static final Client CLIENT = ClientBuilder.newClient();
    // Executor para processamento assíncrono
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    public static JsonObject buscarInformacoesPorIsbn(String isbn) {
        try {
            Response response = CLIENT.target(BASE_URL + isbn + "&format=json&jscmd=data")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                throw new IOException("Resposta não esperada: " + response.getStatus());
            }

            String jsonData = response.readEntity(String.class);
            JsonObject parsedResponse = JsonParser.parseString(jsonData).getAsJsonObject();

            String isbnKey = "ISBN:" + isbn;
            if (!parsedResponse.has(isbnKey)) {
                throw new IOException("Nenhum livro encontrado para o ISBN: " + isbn);
            }

            return parsedResponse.getAsJsonObject(isbnKey);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar informações do livro", e);
        }
    }

    public static Livro converterParaLivro(JsonObject dadosLivro, String isbn) {
        Livro livro = new Livro();
        livro.setIsbn(isbn);

        if (dadosLivro.has("title")) {
            livro.setTitulo(dadosLivro.get("title").getAsString());
        }

        if (dadosLivro.has("authors") && dadosLivro.get("authors").isJsonArray()) {
            JsonArray autores = dadosLivro.getAsJsonArray("authors");
            StringBuilder autorBuilder = new StringBuilder();
            for (JsonElement autorElement : autores) {
                JsonObject autor = autorElement.getAsJsonObject();
                if (autor.has("name")) {
                    if (autorBuilder.length() > 0) {
                        autorBuilder.append(", ");
                    }
                    autorBuilder.append(autor.get("name").getAsString());
                }
            }
            livro.setAutores(autorBuilder.toString());
        }

        if (dadosLivro.has("publishers") && dadosLivro.get("publishers").isJsonArray()) {
            JsonArray editoras = dadosLivro.getAsJsonArray("publishers");
            if (!editoras.isEmpty()) {
                JsonObject primeiraEditora = editoras.get(0).getAsJsonObject();
                if (primeiraEditora.has("name")) {
                    livro.setEditora(primeiraEditora.get("name").getAsString());
                }
            }
        }

        if (dadosLivro.has("publish_date")) {
            try {
                String dataPublicacao = dadosLivro.get("publish_date").getAsString();
                livro.setDataPublicacao(parseData(dataPublicacao));
            } catch (Exception e) {
                livro.setDataPublicacao(null);
            }
        }

        /**
         * Busca do número de edições para um livro a partir do ISBN.
         *
         * O processo segue a seguinte lógica:
         * 1. Realiza uma consulta à API OpenLibrary com o ISBN do livro para obter os dados básicos (feito anteriormente)
         * 2. Extrai o work_id(works/key) (identificador do livro) da resposta JSON inicial(resposta do endpoint de ISBN)
         * 3. Utiliza o work_id para fazer uma segunda consulta à API de edições:
         *    https://openlibrary.org/works/[work_id]/editions.json
         * 4. Obtém e retorna o número de edições associadas a esta obra("size": [nº de edições])
         */
        CompletableFuture<Integer> edicoesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Consulta para obter work_id(works/key)
                Response response = CLIENT.target("https://openlibrary.org/isbn/" + isbn + ".json")
                        .request(MediaType.APPLICATION_JSON)
                        .get();

                if (response.getStatus() != 200) {
                    return 0;
                }

                String jsonData = response.readEntity(String.class);
                JsonObject isbnData = JsonParser.parseString(jsonData).getAsJsonObject();

                if (!isbnData.has("works") || isbnData.getAsJsonArray("works").size() == 0) {
                    return 0;
                }

                JsonObject work = isbnData.getAsJsonArray("works").get(0).getAsJsonObject();
                if (!work.has("key")) {
                    return 0;
                }

                String workId = work.get("key").getAsString().replace("/works/", "");
                return buscarNumeroEdicoes(workId);
            } catch (Exception e) {
                System.err.println("Erro ao buscar work_id: " + e.getMessage());
                return 0;
            }
        }, EXECUTOR);

        try {
            int edicoes = edicoesFuture.get(3, TimeUnit.SECONDS);
            livro.setLivrosSemelhantes(edicoes);
        } catch (Exception e) {
            System.err.println("Tempo esgotado ao buscar edições: " + e.getMessage());
            livro.setLivrosSemelhantes(0);
        }

        return livro;
    }

    private static int buscarNumeroEdicoes(String workId) {
        try {
            String url = "https://openlibrary.org/works/" + workId + "/editions.json";

            Response response = CLIENT.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                return 0;
            }

            String jsonData = response.readEntity(String.class);
            JsonObject parsedResponse = JsonParser.parseString(jsonData).getAsJsonObject();

            if (parsedResponse.has("size")) {
                JsonElement sizeElement = parsedResponse.get("size");

                if (sizeElement.isJsonPrimitive()) {
                    return sizeElement.getAsInt();
                }
            }

            // Alternativa: contar os elementos no array "entries"
            if (parsedResponse.has("entries") && parsedResponse.get("entries").isJsonArray()) {
                return parsedResponse.getAsJsonArray("entries").size();
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Exceção ao buscar edições: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Analisa uma string de data em diferentes formatos possíveis.
     */
    private static LocalDate parseData(String textoData) {
        if (textoData == null || textoData.trim().isEmpty()) {
            return null;
        }

        try {
            // Formatos ISO
            if (textoData.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(textoData.trim());  // LocalDate.parse() já usa ISO por padrão
            }

            if (textoData.matches("\\d{4}-\\d{2}")) {
                return LocalDate.parse(textoData.trim() + "-01");  // Adiciona dia 01
            }

            if (textoData.matches("\\d{4}")) {
                int ano = Integer.parseInt(textoData.trim());
                return LocalDate.of(ano, 1, 1);
            }

            // Tenta extrair o ano de formatos como "October 1, 1988"
            if (textoData.matches(".*\\d{4}.*")) {
                // Procura por 4 dígitos seguidos que representam um ano
                String anoStr = textoData.replaceAll(".*?(\\d{4}).*", "$1");
                try {
                    int ano = Integer.parseInt(anoStr);
                    if (ano >= 1000 && ano <= 9999) {
                        return LocalDate.of(ano, 1, 1);
                    }
                } catch (NumberFormatException e) {
                }
            }

            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
                return LocalDate.parse(textoData.trim(), formatter);
            } catch (Exception e) {
            }

            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH);
                return LocalDate.parse(textoData.trim(), formatter);
            } catch (Exception e) {
            }

            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH);
                return LocalDate.parse(textoData.trim(), formatter).withDayOfMonth(1);
            } catch (Exception e) {
            }

            // Tenta usar o FormatacaoDatas para outros formatos
            return FormatacaoDatas.analisarEntradaUsuario(textoData);
        } catch (Exception e) {
            System.err.println("Erro ao converter data: " + textoData + " - " + e.getMessage());

            // Última tentativa: extrair somente o ano se estiver presente na string
            try {
                String anoPattern = ".*?(\\d{4}).*";
                if (textoData.matches(anoPattern)) {
                    String anoStr = textoData.replaceAll(anoPattern, "$1");
                    int ano = Integer.parseInt(anoStr);
                    return LocalDate.of(ano, 1, 1);
                }
            } catch (Exception ex) {

            }

            return null;
        }
    }

    // Método para encerrar recursos quando a aplicação for finalizada
    public static void encerrarRecursos() {
        try {
            CLIENT.close();
            EXECUTOR.shutdown();
            EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Erro ao encerrar recursos: " + e.getMessage());
        }
    }
}