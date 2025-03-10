package biblioteca.service;


import biblioteca.model.Livro;
import biblioteca.repository.LivroRepository;

import java.util.List;

public class LivroService {
    private LivroRepository repository;

    public LivroService() {
        this.repository = new LivroRepository();
    }

    /**
     * Salva um livro no banco de dados, verificando se já existe
     * um livro com o mesmo título e autor
     *
     * @param livro o livro a ser salvo
     * @throws RuntimeException se já existir um livro com o mesmo título e autor
     */
    public void salvarLivro(Livro livro) throws RuntimeException {
        // A validação agora é feita diretamente no repository
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