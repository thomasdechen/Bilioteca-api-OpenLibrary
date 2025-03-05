package biblioteca.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "livros")
public class Livro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String autores;

    @Column(name = "data_publicacao")
    private LocalDate dataPublicacao;

    @Column(unique = true, nullable = false)
    private String isbn;

    @Column
    private String editora;

    @ElementCollection
    @CollectionTable(name = "livros_semelhantes", joinColumns = @JoinColumn(name = "livro_id"))
    @Column(name = "livro_semelhante")
    private List<String> livrosSemelhantes;

    // Construtores
    public Livro() {}

    public Livro(String titulo, String autores, LocalDate dataPublicacao,
                 String isbn, String editora, List<String> livrosSemelhantes) {
        this.titulo = titulo;
        this.autores = autores;
        this.dataPublicacao = dataPublicacao;
        this.isbn = isbn;
        this.editora = editora;
        this.livrosSemelhantes = livrosSemelhantes;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public LocalDate getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(LocalDate dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public List<String> getLivrosSemelhantes() {
        return livrosSemelhantes;
    }

    public void setLivrosSemelhantes(List<String> livrosSemelhantes) {
        this.livrosSemelhantes = livrosSemelhantes;
    }

    @Override
    public String toString() {
        return titulo + " - " + autores;
    }
}