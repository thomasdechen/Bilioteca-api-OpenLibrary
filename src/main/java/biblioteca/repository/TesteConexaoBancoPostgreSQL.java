package biblioteca.repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Classe para testar a conexão com o banco de dados PostgreSQL.
 *
 * Para configurar corretamente o acesso ao banco de dados:
 * 1. Edite o arquivo persistence.xml em src/main/resources/META-INF/
 * 2. Modifique as propriedades de conexão:
 *    - javax.persistence.jdbc.url
 *    - javax.persistence.jdbc.user
 *    - javax.persistence.jdbc.password
 *
 * A aplicação requer acesso a um banco PostgreSQL configurado com a tabela "livros".
 */
public class TesteConexaoBancoPostgreSQL {
    public static void main(String[] args) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("BibliotecaPU");
            EntityManager em = emf.createEntityManager();

            System.out.println("Conexão estabelecida com sucesso!");

            em.close();
            emf.close();
        } catch (Exception e) {
            System.out.println("Erro ao conectar. Configure o 'persistence.xml' com seu usuário e senha do postgreSQL.");
            e.printStackTrace();
        }
    }
}