package biblioteca.service;

import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.util.ArrayList;

public class ImportacaoService {
    private LivroRepository repository;

    public ImportacaoService() {
        this.repository = new LivroRepository();
    }

    public void importarCSV(String caminhoArquivo) {
        try (CSVReader reader = new CSVReader(new FileReader(caminhoArquivo))) {
            String[] linha;
            // Pula cabeçalho
            reader.readNext();

            while ((linha = reader.readNext()) != null) {
                Livro livro = new Livro();
                livro.setTitulo(linha[0]);
                livro.setAutores(linha[1]);
                livro.setIsbn(linha[2]);
                // Adicione mais campos conforme seu CSV

                Livro existente = repository.buscarPorIsbn(livro.getIsbn());
                if (existente != null) {
                    // Atualiza livro existente
                    existente.setTitulo(livro.getTitulo());
                    repository.salvar(existente);
                } else {
                    // Salva novo livro
                    repository.salvar(livro);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}