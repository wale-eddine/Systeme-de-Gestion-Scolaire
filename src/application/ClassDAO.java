package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les requêtes liées aux Classes (ex: 1A, 2B).
 * Opérations :
 * - Création, modification, suppression de classes [ADMIN]
 * - `getClassesByYear` / `getClassesByLevelAndYear` : Filtre les classes selon l'année sélectionnée [ADMIN] [FILTER]
 * - `assignStudentToClass` / `removeStudentFromClass` : Gère l'affectation des élèves [ADMIN] [ACTION]
 */
public class ClassDAO {
    
    public static void addClass(SchoolClass schoolClass) throws SQLException {
        String sql = "INSERT INTO Classe (nomClasse, capaciteMax, idNiveau, idAnnee) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schoolClass.getNomClasse());
            stmt.setInt(2, schoolClass.getCapaciteMax());
            stmt.setInt(3, schoolClass.getIdNiveau());
            stmt.setInt(4, schoolClass.getIdAnnee());
            stmt.executeUpdate();

            // SQLite JDBC can throw "not implemented" for getGeneratedKeys in some setups.
            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    schoolClass.setIdClasse(rs.getInt(1));
                }
            }
        }
    }

    public static void updateClass(SchoolClass schoolClass) throws SQLException {
        String sql = "UPDATE Classe SET nomClasse=?, capaciteMax=?, idNiveau=?, idAnnee=? WHERE idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schoolClass.getNomClasse());
            stmt.setInt(2, schoolClass.getCapaciteMax());
            stmt.setInt(3, schoolClass.getIdNiveau());
            stmt.setInt(4, schoolClass.getIdAnnee());
            stmt.setInt(5, schoolClass.getIdClasse());
            stmt.executeUpdate();
        }
    }

    public static void deleteClass(int idClasse) throws SQLException {
        String sql = "DELETE FROM Classe WHERE idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClasse);
            stmt.executeUpdate();
        }
    }

    public static SchoolClass getClassById(int idClasse) throws SQLException {
        String sql = "SELECT * FROM Classe WHERE idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClasse);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToClass(rs);
            }
        }
        return null;
    }

    public static List<SchoolClass> getAllClasses() throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM Classe";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                classes.add(mapRowToClass(rs));
            }
        }
        return classes;
    }

    public static List<SchoolClass> getClassesByLevel(int idNiveau) throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM Classe WHERE idNiveau=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapRowToClass(rs));
            }
        }
        return classes;
    }

    public static List<SchoolClass> getClassesByYear(int idAnnee) throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM Classe WHERE idAnnee=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapRowToClass(rs));
            }
        }
        return classes;
    }

    public static List<SchoolClass> getClassesByLevelAndYear(int idNiveau, int idAnnee) throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM Classe WHERE idNiveau=? AND idAnnee=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            stmt.setInt(2, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapRowToClass(rs));
            }
        }
        return classes;
    }

    public static void assignStudentToClass(int idEleve, int idClasse) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM AffectationEleve WHERE idEleve=?");
                 PreparedStatement insertStmt = conn.prepareStatement(
                     "INSERT INTO AffectationEleve (idEleve, idClasse, dateAffectation) VALUES (?, ?, ?)")) {
                deleteStmt.setInt(1, idEleve);
                deleteStmt.executeUpdate();

                insertStmt.setInt(1, idEleve);
                insertStmt.setInt(2, idClasse);
                insertStmt.setObject(3, java.time.LocalDate.now());
                insertStmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void moveStudentToClass(int idEleve, int idClasse) throws SQLException {
        assignStudentToClass(idEleve, idClasse);
    }

    public static void removeStudentFromAllClasses(int idEleve) throws SQLException {
        String sql = "DELETE FROM AffectationEleve WHERE idEleve=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            stmt.executeUpdate();
        }
    }

    public static void removeStudentFromClass(int idEleve, int idClasse) throws SQLException {
        String sql = "DELETE FROM AffectationEleve WHERE idEleve=? AND idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            stmt.setInt(2, idClasse);
            stmt.executeUpdate();
        }
    }

    public static int getStudentCountInClass(int idClasse) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AffectationEleve WHERE idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClasse);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static SchoolClass mapRowToClass(ResultSet rs) throws SQLException {
        return new SchoolClass(
            rs.getInt("idClasse"),
            rs.getString("nomClasse"),
            rs.getInt("capaciteMax"),
            rs.getInt("idNiveau"),
            rs.getInt("idAnnee")
        );
    }
}
