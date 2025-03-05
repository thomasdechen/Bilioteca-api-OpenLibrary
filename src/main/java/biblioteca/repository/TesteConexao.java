package biblioteca.repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TesteConexao {
    public static void main(String[] args) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("BibliotecaPU");
            EntityManager em = emf.createEntityManager();

            System.out.println("Conexão estabelecida com sucesso!");

            em.close();
            emf.close();
        } catch (Exception e) {
            System.err.println("Erro ao conectar:");
            e.printStackTrace();
        }
    }
}