package biblioteca.ui;

import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;
import biblioteca.service.LivroService;
import biblioteca.service.OpenLibraryService;
import biblioteca.util.FormatacaoDatas;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * Interface gráfica principal
 */
public class BibliotecaApp extends JFrame {
    private LivroRepository repository;
    private JTable tabelaLivros;
    private JTextField campoIsbn, campoPesquisa;
    private JComboBox<String> campoBusca;
    private DefaultTableModel modeloTabela;
    private JDialog dialogoProgresso;
    private JProgressBar barraProgresso;

    // Variável para controlar se há uma janela de cadastro aberta
    private LivroCadastro cadastroAtivo = null;

    public BibliotecaApp() {
        repository = new LivroRepository();
        initComponents();
        inicializarDialogoProgresso();
        carregarLivros();

        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setTitle("Catálogo de Livros");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel painelBusca = new JPanel();
        campoBusca = new JComboBox<>(new String[]{"Título", "Autor", "ISBN", "Editora", "Data Publicação"});
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

        JButton botaoImportar = new JButton("Importar CSV");
        botaoImportar.addActionListener(e -> abrirTelaImportacao());

        painelCadastro.add(new JLabel("ISBN:"));
        painelCadastro.add(campoIsbn);
        painelCadastro.add(botaoCadastrarIsbn);
        painelCadastro.add(botaoIncluir);
        painelCadastro.add(botaoEditar);
        painelCadastro.add(botaoExcluir);
        painelCadastro.add(botaoImportar);

        // Tabela de Livros
        String[] colunas = {"ID", "Título", "Autor", "ISBN", "Editora", "Data Publicação", "Livros Semelhantes(edições)"};
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

    private void inicializarDialogoProgresso() {
        dialogoProgresso = new JDialog(this, "Carregando...", true);
        dialogoProgresso.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogoProgresso.setSize(300, 100);
        dialogoProgresso.setLayout(new BorderLayout());
        dialogoProgresso.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel mensagem = new JLabel("Buscando informações do livro...");
        panel.add(mensagem, BorderLayout.NORTH);

        barraProgresso = new JProgressBar();
        barraProgresso.setIndeterminate(true);
        panel.add(barraProgresso, BorderLayout.CENTER);

        dialogoProgresso.add(panel);
    }

    private void mostrarDialogoProgresso() {
        Timer timer = new Timer(200, e -> {
            if (!dialogoProgresso.isVisible()) {
                dialogoProgresso.setVisible(true);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void esconderDialogoProgresso() {
        dialogoProgresso.setVisible(false);
    }

    private void abrirTelaImportacao() {
        Importacao telaImportacao = new Importacao(this);
        telaImportacao.setLocationRelativeTo(this);
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

        String campoRepositorio;
        switch (campo) {
            case "Título":
                campoRepositorio = "titulo";
                break;
            case "Autor":
                campoRepositorio = "autores";
                break;
            case "ISBN":
                campoRepositorio = "isbn";
                break;
            case "Editora":
                campoRepositorio = "editora";
                break;
            case "Data Publicação":
                campoRepositorio = "dataPublicacao";
                break;
            default:
                campoRepositorio = "titulo";
                break;
        }

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

    /**
     * Nesta funcionalidade foi utilizado Thread por estar puxando dados de uma API externa,
     * sendo assim, essa requisição pode demorar em alguns momentos, por instabilidade do servidor.
     * Utilizando o Thread, o usuário pode continuar mexendo na aplicação, pois o Swing por padrão roda em
     * um único thread.
     */
    private void cadastrarPorIsbn() {
        String isbn = campoIsbn.getText().trim();
        if (isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, digite um ISBN.");
            return;
        }

        LivroService livroService = new LivroService();
        if (livroService.buscarPorIsbn(isbn) != null) {
            JOptionPane.showMessageDialog(this,
                    "Este ISBN já está cadastrado no sistema.",
                    "ISBN Duplicado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        mostrarDialogoProgresso();

        new Thread(() -> {
            try {
                JsonObject dadosLivro = OpenLibraryService.buscarInformacoesPorIsbn(isbn);
                Livro livro = OpenLibraryService.converterParaLivro(dadosLivro, isbn);

                if (livroService.buscarPorIsbn(isbn) != null) {
                    SwingUtilities.invokeLater(() -> {
                        esconderDialogoProgresso();
                        JOptionPane.showMessageDialog(BibliotecaApp.this,
                                "Este ISBN já foi cadastrado por outro usuário.",
                                "ISBN Duplicado",
                                JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }

                try {
                    livroService.salvarLivro(livro);

                    SwingUtilities.invokeLater(() -> {
                        esconderDialogoProgresso();
                        carregarLivros();
                        JOptionPane.showMessageDialog(BibliotecaApp.this, "Livro cadastrado com sucesso!");
                        campoIsbn.setText("");
                    });
                } catch (RuntimeException e) {
                    SwingUtilities.invokeLater(() -> {
                        esconderDialogoProgresso();
                        if (e.getMessage() != null && e.getMessage().contains("ISBN já existe")) {
                            JOptionPane.showMessageDialog(BibliotecaApp.this,
                                    "Este ISBN já está cadastrado no sistema.",
                                    "ISBN Duplicado",
                                    JOptionPane.WARNING_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(BibliotecaApp.this,
                                    "Erro ao salvar o livro: " + e.getMessage(),
                                    "Erro",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    esconderDialogoProgresso();

                    String mensagem;
                    if (e.getMessage() == null || e.getMessage().isEmpty()) {
                        mensagem = "Erro desconhecido ao buscar informações do livro.";
                    } else if (e.getMessage().contains("not found") || e.getMessage().contains("não encontrado")) {
                        mensagem = "Não foi possível encontrar um livro com este ISBN.";
                    } else {
                        mensagem = "Erro ao buscar informações do livro: " + e.getMessage();
                    }

                    JOptionPane.showMessageDialog(BibliotecaApp.this,
                            mensagem,
                            "Erro",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void abrirCadastroLivro(Livro livro) {
        // Verificar se já existe uma janela de cadastro aberta
        if (cadastroAtivo != null && cadastroAtivo.isVisible()) {
            cadastroAtivo.toFront();
            cadastroAtivo.requestFocus();
            JOptionPane.showMessageDialog(this,
                    "Uma janela de edição já está aberta. Finalize-a antes de abrir outra.",
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        cadastroAtivo = new LivroCadastro(this, livro);

        cadastroAtivo.setLocationRelativeTo(this);

        cadastroAtivo.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                cadastroAtivo = null;
            }
        });

        cadastroAtivo.setVisible(true);
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
                    OpenLibraryService.encerrarRecursos();
                } catch (Exception e) {
                    System.err.println("Erro ao fechar recursos: " + e.getMessage());
                }
            }));
        });
    }
}