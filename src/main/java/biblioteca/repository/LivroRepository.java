package biblioteca.repository;

import biblioteca.model.Livro;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    /**
     * Verifica se já existe um livro com o mesmo título e autor
     * @param livro O livro a ser verificado
     * @return True se já existir, False caso contrário
     */
    public boolean livroJaExiste(Livro livro) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT COUNT(l) FROM Livro l WHERE " +
                    "LOWER(l.titulo) = LOWER(:titulo) AND " +
                    "LOWER(l.autores) = LOWER(:autores)";

            if (livro.getId() != null) {
                jpql += " AND l.id != :id";
            }

            javax.persistence.Query query = em.createQuery(jpql);
            query.setParameter("titulo", livro.getTitulo().toLowerCase());
            query.setParameter("autores", livro.getAutores().toLowerCase());

            if (livro.getId() != null) {
                query.setParameter("id", livro.getId());
            }

            Long count = (Long) query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            em.close();
        }
    }

    /**
     * Salva um livro no banco de dados, verificando se já existe
     * um livro com o mesmo título e autor
     */
    public void salvar(Livro livro) throws RuntimeException {
        if (livroJaExiste(livro)) {
            throw new RuntimeException("Já existe um livro cadastrado com o mesmo título e autor.");
        }

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


    /**
     * Funções para cada tipo de busca na barra de pesquisa.
     */
    public Livro buscarPorIsbn(String isbn) {
        EntityManager em = emf.createEntityManager();
        try {
            if (isbn == null || isbn.isEmpty()) {
                return null;
            }

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

            if ("dataPublicacao".equals(campo)) {
                return buscarPorData(valor);
            }

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
                    if (!Arrays.asList("titulo", "autores", "isbn", "editora", "dataPublicacao").contains(campo)) {
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

    public List<Livro> buscarPorData(String dataString) {
        EntityManager em = emf.createEntityManager();
        try {
            LocalDate data = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try {
                data = LocalDate.parse(dataString, formatter);
            } catch (DateTimeParseException e) {
                try {
                    data = LocalDate.parse(dataString);
                } catch (DateTimeParseException ex) {
                    try {
                        int ano = Integer.parseInt(dataString);
                        return buscarPorAno(ano);
                    } catch (NumberFormatException nex) {
                        // Se tudo falhar, faz busca parcial na string da data
                        String jpql = "SELECT l FROM Livro l WHERE CAST(l.dataPublicacao AS string) LIKE :valor";
                        return em.createQuery(jpql, Livro.class)
                                .setParameter("valor", "%" + dataString + "%")
                                .getResultList();
                    }
                }
            }

            String jpql = "SELECT l FROM Livro l WHERE l.dataPublicacao = :data";
            return em.createQuery(jpql, Livro.class)
                    .setParameter("data", data)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Livro> buscarPorAno(int ano) {
        EntityManager em = emf.createEntityManager();
        try {
            LocalDate inicioAno = LocalDate.of(ano, 1, 1);
            LocalDate fimAno = LocalDate.of(ano, 12, 31);

            String jpql = "SELECT l FROM Livro l WHERE l.dataPublicacao BETWEEN :inicio AND :fim";
            return em.createQuery(jpql, Livro.class)
                    .setParameter("inicio", inicioAno)
                    .setParameter("fim", fimAno)
                    .getResultList();
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