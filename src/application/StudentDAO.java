package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les requêtes liées aux Élèves.
 * Opérations :
 * - CRUD pour les élèves (Ajout, Modification, Suppression) [ADMIN]
 * - `getStudentsByClass` : Liste les élèves pour la saisie des notes [TEACHER] [UI]
 * - `getUnassignedStudents` / `getAssignedStudents` : Pour l'interface d'affectation [ADMIN] [FILTER]
 */
public class StudentDAO {
    
    public static void addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO Eleve (nom, prenom, dateNaissance, adresse, telephoneParent) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, student.getNom());
            stmt.setString(2, student.getPrenom());
            stmt.setObject(3, student.getDateNaissance());
            stmt.setString(4, student.getAdresse());
            stmt.setString(5, student.getTelephoneParent());
            stmt.executeUpdate();

            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    student.setIdEleve(rs.getInt(1));
                }
            }
        }
    }

    public static void updateStudent(Student student) throws SQLException {
        String sql = "UPDATE Eleve SET nom=?, prenom=?, dateNaissance=?, adresse=?, telephoneParent=? " +
                     "WHERE idEleve=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, student.getNom());
            stmt.setString(2, student.getPrenom());
            stmt.setObject(3, student.getDateNaissance());
            stmt.setString(4, student.getAdresse());
            stmt.setString(5, student.getTelephoneParent());
            stmt.setInt(6, student.getIdEleve());
            stmt.executeUpdate();
        }
    }

    public static void deleteStudent(int idEleve) throws SQLException {
        String sql = "DELETE FROM Eleve WHERE idEleve=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            stmt.executeUpdate();
        }
    }

    public static Student getStudentById(int idEleve) throws SQLException {
        String sql = "SELECT * FROM Eleve WHERE idEleve=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToStudent(rs);
            }
        }
        return null;
    }

    public static List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c.nomClasse) FROM AffectationEleve ae2 " +
                     "JOIN Classe c ON ae2.idClasse = c.idClasse " +
                     "WHERE ae2.idEleve = e.idEleve" +
                     "), 'Non affecté') AS classesAffectees " +
                     "FROM Eleve e ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        }
        return students;
    }

    public static List<Student> getStudentsByClass(int idClasse) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT DISTINCT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c2.nomClasse) FROM AffectationEleve ae2 " +
                     "JOIN Classe c2 ON ae2.idClasse = c2.idClasse " +
                     "WHERE ae2.idEleve = e.idEleve" +
                     "), 'Non affecté') AS classesAffectees FROM Eleve e " +
                     "JOIN AffectationEleve ae ON e.idEleve = ae.idEleve " +
                     "WHERE ae.idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClasse);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        }
        return students;
    }

    public static List<Student> getUnassignedStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT DISTINCT e.*, 'Non affecté' AS classesAffectees FROM Eleve e " +
                     "LEFT JOIN AffectationEleve ae ON e.idEleve = ae.idEleve " +
                     "WHERE ae.idEleve IS NULL " +
                     "ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        }
        return students;
    }

    public static List<Student> getAssignedStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT DISTINCT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c2.nomClasse) FROM AffectationEleve ae2 " +
                     "JOIN Classe c2 ON ae2.idClasse = c2.idClasse " +
                     "WHERE ae2.idEleve = e.idEleve" +
                     "), 'Non affecté') AS classesAffectees FROM Eleve e " +
                     "JOIN AffectationEleve ae ON e.idEleve = ae.idEleve " +
                     "ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        }
        return students;
    }

    public static List<Student> getStudentsByLevel(int idNiveau) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT DISTINCT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c2.nomClasse) FROM AffectationEleve ae2 " +
                     "JOIN Classe c2 ON ae2.idClasse = c2.idClasse " +
                     "WHERE ae2.idEleve = e.idEleve" +
                     "), 'Non affecté') AS classesAffectees FROM Eleve e " +
                     "JOIN AffectationEleve ae ON e.idEleve = ae.idEleve " +
                     "JOIN Classe c ON ae.idClasse = c.idClasse " +
                     "WHERE c.idNiveau=? " +
                     "ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                students.add(mapRowToStudent(rs));
            }
        }
        return students;
    }

    private static Student mapRowToStudent(ResultSet rs) throws SQLException {
        LocalDate dateNaissance = null;
        String dateText = rs.getString("dateNaissance");
        if (dateText != null && !dateText.isBlank()) {
            dateNaissance = LocalDate.parse(dateText);
        }

        Student student = new Student(
            rs.getInt("idEleve"),
            rs.getString("nom"),
            rs.getString("prenom"),
            dateNaissance,
            rs.getString("adresse"),
            rs.getString("telephoneParent")
        );
        try {
            student.setClassesAffectees(rs.getString("classesAffectees"));
        } catch (SQLException ignored) {
            student.setClassesAffectees("Non affecté");
        }
        return student;
    }
}
