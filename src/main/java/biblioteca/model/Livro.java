package biblioteca.model;

import javax.persistence.*;
import java.time.LocalDate;

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

    @Column(unique = true)
    private String isbn;

    @Column
    private String editora;

    @Column(name = "livros_semelhantes")
    private Integer livrosSemelhantes;

    public Livro() {}

    public Livro(String titulo, String autores, LocalDate dataPublicacao,
                 String isbn, String editora) {
        this.titulo = titulo;
        this.autores = autores;
        this.dataPublicacao = dataPublicacao;
        this.isbn = isbn;
        this.editora = editora;
    }

    public Integer getLivrosSemelhantes() {
        return livrosSemelhantes;
    }

    public void setLivrosSemelhantes(Integer livrosSemelhantes) {
        this.livrosSemelhantes = livrosSemelhantes;
    }

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

    @Override
    public String toString() {
        return titulo + " - " + autores;
    }
}