package biblioteca.service;

import biblioteca.model.Livro;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class OpenLibraryService {
    private static final String BASE_URL = "https://openlibrary.org/api/books?bibkeys=ISBN:";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static JsonObject buscarInformacoesPorIsbn(String isbn) {
        Client client = ClientBuilder.newClient();

        try {
            Response response = client.target(BASE_URL + isbn + "&format=json&jscmd=data")
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
        } finally {
            client.close();
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

        // Extrair o work ID
        if (dadosLivro.has("works") && dadosLivro.get("works").isJsonArray()) {
            JsonArray works = dadosLivro.getAsJsonArray("works");
            if (!works.isEmpty()) {
                JsonObject work = works.get(0).getAsJsonObject();
                if (work.has("key")) {
                    String workId = work.get("key").getAsString().replace("/works/", "");

                    // Buscar número de edições
                    int edicoes = buscarNumeroEdicoes(workId);
                    livro.setLivrosSemelhantes(edicoes);
                }
            }
        }

        return livro;
    }

    private static List<String> extrairTodosIsbn(JsonObject dadosLivro) {
        List<String> todosIsbn = new ArrayList<>();

        if (dadosLivro.has("isbn_10")) {
            JsonArray isbn10 = dadosLivro.getAsJsonArray("isbn_10");
            for (JsonElement isbn : isbn10) {
                todosIsbn.add(isbn.getAsString());
            }
        }

        if (dadosLivro.has("isbn_13")) {
            JsonArray isbn13 = dadosLivro.getAsJsonArray("isbn_13");
            for (JsonElement isbn : isbn13) {
                todosIsbn.add(isbn.getAsString());
            }
        }

        return todosIsbn;
    }

    private static int buscarNumeroEdicoes(String workId) {
        Client client = ClientBuilder.newClient();

        try {
            Response response = client.target("https://openlibrary.org/works/" + workId + "/editions.json")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                return 0;
            }

            String jsonData = response.readEntity(String.class);
            JsonObject parsedResponse = JsonParser.parseString(jsonData).getAsJsonObject();

            System.out.println("Resposta da API: " + parsedResponse);

            if (parsedResponse.has("size")) {
                JsonElement sizeElement = parsedResponse.get("size");

                if (sizeElement.isJsonPrimitive()) {
                    return sizeElement.getAsInt();
                }
            }

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            client.close();
        }
    }

    private static LocalDate parseData(String dataString) {
        if (dataString == null || dataString.trim().isEmpty()) {
            return null;
        }

        try {
            if (dataString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dataString.trim(), FULL_DATE_FORMATTER);
            }

            if (dataString.matches("\\d{4}-\\d{2}")) {
                return LocalDate.parse(dataString.trim(), YEAR_MONTH_FORMATTER);
            }

            if (dataString.matches("\\d{4}")) {
                int year = Integer.parseInt(dataString.trim());
                return LocalDate.of(year, 1, 1);
            }
        } catch (DateTimeParseException e) {
            return null;
        }

        return null;
    }
}