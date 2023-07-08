package primenumber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class Main {

    //a computacao paralela ocorre aqui
    private static final int nThreads = Runtime.getRuntime().availableProcessors();

    public static File selecionaDiretorioRaiz() {
        JFileChooser janelaSelecao = new JFileChooser(".");
        //janelaSelecao.setControlButtonsAreShown(false);

        //conf. do filtro de selecao
        janelaSelecao.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File arquivo) {
                return arquivo.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Diretório";
            }
        });

        janelaSelecao.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //avaliando a acao do usuario na selecao da pasta de inicio da busca
        int acao = janelaSelecao.showOpenDialog(null);

        if (acao == JFileChooser.APPROVE_OPTION) {
            return janelaSelecao.getSelectedFile();
        } else {
            return null;
        }
    }

//percorre recursivamente todos os arquivos e subpastas do diretorio e retorna uma lista com os arquivos encontrados
    public static List<File> obterArquivos(File diretorio) {
        List<File> arquivos = new ArrayList<>();

        if (diretorio.isDirectory()) {
            File[] listaArquivos = diretorio.listFiles();
            if (listaArquivos != null) {
                for (File arquivo : listaArquivos) {
                    if (arquivo.isFile()) {
                        arquivos.add(arquivo);
                    } else if (arquivo.isDirectory()) {
                        arquivos.addAll(obterArquivos(arquivo));
                    }
                }
            }
        }

        return arquivos;
    }

    //calcula se os numeros encontrados sao primos
    public static boolean isPrimo(int number) {
        if (number < 2) {
            return false;
        }
        for (int i = 2; i <= number / 2; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    //armazena o maior numero primo e garante sincronizacao correta das threads
    public static class maiorPrimoInfo {

        private int maiorPrimo = 0;
        private File arquivoMaiorPrimo = null;

        public synchronized void atualizar(int primo, File arquivo) {
            if (primo > maiorPrimo) {
                maiorPrimo = primo;
                arquivoMaiorPrimo = arquivo;
            }
        }

        public int getMaiorPrimo() {
            return maiorPrimo;
        }

        public File getArquivoMaiorPrimo() {
            return arquivoMaiorPrimo;
        }
    }

//recebe uma lista de arquivos e percorre cada arquivo
    public static int achaMaiorPrimo(List<File> files) {
        maiorPrimoInfo maiorPrimoInfo = new maiorPrimoInfo();

        //utiliza a execucao paralela
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                executor.submit(() -> {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            //separa os numeros com espaço e virgula dos valores que nao sao int
                            String[] numbers = line.split("[,\\s]+");
                            for (String numberStr : numbers) {
                                try {
                                    int number = Integer.parseInt(numberStr);
                                    if (isPrimo(number)) {
                                        maiorPrimoInfo.atualizar(number, file);
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignora valores que não forem int
                                }
                            }
                        }
                    } catch (IOException e) {
                    }
                });
            }
        }
        //interrompe a computacao pararela ao encontrar o resultado
        //impede looping
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
        }
        File arquivoMaiorPrimo = maiorPrimoInfo.getArquivoMaiorPrimo();
        if (arquivoMaiorPrimo != null) {
            System.out.println("O maior número primo encontrado está no arquivo: " + arquivoMaiorPrimo.getAbsolutePath());
        }
        return maiorPrimoInfo.getMaiorPrimo();
    }

    public static void main(String[] args) {

        //selecao de um diretorio para iniciar a busca
        File pastaInicial = selecionaDiretorioRaiz();

        if (pastaInicial == null) {
            JOptionPane.showMessageDialog(null, "Você deve selecionar uma pasta para o processamento",
                    "Selecione o arquivo", JOptionPane.WARNING_MESSAGE);
        } else {
            //retorna o numero primo encontrado e o diretorio onde esse numero se encontra
            List<File> files = obterArquivos(pastaInicial);
            int maiorPrimo = achaMaiorPrimo(files);
            JOptionPane.showMessageDialog(null, "O maior número primo encontrado é: " + maiorPrimo,
                    "Resultado", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
