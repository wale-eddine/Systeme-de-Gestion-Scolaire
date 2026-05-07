package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère toutes les opérations de base de données pour les Enseignants.
 * Opérations :
 * - `addTeacher`, `updateTeacher`, `deleteTeacher` : CRUD de base [ADMIN]
 * - `authenticate` : Vérifie les identifiants à la connexion [LOGIC]
 * - `getAllTeachersByYear` : Récupère la liste des professeurs et leurs affectations pour une année [ADMIN] [FILTER]
 * - `getTeacherClasses`, `getTeacherSubjectsByClass` : Trouve où un prof enseigne [TEACHER]
 */
public class TeacherDAO {
    
    public static User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT idEnseignant, nomUtilisateur, nom, prenom, role FROM Enseignant " +
                     "WHERE nomUtilisateur=? AND motDePasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("idEnseignant"),
                    rs.getString("nomUtilisateur"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("role")
                );
            }
        }
        return null;
    }

    public static void addTeacher(Teacher teacher) throws SQLException {
        String sql = "INSERT INTO Enseignant (code, nom, prenom, telephone, nomUtilisateur, motDePasse, role) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'teacher')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String code = teacher.getCode();
            if (code == null || code.isBlank()) {
                code = generateTeacherCode(conn);
                teacher.setCode(code);
            }
            stmt.setString(1, code);
            stmt.setString(2, teacher.getNom());
            stmt.setString(3, teacher.getPrenom());
            stmt.setString(4, teacher.getTelephone());
            stmt.setString(5, teacher.getNomUtilisateur());
            stmt.setString(6, teacher.getMotDePasse());
            stmt.executeUpdate();

            // SQLite JDBC may not implement getGeneratedKeys reliably.
            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    teacher.setIdEnseignant(rs.getInt(1));
                }
            }
        }
    }

    private static String generateTeacherCode(Connection conn) throws SQLException {
        long baseCode = System.currentTimeMillis();
        int attempt = 0;

        while (true) {
            String candidate = attempt == 0 ? "ENS-" + baseCode : "ENS-" + baseCode + "-" + attempt;
            if (!teacherCodeExists(conn, candidate)) {
                return candidate;
            }
            attempt++;
        }
    }

    private static boolean teacherCodeExists(Connection conn, String code) throws SQLException {
        String sql = "SELECT 1 FROM Enseignant WHERE code=? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static void updateTeacher(Teacher teacher) throws SQLException {
        String sql = "UPDATE Enseignant SET code=?, nom=?, prenom=?, telephone=?, " +
                     "nomUtilisateur=?, motDePasse=? WHERE idEnseignant=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacher.getCode());
            stmt.setString(2, teacher.getNom());
            stmt.setString(3, teacher.getPrenom());
            stmt.setString(4, teacher.getTelephone());
            stmt.setString(5, teacher.getNomUtilisateur());
            stmt.setString(6, teacher.getMotDePasse());
            stmt.setInt(7, teacher.getIdEnseignant());
            stmt.executeUpdate();
        }
    }

    public static void deleteTeacher(int idEnseignant) throws SQLException {
        String sql = "DELETE FROM Enseignant WHERE idEnseignant=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.executeUpdate();
        }
    }

    public static Teacher getTeacherById(int idEnseignant) throws SQLException {
        String sql = "SELECT * FROM Enseignant WHERE idEnseignant=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToTeacher(rs);
            }
        }
        return null;
    }

    public static List<Teacher> getAllTeachers() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c.nomClasse) FROM TeacherAssignment ta " +
                     "JOIN Classe c ON ta.idClasse = c.idClasse " +
                     "WHERE ta.idEnseignant = e.idEnseignant" +
                     "), 'Non assigne') AS classesAffectees, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT m.nomMatiere) FROM TeacherAssignment ta " +
                     "JOIN Matiere m ON ta.idMatiere = m.idMatiere " +
                     "WHERE ta.idEnseignant = e.idEnseignant" +
                     "), 'Non assignee') AS matieresAffectees " +
                     "FROM Enseignant e WHERE e.role='teacher' ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                teachers.add(mapRowToTeacher(rs));
            }
        }
        return teachers;
    }

    public static List<Teacher> getAllTeachersByYear(int idAnnee) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT e.*, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT c.nomClasse) FROM TeacherAssignment ta " +
                     "JOIN Classe c ON ta.idClasse = c.idClasse " +
                     "WHERE ta.idEnseignant = e.idEnseignant AND c.idAnnee = ?" +
                     "), 'Non assigne') AS classesAffectees, COALESCE((" +
                     "SELECT GROUP_CONCAT(DISTINCT m.nomMatiere) FROM TeacherAssignment ta " +
                     "JOIN Matiere m ON ta.idMatiere = m.idMatiere " +
                     "JOIN Classe c ON ta.idClasse = c.idClasse " +
                     "WHERE ta.idEnseignant = e.idEnseignant AND c.idAnnee = ?" +
                     "), 'Non assignee') AS matieresAffectees " +
                     "FROM Enseignant e WHERE e.role='teacher' ORDER BY e.nom, e.prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAnnee);
            stmt.setInt(2, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                teachers.add(mapRowToTeacher(rs));
            }
        }
        return teachers;
    }

    public static List<Teacher> getAssignedTeachers() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        for (Teacher teacher : getAllTeachers()) {
            if ("Assigne".equals(teacher.getStatutAffectation())) {
                teachers.add(teacher);
            }
        }
        return teachers;
    }

    public static List<Teacher> getUnassignedTeachers() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        for (Teacher teacher : getAllTeachers()) {
            if ("Non assigne".equals(teacher.getStatutAffectation())) {
                teachers.add(teacher);
            }
        }
        return teachers;
    }

    public static List<SchoolClass> getTeacherClasses(int idEnseignant) throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT DISTINCT c.* FROM Classe c " +
                     "JOIN TeacherAssignment ta ON c.idClasse = ta.idClasse " +
                     "WHERE ta.idEnseignant=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(new SchoolClass(
                    rs.getInt("idClasse"),
                    rs.getString("nomClasse"),
                    rs.getInt("capaciteMax"),
                    rs.getInt("idNiveau"),
                    rs.getInt("idAnnee")
                ));
            }
        }
        return classes;
    }

    public static List<SchoolClass> getTeacherClassesByYear(int idEnseignant, int idAnnee) throws SQLException {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT DISTINCT c.* FROM Classe c " +
                     "JOIN TeacherAssignment ta ON c.idClasse = ta.idClasse " +
                     "WHERE ta.idEnseignant=? AND c.idAnnee=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.setInt(2, idAnnee);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(new SchoolClass(
                    rs.getInt("idClasse"),
                    rs.getString("nomClasse"),
                    rs.getInt("capaciteMax"),
                    rs.getInt("idNiveau"),
                    rs.getInt("idAnnee")
                ));
            }
        }
        return classes;
    }

    public static List<Subject> getTeacherSubjectsByClass(int idEnseignant, int idClasse) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT DISTINCT m.* FROM Matiere m " +
                     "JOIN TeacherAssignment ta ON m.idMatiere = ta.idMatiere " +
                     "WHERE ta.idEnseignant=? AND ta.idClasse=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEnseignant);
            stmt.setInt(2, idClasse);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subjects.add(new Subject(
                    rs.getInt("idMatiere"),
                    rs.getString("nomMatiere"),
                    rs.getInt("idNiveau")
                ));
            }
        }
        return subjects;
    }

    private static Teacher mapRowToTeacher(ResultSet rs) throws SQLException {
        Teacher teacher = new Teacher(
            rs.getInt("idEnseignant"),
            rs.getString("code"),
            rs.getString("nom"),
            rs.getString("prenom"),
            rs.getString("telephone"),
            rs.getString("nomUtilisateur"),
            rs.getString("motDePasse")
        );
        try {
            teacher.setClassesAffectees(rs.getString("classesAffectees"));
        } catch (SQLException ignored) {
            teacher.setClassesAffectees("Non assigne");
        }
        try {
            teacher.setMatieresAffectees(rs.getString("matieresAffectees"));
        } catch (SQLException ignored) {
            teacher.setMatieresAffectees("Non assignee");
        }
        return teacher;
    }
}
