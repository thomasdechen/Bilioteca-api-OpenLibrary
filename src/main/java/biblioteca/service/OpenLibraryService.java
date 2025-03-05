package biblioteca.service;

import biblioteca.model.Livro;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class OpenLibraryService {
    private static final String BASE_URL = "https://openlibrary.org/api/books?bibkeys=ISBN:";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static JsonObject buscarInformacoesPorIsbn(String isbn) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + isbn + "&format=json&jscmd=data")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Resposta não esperada: " + response);
            }

            String jsonData = response.body().string();
            JsonObject parsedResponse = JsonParser.parseString(jsonData).getAsJsonObject();

            String isbnKey = "ISBN:" + isbn;
            if (!parsedResponse.has(isbnKey)) {
                throw new IOException("Nenhum livro encontrado para o ISBN: " + isbn);
            }

            return parsedResponse.getAsJsonObject(isbnKey);
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

        return livro;
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