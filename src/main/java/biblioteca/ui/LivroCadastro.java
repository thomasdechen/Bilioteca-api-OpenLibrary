package biblioteca.ui;

import biblioteca.model.Livro;
import biblioteca.service.LivroService;
import biblioteca.service.OpenLibraryService;
import biblioteca.util.FormatacaoDatas;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class LivroCadastro extends JFrame {
    private LivroService livroService;
    private Livro livro;
    private BibliotecaApp framePai;

    private JTextField campoTitulo;
    private JTextField campoAutores;
    private JTextField campoIsbn;
    private JTextField campoEditora;
    private JTextField campoDataPublicacao;

    // Padrões para validação do lado do usuário
    private static final Pattern PATTERN_APENAS_SIMBOLOS = Pattern.compile("^[^a-zA-Z0-9]+$");
    private static final Pattern PATTERN_APENAS_NUMEROS = Pattern.compile("^[0-9]+$");
    private static final Pattern PATTERN_ISBN_10 = Pattern.compile("^\\d{9}[\\dXx]$");
    private static final Pattern PATTERN_ISBN_13 = Pattern.compile("^97[89]\\d{10}$");

    public LivroCadastro(BibliotecaApp framePai, Livro livro) {
        this.framePai = framePai;
        this.livroService = new LivroService();
        this.livro = livro != null ? livro : new Livro();

        initComponents();
    }

    private void initComponents() {
        setTitle(livro.getId() == null ? "Incluir Livro" : "Editar Livro");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(6, 2, 10, 10));

        add(new JLabel("Título:"));
        campoTitulo = new JTextField(livro.getTitulo() != null ? livro.getTitulo() : "");
        add(campoTitulo);

        add(new JLabel("Autores:"));
        campoAutores = new JTextField(livro.getAutores() != null ? livro.getAutores() : "");
        add(campoAutores);

        add(new JLabel("ISBN:"));
        campoIsbn = new JTextField(livro.getIsbn() != null ? livro.getIsbn() : "");
        add(campoIsbn);

        add(new JLabel("Editora:"));
        campoEditora = new JTextField(livro.getEditora() != null ? livro.getEditora() : "");
        add(campoEditora);

        add(new JLabel("Data Publicação (DD/MM/AAAA ou AAAA):"));
        campoDataPublicacao = new JTextField(
                livro.getDataPublicacao() != null
                        ? FormatacaoDatas.formatarParaExibicao(livro.getDataPublicacao())
                        : ""
        );
        add(campoDataPublicacao);

        JButton botaoBuscarIsbn = new JButton("Buscar por ISBN");
        botaoBuscarIsbn.addActionListener(e -> buscarPorIsbn());
        add(botaoBuscarIsbn);

        JButton botaoSalvar = new JButton("Salvar");
        botaoSalvar.addActionListener(e -> salvarLivro());
        add(botaoSalvar);
    }

    private void buscarPorIsbn() {
        String isbn = campoIsbn.getText().trim();
        if (isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite um ISBN");
            return;
        }

        isbn = isbn.replace("-", "").replace(" ", "");
        if (!PATTERN_ISBN_10.matcher(isbn).matches() && !PATTERN_ISBN_13.matcher(isbn).matches()) {
            JOptionPane.showMessageDialog(this,
                    "ISBN inválido. Deve ter 10 ou 13 dígitos no formato correto.",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            var dadosLivro = OpenLibraryService.buscarInformacoesPorIsbn(isbn);
            Livro livroEncontrado = OpenLibraryService.converterParaLivro(dadosLivro, isbn);

            campoTitulo.setText(livroEncontrado.getTitulo());
            campoAutores.setText(livroEncontrado.getAutores());
            campoEditora.setText(livroEncontrado.getEditora());

            if (livroEncontrado.getDataPublicacao() != null) {
                campoDataPublicacao.setText(
                        FormatacaoDatas.formatarParaExibicao(livroEncontrado.getDataPublicacao())
                );
            }

            livro.setLivrosSemelhantes(livroEncontrado.getLivrosSemelhantes());

            JOptionPane.showMessageDialog(this,
                    "Informações do livro obtidas com sucesso!\n" +
                            "Título: " + livroEncontrado.getTitulo() + "\n",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao buscar livro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validarCampos() {
        // Validar título
        String titulo = campoTitulo.getText().trim();
        if (titulo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Título do livro é obrigatório",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoTitulo.requestFocus();
            return false;
        }

        if (titulo.length() > 255) {
            JOptionPane.showMessageDialog(this,
                    "Título do livro não pode exceder 255 caracteres",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoTitulo.requestFocus();
            return false;
        }

        if (PATTERN_APENAS_SIMBOLOS.matcher(titulo).matches()) {
            JOptionPane.showMessageDialog(this,
                    "Título do livro não pode conter apenas símbolos",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoTitulo.requestFocus();
            return false;
        }

        // Validar autores
        String autores = campoAutores.getText().trim();
        if (autores.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Autores do livro são obrigatórios",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoAutores.requestFocus();
            return false;
        }

        if (autores.length() > 255) {
            JOptionPane.showMessageDialog(this,
                    "Nome dos autores não pode exceder 255 caracteres",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoAutores.requestFocus();
            return false;
        }

        if (PATTERN_APENAS_SIMBOLOS.matcher(autores).matches()) {
            JOptionPane.showMessageDialog(this,
                    "Nome dos autores não pode conter apenas símbolos",
                    "Erro de Validação",
                    JOptionPane.ERROR_MESSAGE);
            campoAutores.requestFocus();
            return false;
        }

        // Validar ISBN
        String isbn = campoIsbn.getText().trim();
        if (!isbn.isEmpty()) {
            isbn = isbn.replace("-", "").replace(" ", "");
            if (!PATTERN_ISBN_10.matcher(isbn).matches() && !PATTERN_ISBN_13.matcher(isbn).matches()) {
                JOptionPane.showMessageDialog(this,
                        "ISBN inválido. Deve ter 10 ou 13 dígitos no formato correto.",
                        "Erro de Validação",
                        JOptionPane.ERROR_MESSAGE);
                campoIsbn.requestFocus();
                return false;
            }
        }

        // Validar data
        String dataStr = campoDataPublicacao.getText().trim();
        if (!dataStr.isEmpty()) {
            try {
                LocalDate data = FormatacaoDatas.analisarEntradaUsuario(dataStr);
                LocalDate hoje = LocalDate.now();

                if (data.isAfter(hoje)) {
                    JOptionPane.showMessageDialog(this,
                            "A data de publicação não pode ser uma data futura",
                            "Erro de Validação",
                            JOptionPane.ERROR_MESSAGE);
                    campoDataPublicacao.requestFocus();
                    return false;
                }

                LocalDate dataMinimaRazoavel = LocalDate.of(1000, 1, 1);
                if (data.isBefore(dataMinimaRazoavel)) {
                    JOptionPane.showMessageDialog(this,
                            "A data de publicação inserida é muito antiga para ser válida",
                            "Erro de Validação",
                            JOptionPane.ERROR_MESSAGE);
                    campoDataPublicacao.requestFocus();
                    return false;
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Data inválida. Use o formato DD/MM/AAAA ou apenas o ano (AAAA)",
                        "Erro de Validação",
                        JOptionPane.ERROR_MESSAGE);
                campoDataPublicacao.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void salvarLivro() {
        // Validar campos antes de prosseguir
        if (!validarCampos()) {
            return;
        }

        try {
            // Atualizar objeto
            livro.setTitulo(campoTitulo.getText().trim());
            livro.setAutores(campoAutores.getText().trim());

            // Normalizar ISBN
            String isbn = campoIsbn.getText().trim();
            if (!isbn.isEmpty()) {
                isbn = isbn.replace("-", "").replace(" ", "");
            } else {
                isbn = null;
            }
            livro.setIsbn(isbn);

            livro.setEditora(campoEditora.getText().trim());

            // Converter data
            if (!campoDataPublicacao.getText().trim().isEmpty()) {
                try {
                    livro.setDataPublicacao(FormatacaoDatas.analisarEntradaUsuario(campoDataPublicacao.getText()));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Data inválida. Use o formato DD/MM/AAAA ou apenas o ano (AAAA)");
                    return;
                }
            } else {
                livro.setDataPublicacao(null);
            }

            try {
                livroService.salvarLivro(livro);

                if (framePai != null) {
                    framePai.notificarMudanca();
                }

                JOptionPane.showMessageDialog(this, "Livro salvo com sucesso!");
                dispose();
            } catch (RuntimeException e) {
                if (e.getMessage().contains("mesmo título e autor")) {
                    // Mensagem específica para o caso de duplicação de título e autor
                    JOptionPane.showMessageDialog(this,
                            "Não é permitido cadastrar livros com o mesmo título e autor.",
                            "Erro de Validação",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    // Outras exceções
                    JOptionPane.showMessageDialog(this,
                            "Erro ao salvar: " + e.getMessage(),
                            "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }
}