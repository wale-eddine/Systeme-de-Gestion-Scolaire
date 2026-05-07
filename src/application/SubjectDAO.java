package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les requêtes liées aux Matières.
 * Opérations :
 * - CRUD des matières [ADMIN]
 * - `assignTeacherToClassSubject` : Lie un professeur à une matière dans une classe précise [ADMIN] [ACTION]
 * - `isTeacherAssignedToClassSubject` : Vérifie si la matière est déjà prise (Empêche les doublons) [LOGIC]
 */
public class SubjectDAO {
    
    public static void addSubject(Subject subject) throws SQLException {
        String sql = "INSERT INTO Matiere (nomMatiere, idNiveau) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getNomMatiere());
            stmt.setInt(2, subject.getIdNiveau());
            stmt.executeUpdate();

            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    subject.setIdMatiere(rs.getInt(1));
                }
            }
        }
    }

    public static void updateSubject(Subject subject) throws SQLException {
        String sql = "UPDATE Matiere SET nomMatiere=?, idNiveau=? WHERE idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getNomMatiere());
            stmt.setInt(2, subject.getIdNiveau());
            stmt.setInt(3, subject.getIdMatiere());
            stmt.executeUpdate();
        }
    }

    public static void deleteSubject(int idMatiere) throws SQLException {
        String sql = "DELETE FROM Matiere WHERE idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMatiere);
            stmt.executeUpdate();
        }
    }

    public static Subject getSubjectById(int idMatiere) throws SQLException {
        String sql = "SELECT * FROM Matiere WHERE idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMatiere);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToSubject(rs);
            }
        }
        return null;
    }

    public static List<Subject> getAllSubjects() throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM Matiere ORDER BY idNiveau, nomMatiere";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                subjects.add(mapRowToSubject(rs));
            }
        }
        return subjects;
    }

    public static List<Subject> getSubjectsByLevel(int idNiveau) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM Matiere WHERE idNiveau=? ORDER BY nomMatiere";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subjects.add(mapRowToSubject(rs));
            }
        }
        return subjects;
    }

    public static void assignTeacherToClassSubject(int idEnseignant, int idClasse, int idMatiere) throws SQLException {
        // Check if another teacher is already assigned to this subject in this class
        String checkSql = "SELECT COUNT(*) as count FROM TeacherAssignment WHERE idClasse=? AND idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, idClasse);
            checkStmt.setInt(2, idMatiere);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                // Get the current teacher name for better error message
                String getTeacherSql = "SELECT nom, prenom FROM TeacherAssignment ta " +
                                      "JOIN Enseignant e ON ta.idEnseignant = e.idEnseignant " +
                                      "WHERE ta.idClasse=? AND ta.idMatiere=?";
                try (PreparedStatement getTeacherStmt = conn.prepareStatement(getTeacherSql)) {
                    getTeacherStmt.setInt(1, idClasse);
                    getTeacherStmt.setInt(2, idMatiere);
                    ResultSet teacherRs = getTeacherStmt.executeQuery();
                    if (teacherRs.next()) {
                        String existingTeacher = teacherRs.getString("prenom") + " " + teacherRs.getString("nom");
                            throw new SQLException("Matière: " + existingTeacher);
                    }
                }
                    throw new SQLException("Matière déjà assignée.");
            }
        }
        
        // Check if this specific teacher is already assigned to this subject in this class
        String dupCheckSql = "SELECT COUNT(*) as count FROM TeacherAssignment WHERE idEnseignant=? AND idClasse=? AND idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement dupStmt = conn.prepareStatement(dupCheckSql)) {
            dupStmt.setInt(1, idEnseignant);
            dupStmt.setInt(2, idClasse);
            dupStmt.setInt(3, idMatiere);
            ResultSet rs = dupStmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                    throw new SQLException("Déjà assigné.");
            }
        }
        
        String sql = "INSERT INTO TeacherAssignment (idEnseignant, idClasse, idMatiere) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.setInt(2, idClasse);
            stmt.setInt(3, idMatiere);
            stmt.executeUpdate();
        }
    }

    public static void removeTeacherFromClassSubject(int idEnseignant, int idClasse, int idMatiere) throws SQLException {
        String sql = "DELETE FROM TeacherAssignment WHERE idEnseignant=? AND idClasse=? AND idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.setInt(2, idClasse);
            stmt.setInt(3, idMatiere);
            stmt.executeUpdate();
        }
    }

    public static boolean isTeacherAssignedToClassSubject(int idEnseignant, int idClasse, int idMatiere) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TeacherAssignment WHERE idEnseignant=? AND idClasse=? AND idMatiere=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.setInt(2, idClasse);
            stmt.setInt(3, idMatiere);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static Subject mapRowToSubject(ResultSet rs) throws SQLException {
        return new Subject(
            rs.getInt("idMatiere"),
            rs.getString("nomMatiere"),
            rs.getInt("idNiveau")
        );
    }
}
