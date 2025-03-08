package biblioteca.repository;

import biblioteca.model.Livro;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LivroRepository {
    private static final EntityManagerFactory emf;

    static {
        try {
            emf = Persistence.createEntityManagerFactory("BibliotecaPU");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("Erro ao criar EntityManagerFactory: " + e.getMessage());
        }
    }

    public void salvar(Livro livro) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            if (livro.getId() == null) {
                em.persist(livro);
            } else {
                em.merge(livro);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Erro ao salvar livro", e);
        } finally {
            em.close();
        }
    }


    public Livro buscarPorIsbn(String isbn) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT l FROM Livro l WHERE l.isbn = :isbn";

            List<Livro> resultados = em.createQuery(jpql, Livro.class)
                    .setParameter("isbn", isbn)
                    .getResultList();

            return resultados.isEmpty() ? null : resultados.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public Livro buscarPorId(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Livro.class, id);
        } finally {
            em.close();
        }
    }

    public List<Livro> buscarPorCampo(String campo, String valor) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT l FROM Livro l WHERE ";
            switch (campo) {
                case "titulo":
                    jpql += "LOWER(l.titulo) LIKE LOWER(:valor)";
                    break;
                case "autores":
                    jpql += "LOWER(l.autores) LIKE LOWER(:valor)";
                    break;
                case "isbn":
                    jpql += "(:valor IS NULL OR l.isbn = :valor)";
                    break;
                case "editora":
                    jpql += "LOWER(l.editora) LIKE LOWER(:valor)";
                    break;
                default:
                    // Evitar SQL Injection quando se constrói consultas JPQL
                    if (!Arrays.asList("titulo", "autores", "isbn", "editora").contains(campo)) {
                        throw new IllegalArgumentException("Campo de busca inválido: " + campo);
                    }
            }

            // Para campos de texto (não ISBN), usar busca parcial
            if (!campo.equals("isbn")) {
                return em.createQuery(jpql, Livro.class)
                        .setParameter("valor", "%" + valor + "%")
                        .getResultList();
            } else {
                return em.createQuery(jpql, Livro.class)
                        .setParameter("valor", valor)
                        .getResultList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public void excluir(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Livro livro = em.find(Livro.class, id);
            if (livro != null) {
                em.remove(em.contains(livro) ? livro : em.merge(livro));
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Erro ao excluir livro", e);
        } finally {
            em.close();
        }
    }

    public List<Livro> listarTodos() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT l FROM Livro l", Livro.class)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public static void closeEntityManagerFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}