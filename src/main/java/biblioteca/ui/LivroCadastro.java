package biblioteca.ui;

import biblioteca.model.Livro;
import biblioteca.service.LivroService;
import biblioteca.service.OpenLibraryService;
import biblioteca.util.FormatacaoDatas;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class LivroCadastro extends JFrame {
    private LivroService livroService;
    private Livro livro;
    private BibliotecaApp framePai;

    private JTextField campoTitulo;
    private JTextField campoAutores;
    private JTextField campoIsbn;
    private JTextField campoEditora;
    private JTextField campoDataPublicacao;

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

    private void salvarLivro() {
        try {
            // Atualizar objeto
            livro.setTitulo(campoTitulo.getText());
            livro.setAutores(campoAutores.getText());
            livro.setIsbn(campoIsbn.getText());
            livro.setEditora(campoEditora.getText());

            // Converter data
            if (!campoDataPublicacao.getText().trim().isEmpty()) {
                try {
                    livro.setDataPublicacao(FormatacaoDatas.analisarEntradaUsuario(campoDataPublicacao.getText()));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Data inválida. Use o formato DD/MM/AAAA ou apenas o ano (AAAA)");
                    return;
                }
            }

            livroService.salvarLivro(livro);

            if (framePai != null) {
                framePai.notificarMudanca();
            }

            JOptionPane.showMessageDialog(this, "Livro salvo com sucesso!");
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }


}