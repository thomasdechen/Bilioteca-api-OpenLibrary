package biblioteca.repository;

import biblioteca.model.Livro;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;

public class LivroRepositoryTest {
    private LivroRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new LivroRepository();
    }



    @Test
    public void testSalvarEBuscarLivro() {
        Livro livro = new Livro(
                "Arquitetura Limpa",
                "Robert C. Martin",
                LocalDate.of(2018, 1, 1),
                "978-8550804577",
                "Alta Books",
                Arrays.asList("Engenharia de Software")
        );

        repository.salvar(livro);

        Livro livroEncontrado = repository.buscarPorIsbn("978-8550804577");
        assertNotNull(livroEncontrado);
        assertEquals("Arquitetura Limpa", livroEncontrado.getTitulo());
    }

    @Test
    public void testBuscarPorCampo() {
        Livro livro1 = new Livro(
                "Java para Iniciantes",
                "Herbert Schildt",
                LocalDate.of(2015, 1, 1),
                "978-8576089637",
                "Bookman",
                null
        );

        Livro livro2 = new Livro(
                "Java Efetivo",
                "Joshua Bloch",
                LocalDate.of(2019, 1, 1),
                "978-8550804842",
                "Alta Books",
                null
        );

        repository.salvar(livro1);
        repository.salvar(livro2);

        var livrosEncontrados = repository.buscarPorCampo("titulo", "Java");
        assertFalse(livrosEncontrados.isEmpty());
        assertEquals(2, livrosEncontrados.size());
    }
}