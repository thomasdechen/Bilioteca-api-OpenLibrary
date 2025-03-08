package biblioteca.ui;

import biblioteca.service.ImportacaoService;
import biblioteca.service.ImportacaoService.ImportacaoResultado;
import com.opencsv.exceptions.CsvValidationException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Interface gráfica para importação de arquivos CSV
 */
public class Importacao extends JFrame {
    private BibliotecaApp framePai;
    private ImportacaoService importacaoService;
    private JTextArea logArea;
    private JButton botaoSelecionar;
    private JTextField campoArquivo;

    public Importacao(BibliotecaApp framePai) {
        this.framePai = framePai;
        this.importacaoService = new ImportacaoService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Importação de Livros");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Painel Superior - Seleção de arquivo
        JPanel painelSelecao = new JPanel(new BorderLayout(5, 0));
        painelSelecao.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel labelArquivo = new JLabel("Arquivo CSV:");
        campoArquivo = new JTextField();
        campoArquivo.setEditable(false);

        botaoSelecionar = new JButton("Selecionar");
        botaoSelecionar.addActionListener(e -> selecionarArquivo());

        painelSelecao.add(labelArquivo, BorderLayout.WEST);
        painelSelecao.add(campoArquivo, BorderLayout.CENTER);
        painelSelecao.add(botaoSelecionar, BorderLayout.EAST);

        // Área de Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.append("Instruções para importação de CSV:\n");
        logArea.append("1. O arquivo CSV deve ter cabeçalho com os nomes das colunas.\n");
        logArea.append("2. Colunas obrigatórias: 'titulo' e 'autores'.\n");
        logArea.append("3. Colunas opcionais: 'isbn', 'editora', 'data_publicacao'.\n");
        logArea.append("4. Se o ISBN já existir, o livro será atualizado.\n");
        logArea.append("5. Selecione um arquivo CSV e clique em 'Importar'.\n\n");
        logArea.append("Exemplo de arquivo CSV:\n");
        logArea.append("titulo,autores,isbn,editora,data_publicacao\n\n");
        logArea.append("\"Vetores e Geometria Analitica\",\"Paulo Winterle\",\"9788574801711\",\"Pearson\",\"2009-03-01\"\n");
        logArea.append("\"O Cortiço\",\"Aluísio Azevedo\",\"9788506055342\",\"Ática\",\"1890-01-01\"\n\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Painel Inferior - Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelBotoes.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton botaoImportar = new JButton("Importar");
        botaoImportar.addActionListener(e -> importarArquivo());

        JButton botaoFechar = new JButton("Fechar");
        botaoFechar.addActionListener(e -> dispose());

        painelBotoes.add(botaoImportar);
        painelBotoes.add(botaoFechar);

        // Adiciona os componentes ao frame
        add(painelSelecao, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(painelBotoes, BorderLayout.SOUTH);

        // Centraliza na tela
        setLocationRelativeTo(null);
    }

    private void selecionarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecione o arquivo CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));

        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();
            campoArquivo.setText(arquivo.getAbsolutePath());
        }
    }

    private void importarArquivo() {
        String caminhoArquivo = campoArquivo.getText();
        if (caminhoArquivo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, selecione um arquivo CSV para importar",
                    "Atenção",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Desabilita botões durante importação
        botaoSelecionar.setEnabled(false);

        // Executa importação em thread separada para não travar a UI
        SwingWorker<ImportacaoResultado, String> worker = new SwingWorker<>() {
            @Override
            protected ImportacaoResultado doInBackground() throws Exception {
                publish("Iniciando importação do arquivo: " + caminhoArquivo);

                try {
                    return importacaoService.importarCSV(caminhoArquivo);
                } catch (IOException e) {
                    publish("Erro ao ler arquivo: " + e.getMessage());
                    throw e;
                } catch (CsvValidationException e) {
                    publish("Erro no formato do CSV: " + e.getMessage());
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String mensagem : chunks) {
                    logArea.append(mensagem + "\n");
                }
            }

            @Override
            protected void done() {
                botaoSelecionar.setEnabled(true);

                try {
                    ImportacaoResultado resultado = get();
                    logArea.append("\n" + resultado.toString() + "\n");

                    // Mostra erros se houver
                    if (resultado.erros > 0) {
                        logArea.append("\nDetalhes dos erros:\n");
                        for (String erro : resultado.mensagensErro) {
                            logArea.append("- " + erro + "\n");
                        }
                    }

                    // Mostra avisos se houver
                    if (resultado.avisos > 0) {
                        logArea.append("\nAvisos:\n");
                        for (String aviso : resultado.mensagensAviso) {
                            logArea.append("- " + aviso + "\n");
                        }
                    }

                    // Notifica o frame principal para atualizar a listagem
                    if (framePai != null) {
                        framePai.notificarMudanca();
                    }

                    // Exibe mensagem de conclusão
                    String mensagem = String.format("Importação concluída! %d inseridos, %d atualizados",
                            resultado.inseridos, resultado.atualizados);
                    JOptionPane.showMessageDialog(Importacao.this, mensagem);

                } catch (Exception e) {
                    logArea.append("\nA importação falhou: " + e.getMessage() + "\n");
                }
            }
        };

        worker.execute();
    }
}
