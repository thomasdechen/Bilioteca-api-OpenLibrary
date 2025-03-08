package biblioteca.ui;

import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;
import biblioteca.service.OpenLibraryService;
import biblioteca.util.FormatacaoDatas;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class BibliotecaApp extends JFrame {
    private LivroRepository repository;
    private JTable tabelaLivros;
    private JTextField campoIsbn, campoPesquisa;
    private JComboBox<String> campoBusca;
    private DefaultTableModel modeloTabela;

    public BibliotecaApp() {
        repository = new LivroRepository();
        initComponents();
        carregarLivros();
    }

    private void initComponents() {
        setTitle("Catálogo de Livros");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel painelBusca = new JPanel();
        campoBusca = new JComboBox<>(new String[]{"Título", "Autor", "ISBN"});
        campoPesquisa = new JTextField(20);
        JButton botaoBuscar = new JButton("Buscar");
        botaoBuscar.addActionListener(e -> buscarLivros());
        JButton botaoMostrarTodos = new JButton("Mostrar Todos");
        botaoMostrarTodos.addActionListener(e -> carregarLivros());

        painelBusca.add(new JLabel("Buscar por:"));
        painelBusca.add(campoBusca);
        painelBusca.add(campoPesquisa);
        painelBusca.add(botaoBuscar);
        painelBusca.add(botaoMostrarTodos);

        // Painel de Cadastro ISBN
        JPanel painelCadastro = new JPanel();
        campoIsbn = new JTextField(15);
        JButton botaoCadastrarIsbn = new JButton("Cadastrar por ISBN");
        botaoCadastrarIsbn.addActionListener(e -> cadastrarPorIsbn());

        JButton botaoIncluir = new JButton("Incluir Livro");
        botaoIncluir.addActionListener(e -> abrirCadastroLivro(null));

        JButton botaoEditar = new JButton("Editar");
        botaoEditar.addActionListener(e -> editarLivroSelecionado());

        JButton botaoExcluir = new JButton("Excluir");
        botaoExcluir.addActionListener(e -> excluirLivroSelecionado());

        // Novo botão de importação
        JButton botaoImportar = new JButton("Importar CSV");
        botaoImportar.addActionListener(e -> abrirTelaImportacao());

        painelCadastro.add(new JLabel("ISBN:"));
        painelCadastro.add(campoIsbn);
        painelCadastro.add(botaoCadastrarIsbn);
        painelCadastro.add(botaoIncluir);
        painelCadastro.add(botaoEditar);
        painelCadastro.add(botaoExcluir);
        painelCadastro.add(botaoImportar); // Adiciona o botão de importação

        // Tabela de Livros
        String[] colunas = {"ID", "Título", "Autor", "ISBN", "Editora", "Data Publicação", "Livros Semelhantes"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaLivros = new JTable(modeloTabela);
        JScrollPane scrollPane = new JScrollPane(tabelaLivros);

        add(painelBusca, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(painelCadastro, BorderLayout.SOUTH);
    }

    private void abrirTelaImportacao() {
        Importacao telaImportacao = new Importacao(this);
        telaImportacao.setVisible(true);
    }

    private void carregarLivros() {
        modeloTabela.setRowCount(0);
        List<Livro> livros = repository.listarTodos();

        livros.sort(Comparator.comparing(Livro::getId));

        for (Livro livro : livros) {
            modeloTabela.addRow(new Object[]{
                    livro.getId(),
                    livro.getTitulo(),
                    livro.getAutores(),
                    livro.getIsbn(),
                    livro.getEditora(),
                    livro.getDataPublicacao() != null ?
                            FormatacaoDatas.formatarParaExibicao(livro.getDataPublicacao()) : "",
                    livro.getLivrosSemelhantes()
            });
        }
    }

    private void buscarLivros() {
        String campo = campoBusca.getSelectedItem().toString();
        String valor = campoPesquisa.getText().trim();

        if (valor.isEmpty()) {
            carregarLivros();
            return;
        }

        String campoRepositorio = campo.equals("Título") ? "titulo" :
                campo.equals("Autor") ? "autores" :
                        campo.equals("Editora") ? "editora" :
                                "isbn";

        List<Livro> livros = repository.buscarPorCampo(campoRepositorio, valor);

        modeloTabela.setRowCount(0);

        if (livros.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nenhum livro encontrado para: " + valor,
                    "Busca",
                    JOptionPane.INFORMATION_MESSAGE);
            carregarLivros();
            return;
        }

        // Adicionar livros encontrados à tabela
        for (Livro livro : livros) {
            modeloTabela.addRow(new Object[]{
                    livro.getId(),
                    livro.getTitulo(),
                    livro.getAutores(),
                    livro.getIsbn(),
                    livro.getEditora(),
                    livro.getDataPublicacao() != null ?
                            FormatacaoDatas.formatarParaExibicao(livro.getDataPublicacao()) : "",
                    livro.getLivrosSemelhantes()
            });
        }
    }

    private void cadastrarPorIsbn() {
        String isbn = campoIsbn.getText().trim();
        if (isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, digite um ISBN.");
            return;
        }

        try {
            JsonObject dadosLivro = OpenLibraryService.buscarInformacoesPorIsbn(isbn);
            Livro livro = OpenLibraryService.converterParaLivro(dadosLivro, isbn);
            repository.salvar(livro);
            carregarLivros();
            JOptionPane.showMessageDialog(this, "Livro cadastrado com sucesso!");
            campoIsbn.setText("");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao buscar informações do livro: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirCadastroLivro(Livro livro) {
        LivroCadastro cadastroFrame = new LivroCadastro(this, livro);
        cadastroFrame.setVisible(true);
    }

    private void editarLivroSelecionado() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um livro para editar.");
            return;
        }

        Long id = (Long) modeloTabela.getValueAt(linhaSelecionada, 0);
        Livro livro = repository.buscarPorId(id);
        abrirCadastroLivro(livro);
    }

    private void excluirLivroSelecionado() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um livro para excluir.");
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir este livro?",
                "Confirmação",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacao == JOptionPane.YES_OPTION) {
            Long id = (Long) modeloTabela.getValueAt(linhaSelecionada, 0);
            repository.excluir(id);
            carregarLivros();
        }
    }

    public void notificarMudanca() {
        carregarLivros();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BibliotecaApp app = new BibliotecaApp();
            app.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    LivroRepository.closeEntityManagerFactory();
                } catch (Exception e) {
                    System.err.println("Erro ao fechar EntityManagerFactory: " + e.getMessage());
                }
            }));
        });
    }
}