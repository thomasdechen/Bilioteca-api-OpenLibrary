package biblioteca.ui;

import biblioteca.service.ImportacaoService;
import biblioteca.service.ImportacaoService.ImportacaoResultado;
import com.opencsv.exceptions.CsvValidationException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Interface gráfica para importação de arquivos CSV
 */
public class Importacao extends JFrame {
    private BibliotecaApp framePai;
    private ImportacaoService importacaoService;
    private JTextArea logArea;
    private JButton botaoSelecionar;
    private JButton botaoImportar;
    private JTextField campoArquivo;
    private JProgressBar barraProgresso;
    private JCheckBox checkboxLogDetalhado;

    public Importacao(BibliotecaApp framePai) {
        this.framePai = framePai;
        this.importacaoService = new ImportacaoService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Importação de Livros");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

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

        JPanel painelCentral = new JPanel(new BorderLayout(10, 10));
        painelCentral.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Área de Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        adicionarInstrucoesLog();

        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel painelOpcoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxLogDetalhado = new JCheckBox("Log detalhado", true);
        painelOpcoes.add(checkboxLogDetalhado);

        barraProgresso = new JProgressBar();
        barraProgresso.setIndeterminate(true);
        barraProgresso.setVisible(false);
        barraProgresso.setStringPainted(true);
        barraProgresso.setString("Importando...");

        painelCentral.add(scrollPane, BorderLayout.CENTER);
        painelCentral.add(painelOpcoes, BorderLayout.NORTH);
        painelCentral.add(barraProgresso, BorderLayout.SOUTH);

        // Painel Inferior - Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelBotoes.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        botaoImportar = new JButton("Importar");
        botaoImportar.addActionListener(e -> importarArquivo());

        JButton botaoLimparLog = new JButton("Limpar Log");
        botaoLimparLog.addActionListener(e -> limparLog());

        JButton botaoFechar = new JButton("Fechar");
        botaoFechar.addActionListener(e -> dispose());

        painelBotoes.add(botaoImportar);
        painelBotoes.add(botaoLimparLog);
        painelBotoes.add(botaoFechar);

        add(painelSelecao, BorderLayout.NORTH);
        add(painelCentral, BorderLayout.CENTER);
        add(painelBotoes, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void adicionarInstrucoesLog() {
        logArea.append("[" + getCurrentTime() + "] Instruções para importação de CSV:\n");
        logArea.append("1. O arquivo CSV deve ter cabeçalho com os nomes das colunas.\n");
        logArea.append("2. Colunas obrigatórias: 'titulo' e 'autores'.\n");
        logArea.append("3. Colunas opcionais: 'isbn', 'editora', 'data_publicacao'.\n");
        logArea.append("4. Se o ISBN já existir, o livro será atualizado se houver diferenças.\n");
        logArea.append("5. Livros sem ISBN serão verificados por título e autor para evitar duplicações.\n");
        logArea.append("6. ISBNs duplicados no arquivo: apenas o primeiro será processado.\n\n");
        logArea.append("Exemplo de arquivo CSV:\n");
        logArea.append("titulo,autores,isbn,editora,data_publicacao\n");
        logArea.append("\"Vetores e Geometria Analitica\",\"Paulo Winterle\",\"9788574801711\",\"Pearson\",\"2009-03-01\"\n");
        logArea.append("\"O Cortiço\",\"Aluísio Azevedo\",\"9788506055342\",\"Ática\",\"1890-01-01\"\n\n");
        logArea.append("Selecione um arquivo CSV e clique em 'Importar'.\n\n");
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    private void limparLog() {
        logArea.setText("");
        adicionarInstrucoesLog();
    }

    private void selecionarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecione o arquivo CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));

        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();
            campoArquivo.setText(arquivo.getAbsolutePath());
            logArea.append("[" + getCurrentTime() + "] Arquivo selecionado: " + arquivo.getName() + "\n");
        }
    }


    /**
     * Importa dados do arquivo CSV escolhido pelo usuário.
     *
     * Usa SwingWorker que é como uma thread especial para o Swing. Isso faz com que
     * a importação rode em segundo plano, assim o usuário pode ver o progresso na
     * tela sem que a aplicação fique travada.
     *
     * O que acontece durante a importação:
     * - Desabilita os botões da tela
     * - Mostra a barra de progresso
     * - Vai adicionando mensagens no log para o usuário acompanhar(necessário muitos dados para ver o processo)
     * - No final, mostra quantos livros foram importados, atualizados/ignorados ou se deu erro
     */

    private void importarArquivo() {
        String caminhoArquivo = campoArquivo.getText();
        if (caminhoArquivo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, selecione um arquivo CSV para importar",
                    "Atenção",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Desabilita botões e mostra barra de progresso durante importação
        setComponentesAtivos(false);
        barraProgresso.setVisible(true);

        // Executa importação em thread separada para não travar a UI
        SwingWorker<ImportacaoResultado, String> worker = new SwingWorker<>() {
            @Override
            protected ImportacaoResultado doInBackground() throws Exception {
                publish("[" + getCurrentTime() + "] Iniciando importação do arquivo: " + caminhoArquivo);

                try {
                    return importacaoService.importarCSV(caminhoArquivo);
                } catch (IOException e) {
                    publish("[" + getCurrentTime() + "] Erro ao ler arquivo: " + e.getMessage());
                    throw e;
                } catch (CsvValidationException e) {
                    publish("[" + getCurrentTime() + "] Erro no formato do CSV: " + e.getMessage());
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String mensagem : chunks) {
                    logArea.append(mensagem + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                barraProgresso.setVisible(false);
                setComponentesAtivos(true);

                try {
                    ImportacaoResultado resultado = get();
                    logArea.append("\n[" + getCurrentTime() + "] " + resultado.toString() + "\n");

                    if (resultado.erros > 0 && checkboxLogDetalhado.isSelected()) {
                        logArea.append("\nDetalhes dos erros:\n");
                        int contador = 0;
                        for (String erro : resultado.getMensagensErro()) {
                            logArea.append((++contador) + ". " + erro + "\n");
                        }
                    }

                    if (resultado.avisos > 0 && checkboxLogDetalhado.isSelected()) {
                        logArea.append("\nAvisos:\n");
                        int contador = 0;
                        for (String aviso : resultado.getMensagensAviso()) {
                            logArea.append((++contador) + ". " + aviso + "\n");
                        }
                    }

                    logArea.setCaretPosition(logArea.getDocument().getLength());

                    if (framePai != null) {
                        framePai.notificarMudanca();
                    }

                    String mensagem = String.format("Importação concluída!\n- %d livros inseridos\n- %d livros atualizados\n- %d livros ignorados\n- %d erros\n- %d avisos",
                            resultado.inseridos, resultado.atualizados, resultado.ignorados, resultado.erros, resultado.avisos);

                    String titulo = "Importação Concluída";
                    int tipo = JOptionPane.INFORMATION_MESSAGE;

                    if (resultado.erros > 0) {
                        titulo = "Importação Concluída com Erros";
                        tipo = JOptionPane.WARNING_MESSAGE;
                    }

                    JOptionPane.showMessageDialog(Importacao.this, mensagem, titulo, tipo);

                } catch (Exception e) {
                    logArea.append("\n[" + getCurrentTime() + "] A importação falhou: " + e.getMessage() + "\n");
                    JOptionPane.showMessageDialog(Importacao.this,
                            "A importação falhou: " + e.getMessage(),
                            "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void setComponentesAtivos(boolean ativo) {
        botaoSelecionar.setEnabled(ativo);
        botaoImportar.setEnabled(ativo);
        checkboxLogDetalhado.setEnabled(ativo);
    }
}