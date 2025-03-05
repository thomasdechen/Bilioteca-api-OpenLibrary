package biblioteca.service;

import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;

import java.util.List;

public class LivroService {
    private LivroRepository repository;

    public LivroService() {
        this.repository = new LivroRepository();
    }

    public void salvarLivro(Livro livro) {
        // Normalizar ISBN: remover espaços em branco e converter string vazia para null
        String isbn = livro.getIsbn() != null ? livro.getIsbn().trim() : null;
        livro.setIsbn(isbn);

        if (isbn != null && !isbn.isEmpty()) {
            // Verificar se já existe um livro com este ISBN ou ISBN semelhante
            Livro livroExistente = repository.buscarPorIsbn(isbn);

            if (livroExistente != null) {
                if (livro.getId() == null || !livro.getId().equals(livroExistente.getId())) {
                    throw new RuntimeException("Livro com este ISBN já existe: " + isbn);
                }
            }
        }

        if (livro.getTitulo() == null || livro.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("Título do livro é obrigatório");
        }

        if (livro.getAutores() == null || livro.getAutores().trim().isEmpty()) {
            throw new IllegalArgumentException("Autores do livro são obrigatórios");
        }

        repository.salvar(livro);
    }

    public Livro buscarPorId(Long id) {
        return repository.buscarPorId(id);
    }

    public Livro buscarPorIsbn(String isbn) {
        return repository.buscarPorIsbn(isbn);
    }

    public List<Livro> buscarPorCampo(String campo, String valor) {
        return repository.buscarPorCampo(campo, valor);
    }

    public List<Livro> listarTodos() {
        return repository.listarTodos();
    }

    public void excluir(Long id) {
        repository.excluir(id);
    }

}