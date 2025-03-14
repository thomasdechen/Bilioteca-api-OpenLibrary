package biblioteca.service;


import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;
import biblioteca.util.FormatacaoDatas;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Serviço responsável pela importação de dados de livros através de arquivo de texto CSV.
 */
public class ImportacaoService {
    private final LivroRepository repository;
    private final LivroService livroService;

    public ImportacaoService() {
        this.repository = new LivroRepository();
        this.livroService = new LivroService();
    }

    /**
     * Importa livros de um arquivo CSV.
     *
     * @param caminhoArquivo caminho para o arquivo CSV
     * @return Uma lista com os resultados da importação
     */
    public ImportacaoResultado importarCSV(String caminhoArquivo) throws IOException, CsvValidationException {
        ImportacaoResultado resultado = new ImportacaoResultado();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(caminhoArquivo)).build()) {
            String[] cabecalho = reader.readNext();
            if (cabecalho == null) {
                throw new IOException("Arquivo CSV vazio ou inválido");
            }

            MapaColunas colunas = mapearColunas(cabecalho);

            if (colunas.idxTitulo == -1 || colunas.idxAutores == -1) {
                throw new IOException("Arquivo CSV não contém as colunas obrigatórias (título e autores)");
            }

            // Passo 1: Leitura inicial e validação dos dados do CSV
            List<LivroImportacao> livrosParaProcessar = lerDadosCSV(reader, cabecalho.length, colunas, resultado);

            // Passo 2: Verificar existência de livros no banco de dados
            Map<String, Livro> livrosExistentes = buscarLivrosExistentes(livrosParaProcessar);

            // Passo 3: Processar os livros
            processarLivros(livrosParaProcessar, livrosExistentes, resultado);
        } catch (CsvValidationException e) {
            resultado.registrarErro("Erro de validação do CSV: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            resultado.registrarErro("Erro de leitura do arquivo: " + e.getMessage());
            throw e;
        }

        return resultado;
    }

    /**
     * Mapeia as colunas do arquivo CSV.
     */
    private MapaColunas mapearColunas(String[] cabecalho) {
        MapaColunas colunas = new MapaColunas();
        colunas.idxTitulo = encontrarIndiceColuna(cabecalho, "titulo", "título", "title");
        colunas.idxAutores = encontrarIndiceColuna(cabecalho, "autores", "autor", "authors", "author");
        colunas.idxIsbn = encontrarIndiceColuna(cabecalho, "isbn");
        colunas.idxEditora = encontrarIndiceColuna(cabecalho, "editora", "publisher");
        colunas.idxDataPublicacao = encontrarIndiceColuna(cabecalho, "data_publicacao", "data publicação", "publish_date");
        return colunas;
    }

    /**
     * Lê todos os dados do CSV e realiza validações iniciais.
     */
    private List<LivroImportacao> lerDadosCSV(CSVReader reader, int numeroCamposEsperados,
                                              MapaColunas colunas, ImportacaoResultado resultado) throws IOException, CsvValidationException {
        Map<String, List<Integer>> isbnLinhasMap = new HashMap<>();
        List<Integer> linhasSemIsbn = new ArrayList<>();
        List<LivroImportacao> livrosParaProcessar = new ArrayList<>();

        String[] linha;
        int numeroLinha = 1; // Começando em 1 porque o cabeçalho já foi lido

        try {
            while ((linha = reader.readNext()) != null) {
                numeroLinha++;

                try {
                    if (linha.length != numeroCamposEsperados) {
                        resultado.registrarErro("Linha " + numeroLinha + ": número incorreto de campos. Esperado: "
                                + numeroCamposEsperados + ", Encontrado: " + linha.length);
                        continue;
                    }

                    if (dadosObrigatoriosAusentes(linha, colunas)) {
                        resultado.registrarErro("Linha " + numeroLinha + ": faltam campos obrigatórios (título ou autores)");
                        continue;
                    }

                    // Verifica se os campos contêm apenas símbolos
                    if (camposInválidos(linha, colunas, numeroLinha, resultado)) {
                        continue; // Pula esta linha se algum campo estiver inválido
                    }

                    String isbn = processarIsbn(linha, colunas.idxIsbn, numeroLinha, isbnLinhasMap, linhasSemIsbn, resultado);

                    Livro livro = criarLivro(linha, colunas, isbn, numeroLinha, resultado);

                    // Adiciona à lista de processamento
                    LivroImportacao livroImportacao = new LivroImportacao(livro, numeroLinha);
                    livrosParaProcessar.add(livroImportacao);
                } catch (Exception e) {
                    resultado.registrarErro("Linha " + numeroLinha + ": " + e.getMessage());
                }
            }
        } catch (CsvValidationException e) {
            resultado.registrarErro("Erro na linha " + numeroLinha + ": formato CSV inválido");
            throw e;
        }

        registrarAvisosIsbnDuplicados(isbnLinhasMap, resultado);

        registrarAvisoLivrosSemIsbn(linhasSemIsbn, resultado);

        return livrosParaProcessar;
    }

    /**
     * Verifica se os dados obrigatórios estão ausentes.
     */
    private boolean dadosObrigatoriosAusentes(String[] linha, MapaColunas colunas) {
        return colunas.idxTitulo >= linha.length || colunas.idxAutores >= linha.length ||
                linha[colunas.idxTitulo].trim().isEmpty() || linha[colunas.idxAutores].trim().isEmpty();
    }

    private String processarIsbn(String[] linha, int idxIsbn, int numeroLinha,
                                 Map<String, List<Integer>> isbnLinhasMap, List<Integer> linhasSemIsbn,
                                 ImportacaoResultado resultado) {
        if (idxIsbn >= 0 && idxIsbn < linha.length && !linha[idxIsbn].trim().isEmpty()) {
            String isbn = linha[idxIsbn].trim();

            // Validação do formato do ISBN
            if (!isbnValido(isbn)) {
                resultado.registrarErro("Linha " + numeroLinha +
                        ": ISBN '" + isbn + "' inválido. ISBN deve ter 10 ou 13 dígitos.");
                linhasSemIsbn.add(numeroLinha);
                return null;
            }

            isbnLinhasMap.computeIfAbsent(isbn, k -> new ArrayList<>()).add(numeroLinha);
            return isbn;
        } else {
            linhasSemIsbn.add(numeroLinha);
            return null;
        }
    }

    /**
     * Cria um objeto Livro a partir de uma linha do CSV.
     */
    private Livro criarLivro(String[] linha, MapaColunas colunas, String isbn,
                             int numeroLinha, ImportacaoResultado resultado) {
        Livro livro = new Livro();
        livro.setTitulo(linha[colunas.idxTitulo].trim());
        livro.setAutores(linha[colunas.idxAutores].trim());
        livro.setIsbn(isbn);

        if (colunas.idxEditora >= 0 && colunas.idxEditora < linha.length) {
            livro.setEditora(linha[colunas.idxEditora].trim());
        }

        if (colunas.idxDataPublicacao >= 0 && colunas.idxDataPublicacao < linha.length &&
                !linha[colunas.idxDataPublicacao].trim().isEmpty()) {
            try {
                livro.setDataPublicacao(FormatacaoDatas.analisarEntradaUsuario(linha[colunas.idxDataPublicacao]));
            } catch (DateTimeParseException e) {
                resultado.registrarAviso("Linha " + numeroLinha + ": Data inválida para livro: " + livro.getTitulo());
            }
        }

        return livro;
    }

    /**
     * Registra avisos para ISBNs duplicados no CSV.
     */
    private void registrarAvisosIsbnDuplicados(Map<String, List<Integer>> isbnLinhasMap, ImportacaoResultado resultado) {
        for (Map.Entry<String, List<Integer>> entry : isbnLinhasMap.entrySet()) {
            String isbn = entry.getKey();
            List<Integer> linhas = entry.getValue();

            if (linhas.size() > 1) {
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
                resultado.registrarAviso(mensagem.toString());
            }
        }
    }

    /**
     * Registra aviso para linhas sem ISBN.
     */
    private void registrarAvisoLivrosSemIsbn(List<Integer> linhasSemIsbn, ImportacaoResultado resultado) {
        if (!linhasSemIsbn.isEmpty()) {
            StringBuilder mensagem = new StringBuilder("Livros sem ISBN nas linhas: ");
            for (int i = 0; i < linhasSemIsbn.size(); i++) {
                mensagem.append(linhasSemIsbn.get(i));
                if (i < linhasSemIsbn.size() - 1) {
                    mensagem.append(", ");
                }
            }
            mensagem.append(". Cada livro será verificado para garantir que não exista um com mesmo título e autor.");
            resultado.registrarAviso(mensagem.toString());
        }
    }

    /**
     * Busca livros existentes no banco de dados.
     */
    private Map<String, Livro> buscarLivrosExistentes(List<LivroImportacao> livrosParaProcessar) {
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
        return livrosExistentes;
    }

    /**
     * Processa a lista de livros para inserção, atualização ou ignorar.
     */
    private void processarLivros(List<LivroImportacao> livrosParaProcessar,
                                 Map<String, Livro> livrosExistentes, ImportacaoResultado resultado) {
        Set<String> isbnsProcessados = new HashSet<>();
        Set<String> tituloAutorProcessados = new HashSet<>();

        for (LivroImportacao livroImportacao : livrosParaProcessar) {
            Livro livro = livroImportacao.livro;
            String isbn = livro.getIsbn();
            int numeroLinha = livroImportacao.numeroLinha;
            String tituloAutorChave = livro.getTitulo() + "|" + livro.getAutores();

            try {
                // Caso 1: Verificação de título e autor duplicados
                if (tituloAutorProcessados.contains(tituloAutorChave)) {
                    resultado.registrarIgnorado();
                    resultado.registrarAviso("Linha " + numeroLinha +
                            ": Livro com mesmo título e autor já foi processado anteriormente neste arquivo: '" +
                            livro.getTitulo() + "' por '" + livro.getAutores() + "'. Registro ignorado.");
                    continue;
                }

                // Caso 2: Verificar se já existe um livro com mesmo título e autor
                Livro livroExistenteTituloAutor = verificarLivroPorTituloAutor(livro);
                if (livroExistenteTituloAutor != null) {
                    resultado.registrarIgnorado();
                    resultado.registrarAviso("Linha " + numeroLinha +
                            ": Já existe um livro com mesmo título e autor no banco de dados: '" +
                            livro.getTitulo() + "' por '" + livro.getAutores() + "'. Registro ignorado.");
                    continue;
                }

                // Caso 3: Livro sem ISBN
                if (isbn == null || isbn.isEmpty()) {
                    livroService.salvarLivro(livro);
                    resultado.registrarInserido();
                    tituloAutorProcessados.add(tituloAutorChave);
                    continue;
                }

                // Caso 4: ISBN já processado nesta importação - pula para evitar duplicação
                if (isbnsProcessados.contains(isbn)) {
                    resultado.registrarIgnorado();
                    resultado.registrarAviso("Linha " + numeroLinha +
                            ": ISBN '" + isbn + "' já foi processado anteriormente neste arquivo.");
                    continue;
                }

                // Marca ISBN como processado
                isbnsProcessados.add(isbn);
                tituloAutorProcessados.add(tituloAutorChave);

                // Caso 5: ISBN existe no banco de dados - potencial atualização
                if (livrosExistentes.containsKey(isbn)) {
                    processarLivroExistente(livro, livrosExistentes.get(isbn), numeroLinha, resultado);
                }
                // Caso 6: ISBN novo - inserção
                else {
                    livroService.salvarLivro(livro);
                    resultado.registrarInserido();
                }
            } catch (Exception e) {
                resultado.registrarErro("Linha " + numeroLinha + ": " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se já existe um livro com o mesmo título e autor no banco de dados.
     */
    private Livro verificarLivroPorTituloAutor(Livro livro) {
        List<Livro> candidatos = repository.buscarPorCampo("titulo", livro.getTitulo());

        for (Livro candidato : candidatos) {
            if (tituloAutorIguais(candidato, livro)) {
                return candidato;
            }
        }

        return null;
    }

    private boolean tituloAutorIguais(Livro livro1, Livro livro2) {
        return Objects.equals(livro1.getTitulo(), livro2.getTitulo()) &&
                Objects.equals(livro1.getAutores(), livro2.getAutores());
    }

    private boolean isbnValido(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        String isbnLimpo = isbn.replaceAll("[-\\s]", "");

        return isbnLimpo.length() == 10 || isbnLimpo.length() == 13;
    }

    private boolean contemApenasSímbolos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return true;
        }
        // Verifica se o texto NÃO contém nenhuma letra ou número
        return !texto.trim().matches(".*[a-zA-Z0-9].*");
    }

    /**
     * Checar se campos titulo, autores e editora estão invalidos
     * Por exemplo: titulo somente composto por símbolos
     */
    private boolean camposInválidos(String[] linha, MapaColunas colunas, int numeroLinha, ImportacaoResultado resultado) {
        boolean inválido = false;

        if (colunas.idxTitulo >= 0 && colunas.idxTitulo < linha.length) {
            String titulo = linha[colunas.idxTitulo].trim();
            if (contemApenasSímbolos(titulo)) {
                resultado.registrarErro("Linha " + numeroLinha + ": Título inválido. Não pode conter apenas símbolos: '" + titulo + "'");
                inválido = true;
            }
        }

        if (colunas.idxAutores >= 0 && colunas.idxAutores < linha.length) {
            String autores = linha[colunas.idxAutores].trim();
            if (contemApenasSímbolos(autores)) {
                resultado.registrarErro("Linha " + numeroLinha + ": Autor inválido. Não pode conter apenas símbolos: '" + autores + "'");
                inválido = true;
            }
        }

        if (colunas.idxEditora >= 0 && colunas.idxEditora < linha.length && !linha[colunas.idxEditora].trim().isEmpty()) {
            String editora = linha[colunas.idxEditora].trim();
            if (contemApenasSímbolos(editora)) {
                resultado.registrarErro("Linha " + numeroLinha + ": Editora inválida. Não pode conter apenas símbolos: '" + editora + "'");
                inválido = true;
            }
        }

        return inválido;
    }

    private void processarLivroExistente(Livro livroNovo, Livro livroExistente,
                                         int numeroLinha, ImportacaoResultado resultado) {
        if (livrosIdenticos(livroExistente, livroNovo)) {
            resultado.registrarIgnorado();
            resultado.registrarAviso("Linha " + numeroLinha +
                    ": Livro com ISBN '" + livroNovo.getIsbn() + "' é idêntico ao existente no banco. Registro ignorado.");
            return;
        }

        // Verifica se os dados são significativamente diferentes
        boolean titulosDiferentes = !livroExistente.getTitulo().equals(livroNovo.getTitulo());
        boolean autoresDiferentes = !livroExistente.getAutores().equals(livroNovo.getAutores());

        if (titulosDiferentes || autoresDiferentes) {
            // Verifica se a combinação título/autor já existe em outro livro
            Livro existenteComMesmoTituloAutor = verificarLivroPorTituloAutor(livroNovo);
            if (existenteComMesmoTituloAutor != null &&
                    !Objects.equals(existenteComMesmoTituloAutor.getIsbn(), livroNovo.getIsbn())) {
                resultado.registrarIgnorado();
                resultado.registrarAviso("Linha " + numeroLinha +
                        ": A atualização causaria duplicação de título e autor com livro existente. " +
                        "Título: '" + livroNovo.getTitulo() + "', Autor: '" + livroNovo.getAutores() + "'. " +
                        "Registro ignorado.");
                return;
            }

            resultado.registrarAviso("Linha " + numeroLinha +
                    ": O livro com ISBN '" + livroNovo.getIsbn() + "' tem título/autor diferente do existente no banco. " +
                    "Existente: '" + livroExistente.getTitulo() + "' por '" + livroExistente.getAutores() + "'. " +
                    "Novo: '" + livroNovo.getTitulo() + "' por '" + livroNovo.getAutores() + "'. " +
                    "Os dados serão atualizados.");
        }

        atualizarLivroExistente(livroExistente, livroNovo);
        livroService.salvarLivro(livroExistente);
        resultado.registrarAtualizado();
    }

    private void atualizarLivroExistente(Livro existente, Livro novo) {
        existente.setTitulo(novo.getTitulo());
        existente.setAutores(novo.getAutores());
        if (novo.getEditora() != null) existente.setEditora(novo.getEditora());
        if (novo.getDataPublicacao() != null) existente.setDataPublicacao(novo.getDataPublicacao());
    }

    /**
     * Verifica se dois livros são idênticos em todos os campos relevantes
     */
    private boolean livrosIdenticos(Livro livro1, Livro livro2) {
        boolean titulosIguais = Objects.equals(livro1.getTitulo(), livro2.getTitulo());
        boolean autoresIguais = Objects.equals(livro1.getAutores(), livro2.getAutores());
        boolean editorasIguais = Objects.equals(livro1.getEditora(), livro2.getEditora());
        boolean isbnsIguais = Objects.equals(livro1.getIsbn(), livro2.getIsbn());

        boolean datasIguais = (livro1.getDataPublicacao() == null && livro2.getDataPublicacao() == null) ||
                (livro1.getDataPublicacao() != null && livro2.getDataPublicacao() != null &&
                        livro1.getDataPublicacao().equals(livro2.getDataPublicacao()));

        return titulosIguais && autoresIguais && editorasIguais && datasIguais && isbnsIguais;
    }

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

    private static class MapaColunas {
        public int idxTitulo = -1;
        public int idxAutores = -1;
        public int idxIsbn = -1;
        public int idxEditora = -1;
        public int idxDataPublicacao = -1;
    }

    /**
     * Classe auxiliar para armazenar informações temporárias durante a importação
     */
    private static class LivroImportacao {
        public final Livro livro;
        public final int numeroLinha;

        public LivroImportacao(Livro livro, int numeroLinha) {
            this.livro = livro;
            this.numeroLinha = numeroLinha;
        }
    }

    public static class ImportacaoResultado {
        public int inseridos = 0;
        public int atualizados = 0;
        public int ignorados = 0;
        public int erros = 0;
        public int avisos = 0;
        private final List<String> mensagensErro = new ArrayList<>();
        private final List<String> mensagensAviso = new ArrayList<>();

        public void registrarInserido() {
            inseridos++;
        }

        public void registrarAtualizado() {
            atualizados++;
        }

        public void registrarIgnorado() {
            ignorados++;
        }

        public void registrarErro(String mensagem) {
            erros++;
            mensagensErro.add(mensagem);
        }

        public void registrarAviso(String mensagem) {
            avisos++;
            mensagensAviso.add(mensagem);
        }

        public List<String> getMensagensErro() {
            return mensagensErro;
        }

        public List<String> getMensagensAviso() {
            return mensagensAviso;
        }

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