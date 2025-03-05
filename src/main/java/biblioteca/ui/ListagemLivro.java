package biblioteca.ui;

import biblioteca.model.Livro;
import biblioteca.service.LivroService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ListagemLivro extends JFrame {
    private LivroService livroService;
    private JTable tabelaLivros;
    private DefaultTableModel modeloTabela;

    public ListagemLivro() {
        livroService = new LivroService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Listagem de Livros");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Modelo da tabela
        String[] colunas = {"ID", "Título", "Autores", "ISBN", "Editora", "Data Publicação"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaLivros = new JTable(modeloTabela);

        JScrollPane scrollPane = new JScrollPane(tabelaLivros);
        add(scrollPane, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel();
        JButton botaoIncluir = new JButton("Incluir");
        JButton botaoEditar = new JButton("Editar");
        JButton botaoExcluir = new JButton("Excluir");
        JButton botaoAtualizar = new JButton("Atualizar");

        botaoIncluir.addActionListener(e -> abrirCadastroLivro(null));
        botaoEditar.addActionListener(e -> editarLivroSelecionado());
        botaoExcluir.addActionListener(e -> excluirLivroSelecionado());
        botaoAtualizar.addActionListener(e -> atualizarListagem());

        painelBotoes.add(botaoIncluir);
        painelBotoes.add(botaoEditar);
        painelBotoes.add(botaoExcluir);
        painelBotoes.add(botaoAtualizar);

        add(painelBotoes, BorderLayout.SOUTH);

        // Carregar dados iniciais
        atualizarListagem();
    }

    private void atualizarListagem() {
        modeloTabela.setRowCount(0);
        List<Livro> livros = livroService.listarTodos();

        for (Livro livro : livros) {
            modeloTabela.addRow(new Object[]{
                    livro.getId(),
                    livro.getTitulo(),
                    livro.getAutores(),
                    livro.getIsbn(),
                    livro.getEditora(),
                    livro.getDataPublicacao()
            });
        }
    }

    private void abrirCadastroLivro(Livro livro) {
        LivroCadastro cadastroFrame = new LivroCadastro(null, livro);
        cadastroFrame.setVisible(true);
    }

    private void editarLivroSelecionado() {
        int linhaSelecionada = tabelaLivros.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um livro para editar.");
            return;
        }

        Long id = (Long) modeloTabela.getValueAt(linhaSelecionada, 0);
        Livro livro = livroService.buscarPorId(id);
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
            livroService.excluir(id);
            atualizarListagem();
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ListagemLivro().setVisible(true);
        });
    }
}