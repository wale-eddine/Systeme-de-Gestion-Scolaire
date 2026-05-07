package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les requêtes liées aux Notes (Grades).
 * Opérations :
 * - `addGrade`, `updateGrade`, `deleteGrade` : Saisie des notes par l'enseignant [TEACHER] [ACTION]
 * - `getClassGradesForSubject` : Affiche les notes de toute la classe pour une matière [TEACHER] [UI]
 * - `getStudentGrades` : Récupère toutes les notes d'un élève pour le calcul du bulletin [ADMIN] [LOGIC]
 */
public class GradeDAO {
    
    public static void addGrade(Grade grade) throws SQLException {
        String sql = "INSERT INTO Note (valeur, trimestre, coefficient, idEleve, idMatiere, idAnnee) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, grade.getValeur());
            stmt.setInt(2, grade.getTrimestre());
            stmt.setInt(3, grade.getCoefficient());
            stmt.setInt(4, grade.getIdEleve());
            stmt.setInt(5, grade.getIdMatiere());
            stmt.setInt(6, grade.getIdAnnee());
            stmt.executeUpdate();

            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    grade.setIdNote(rs.getInt(1));
                }
            }
        }
    }

    public static void updateGrade(Grade grade) throws SQLException {
        String sql = "UPDATE Note SET valeur=?, trimestre=?, coefficient=? WHERE idNote=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, grade.getValeur());
            stmt.setInt(2, grade.getTrimestre());
            stmt.setInt(3, grade.getCoefficient());
            stmt.setInt(4, grade.getIdNote());
            stmt.executeUpdate();
        }
    }

    public static void deleteGrade(int idNote) throws SQLException {
        String sql = "DELETE FROM Note WHERE idNote=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNote);
            stmt.executeUpdate();
        }
    }

    public static Grade getGradeById(int idNote) throws SQLException {
        String sql = "SELECT n.*, e.nom, e.prenom, m.nomMatiere FROM Note n " +
                     "JOIN Eleve e ON n.idEleve = e.idEleve " +
                     "JOIN Matiere m ON n.idMatiere = m.idMatiere " +
                     "WHERE n.idNote=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNote);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToGrade(rs);
            }
        }
        return null;
    }

    public static List<Grade> getStudentGrades(int idEleve, int idAnnee) throws SQLException {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT n.*, m.nomMatiere FROM Note n " +
                     "JOIN Matiere m ON n.idMatiere = m.idMatiere " +
                     "WHERE n.idEleve=? AND n.idAnnee=? ORDER BY n.trimestre, m.nomMatiere";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            stmt.setInt(2, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Grade g = mapRowToGrade(rs);
                grades.add(g);
            }
        }
        return grades;
    }

    public static List<Grade> getClassGradesForSubject(int idClasse, int idMatiere, int idAnnee) throws SQLException {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT DISTINCT n.*, e.nom, e.prenom, m.nomMatiere FROM Note n " +
                     "JOIN Eleve e ON n.idEleve = e.idEleve " +
                     "JOIN Matiere m ON n.idMatiere = m.idMatiere " +
                     "JOIN AffectationEleve ae ON e.idEleve = ae.idEleve " +
                     "WHERE ae.idClasse=? AND n.idMatiere=? AND n.idAnnee=? " +
                     "ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClasse);
            stmt.setInt(2, idMatiere);
            stmt.setInt(3, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                grades.add(mapRowToGrade(rs));
            }
        }
        return grades;
    }

    public static Grade getStudentGradeForSubjectAndTrimester(int idEleve, int idMatiere, 
                                                               int idAnnee, int trimestre) throws SQLException {
        String sql = "SELECT n.*, m.nomMatiere FROM Note n " +
                     "JOIN Matiere m ON n.idMatiere = m.idMatiere " +
                     "WHERE n.idEleve=? AND n.idMatiere=? AND n.idAnnee=? AND n.trimestre=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEleve);
            stmt.setInt(2, idMatiere);
            stmt.setInt(3, idAnnee);
            stmt.setInt(4, trimestre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToGrade(rs);
            }
        }
        return null;
    }

    private static Grade mapRowToGrade(ResultSet rs) throws SQLException {
        Grade g = new Grade(
            rs.getInt("idNote"),
            rs.getDouble("valeur"),
            rs.getInt("trimestre"),
            rs.getInt("coefficient"),
            rs.getInt("idEleve"),
            rs.getInt("idMatiere"),
            rs.getInt("idAnnee")
        );
        try {
            g.setNomMatiere(rs.getString("nomMatiere"));
        } catch (SQLException e) {
            // nomMatiere might not be in result set
        }
        try {
            String nom = rs.getString("nom");
            String prenom = rs.getString("prenom");
            g.setNomEleve(prenom + " " + nom);
        } catch (SQLException e) {
            // nom/prenom might not be in result set
        }
        return g;
    }
}
