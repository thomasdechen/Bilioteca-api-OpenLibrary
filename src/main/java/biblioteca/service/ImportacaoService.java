package biblioteca.service;

import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import biblioteca.util.FormatacaoDatas;

/**
 * Serviço responsável pela importação de dados de livros de arquivos CSV.
 */
public class ImportacaoService {
    private LivroRepository repository;
    private LivroService livroService;

    public ImportacaoService() {
        this.repository = new LivroRepository();
        this.livroService = new LivroService();
    }

    /**
     * Importa livros de um arquivo CSV.
     *
     * @param caminhoArquivo caminho para o arquivo CSV
     * @return Uma lista com os resultados da importação
     * @throws IOException se houver erro na leitura do arquivo
     * @throws CsvValidationException se o formato do CSV for inválido
     */
    public ImportacaoResultado importarCSV(String caminhoArquivo) throws IOException, CsvValidationException {
        ImportacaoResultado resultado = new ImportacaoResultado();

        // Map para controlar ISBNs já encontrados no arquivo CSV
        Map<String, List<Integer>> isbnLinhasMap = new HashMap<>();

        List<Integer> linhasSemIsbn = new ArrayList<>();

        List<LivroImportacao> livrosParaProcessar = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(caminhoArquivo)).build()) {
            String[] cabecalho = reader.readNext();
            if (cabecalho == null) {
                throw new IOException("Arquivo CSV vazio ou inválido");
            }

            // Mapeia os índices das colunas
            int idxTitulo = encontrarIndiceColuna(cabecalho, "titulo", "título", "title");
            int idxAutores = encontrarIndiceColuna(cabecalho, "autores", "autor", "authors", "author");
            int idxIsbn = encontrarIndiceColuna(cabecalho, "isbn");
            int idxEditora = encontrarIndiceColuna(cabecalho, "editora", "publisher");
            int idxDataPublicacao = encontrarIndiceColuna(cabecalho, "data_publicacao", "data publicação", "publish_date");

            if (idxTitulo == -1 || idxAutores == -1) {
                throw new IOException("Arquivo CSV não contém as colunas obrigatórias (título e autores)");
            }

            int numeroCamposEsperados = cabecalho.length;

            String[] linha;
            int numeroLinha = 1; // Começando em 1 porque o cabeçalho já foi lido

            // PASSO 1: Ler todo o arquivo e verificar duplicações de ISBN dentro do CSV
            while ((linha = reader.readNext()) != null) {
                numeroLinha++;

                try {
                    if (linha.length != numeroCamposEsperados) {
                        resultado.erros++;
                        resultado.mensagensErro.add("Linha " + numeroLinha + ": número incorreto de campos. Esperado: "
                                + numeroCamposEsperados + ", Encontrado: " + linha.length);
                        continue;
                    }

                    if (idxTitulo >= linha.length || idxAutores >= linha.length ||
                            linha[idxTitulo].trim().isEmpty() || linha[idxAutores].trim().isEmpty()) {
                        resultado.erros++;
                        resultado.mensagensErro.add("Linha " + numeroLinha + ": faltam campos obrigatórios (título ou autores)");
                        continue;
                    }

                    // Processando ISBN
                    String isbn = null;
                    if (idxIsbn >= 0 && idxIsbn < linha.length && !linha[idxIsbn].trim().isEmpty()) {
                        isbn = linha[idxIsbn].trim();

                        if (!isbnLinhasMap.containsKey(isbn)) {
                            isbnLinhasMap.put(isbn, new ArrayList<>());
                        }
                        isbnLinhasMap.get(isbn).add(numeroLinha);
                    } else {
                        linhasSemIsbn.add(numeroLinha);
                    }

                    // Cria objeto de importação temporário
                    LivroImportacao livroImportacao = new LivroImportacao();
                    livroImportacao.numeroLinha = numeroLinha;

                    Livro livro = new Livro();
                    livro.setTitulo(linha[idxTitulo].trim());
                    livro.setAutores(linha[idxAutores].trim());
                    livro.setIsbn(isbn);

                    if (idxEditora >= 0 && idxEditora < linha.length) {
                        livro.setEditora(linha[idxEditora].trim());
                    }

                    if (idxDataPublicacao >= 0 && idxDataPublicacao < linha.length && !linha[idxDataPublicacao].trim().isEmpty()) {
                        try {
                            livro.setDataPublicacao(FormatacaoDatas.analisarEntradaUsuario(linha[idxDataPublicacao]));
                        } catch (DateTimeParseException e) {
                            resultado.avisos++;
                            resultado.mensagensAviso.add("Linha " + numeroLinha + ": Data inválida para livro: " + livro.getTitulo());
                        }
                    }

                    livroImportacao.livro = livro;
                    livrosParaProcessar.add(livroImportacao);

                } catch (Exception e) {
                    resultado.erros++;
                    resultado.mensagensErro.add("Linha " + numeroLinha + ": " + e.getMessage());
                }
            }

            // PASSO 2: Gerar avisos para ISBNs duplicados no CSV
            for (Map.Entry<String, List<Integer>> entry : isbnLinhasMap.entrySet()) {
                String isbn = entry.getKey();
                List<Integer> linhas = entry.getValue();

                if (linhas.size() > 1) {
                    resultado.avisos++;
                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("ISBN '").append(isbn).append("' aparece em múltiplas linhas: ");

                    for (int i = 0; i < linhas.size(); i++) {
                        mensagem.append(linhas.get(i));
                        if (i < linhas.size() - 1) {
                            mensagem.append(", ");
                        }
                    }

                    mensagem.append(". Apenas o primeiro livro com este ISBN será processado, os demais serão ignorados. " +
                            "Se o ISBN já existir no banco, o livro será atualizado ou ignorado dependendo se há diferenças.");
                    resultado.mensagensAviso.add(mensagem.toString());
                }
            }

            // Adicionar aviso para livros sem ISBN
            if (!linhasSemIsbn.isEmpty()) {
                resultado.avisos++;
                StringBuilder mensagem = new StringBuilder("Livros sem ISBN nas linhas: ");
                for (int i = 0; i < linhasSemIsbn.size(); i++) {
                    mensagem.append(linhasSemIsbn.get(i));
                    if (i < linhasSemIsbn.size() - 1) {
                        mensagem.append(", ");
                    }
                }
                mensagem.append(". Cada livro será inserido como um novo registro.");
                resultado.mensagensAviso.add(mensagem.toString());
            }

            // PASSO 3: Verificar existência de livros no banco de dados
            Map<String, Livro> livrosExistentes = new HashMap<>();
            for (LivroImportacao livroImportacao : livrosParaProcessar) {
                String isbn = livroImportacao.livro.getIsbn();
                if (isbn != null && !isbn.isEmpty() && !livrosExistentes.containsKey(isbn)) {
                    Livro existente = repository.buscarPorIsbn(isbn);
                    if (existente != null) {
                        livrosExistentes.put(isbn, existente);
                    }
                }
            }

            // PASSO 4: Processar os livros
            // Rastrear ISBNs que já foram processados do CSV para evitar processamento duplicado
            Set<String> isbnsProcessados = new HashSet<>();

            for (LivroImportacao livroImportacao : livrosParaProcessar) {
                Livro livro = livroImportacao.livro;
                String isbn = livro.getIsbn();

                try {
                    if (isbn == null || isbn.isEmpty()) {
                        // NOVA VERIFICAÇÃO: Verifica se existe livro idêntico no banco
                        Livro livroIdentico = verificarLivroIdentico(livro);
                        if (livroIdentico != null) {
                            resultado.ignorados++;
                            resultado.mensagensAviso.add("Linha " + livroImportacao.numeroLinha +
                                    ": Livro idêntico já existe no banco: '" + livroIdentico.getTitulo() + "' por '" +
                                    livroIdentico.getAutores() + "'. Registro ignorado.");
                            continue;
                        }

                        livroService.salvarLivro(livro);
                        resultado.inseridos++;
                        continue;
                    }

                    // Caso 2: ISBN já processado nesta importação - pula para evitar duplicação
                    if (isbnsProcessados.contains(isbn)) {
                        resultado.ignorados++;
                        resultado.mensagensAviso.add("Linha " + livroImportacao.numeroLinha +
                                ": ISBN '" + isbn + "' já foi processado anteriormente neste arquivo.");
                        continue;
                    }

                    // Marca ISBN como processado
                    isbnsProcessados.add(isbn);

                    // Caso 3: ISBN existe no banco de dados - potencial atualização
                    if (livrosExistentes.containsKey(isbn)) {
                        Livro existente = livrosExistentes.get(isbn);

                        // NOVA VERIFICAÇÃO: Verifica se o livro é idêntico ao existente
                        if (livrosIdenticos(existente, livro)) {
                            resultado.ignorados++;
                            resultado.mensagensAviso.add("Linha " + livroImportacao.numeroLinha +
                                    ": Livro com ISBN '" + isbn + "' é idêntico ao existente no banco. Registro ignorado.");
                            continue;
                        }

                        // Verifica se os dados são significativamente diferentes
                        boolean titulosDiferentes = !existente.getTitulo().equals(livro.getTitulo());
                        boolean autoresDiferentes = !existente.getAutores().equals(livro.getAutores());

                        if (titulosDiferentes || autoresDiferentes) {
                            resultado.avisos++;
                            resultado.mensagensAviso.add("Linha " + livroImportacao.numeroLinha +
                                    ": O livro com ISBN '" + isbn + "' tem título/autor diferente do existente no banco. " +
                                    "Existente: '" + existente.getTitulo() + "' por '" + existente.getAutores() + "'. " +
                                    "Novo: '" + livro.getTitulo() + "' por '" + livro.getAutores() + "'. " +
                                    "Os dados serão atualizados.");
                        }

                        // Realiza a atualização
                        existente.setTitulo(livro.getTitulo());
                        existente.setAutores(livro.getAutores());
                        if (livro.getEditora() != null) existente.setEditora(livro.getEditora());
                        if (livro.getDataPublicacao() != null) existente.setDataPublicacao(livro.getDataPublicacao());

                        livroService.salvarLivro(existente);
                        resultado.atualizados++;
                    }
                    // Caso 4: ISBN novo - inserção
                    else {
                        livroService.salvarLivro(livro);
                        resultado.inseridos++;
                    }
                } catch (Exception e) {
                    resultado.erros++;
                    resultado.mensagensErro.add("Linha " + livroImportacao.numeroLinha + ": " + e.getMessage());
                }
            }
        }

        return resultado;
    }

    /**
     * Verifica se dois livros são idênticos em todos os campos relevantes
     * @param livro1 primeiro livro
     * @param livro2 segundo livro
     * @return true se os livros forem idênticos
     */
    private boolean livrosIdenticos(Livro livro1, Livro livro2) {
        // Compara os campos básicos
        boolean titulosIguais = Objects.equals(livro1.getTitulo(), livro2.getTitulo());
        boolean autoresIguais = Objects.equals(livro1.getAutores(), livro2.getAutores());
        boolean editorasIguais = Objects.equals(livro1.getEditora(), livro2.getEditora());

        // Compara datas de publicação considerando que podem ser nulas
        boolean datasIguais = false;
        if (livro1.getDataPublicacao() == null && livro2.getDataPublicacao() == null) {
            datasIguais = true;
        } else if (livro1.getDataPublicacao() != null && livro2.getDataPublicacao() != null) {
            datasIguais = livro1.getDataPublicacao().equals(livro2.getDataPublicacao());
        }

        // Compara ISBNs considerando que podem ser nulos
        boolean isbnsIguais = Objects.equals(livro1.getIsbn(), livro2.getIsbn());

        return titulosIguais && autoresIguais && editorasIguais && datasIguais && isbnsIguais;
    }

    /**
     * Verifica se existe um livro idêntico no banco de dados (usado para livros sem ISBN)
     * @param livro livro a ser verificado
     * @return livro existente idêntico ou null se não existir
     */
    private Livro verificarLivroIdentico(Livro livro) {
        // Busca por título e autor, que são os campos mais importantes para identificar um livro
        List<Livro> candidatos = repository.buscarPorCampo("titulo", livro.getTitulo());

        // Filtra apenas os que têm o mesmo autor
        for (Livro candidato : candidatos) {
            if (livrosIdenticos(candidato, livro)) {
                return candidato;
            }
        }

        return null;
    }

    /**
     * Encontra o índice da coluna no cabeçalho, considerando variações de nome.
     *
     * @param cabecalho array de strings com os nomes das colunas
     * @param possiveisNomes variações possíveis do nome da coluna
     * @return o índice da coluna ou -1 se não encontrada
     */
    private int encontrarIndiceColuna(String[] cabecalho, String... possiveisNomes) {
        for (int i = 0; i < cabecalho.length; i++) {
            String nomeColuna = cabecalho[i].trim().toLowerCase();
            for (String nome : possiveisNomes) {
                if (nomeColuna.equals(nome.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Classe auxiliar para armazenar informações temporárias durante a importação
     */
    private static class LivroImportacao {
        public Livro livro;
        public int numeroLinha;
    }

    /**
     * Classe para armazenar os resultados da importação
     */
    public static class ImportacaoResultado {
        public int inseridos = 0;
        public int atualizados = 0;
        public int ignorados = 0;
        public int erros = 0;
        public int avisos = 0;
        public List<String> mensagensErro = new ArrayList<>();
        public List<String> mensagensAviso = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Resultado da importação:\n");
            sb.append("- Livros inseridos: ").append(inseridos).append("\n");
            sb.append("- Livros atualizados: ").append(atualizados).append("\n");
            sb.append("- Livros ignorados: ").append(ignorados).append("\n");
            sb.append("- Erros: ").append(erros).append("\n");
            sb.append("- Avisos: ").append(avisos);
            return sb.toString();
        }
    }
}