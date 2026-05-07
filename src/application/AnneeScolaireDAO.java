package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les Années Scolaires (ex: 2023-2024).
 * Opérations :
 * - `addAnneeScolaire` : Crée une nouvelle année et copie la structure des classes de l'année précédente [ADMIN] [ACTION]
 * - `setAsActive` : Change l'année en cours de visualisation [ADMIN] [LOGIC]
 * - `deleteAnneeScolaire` : Supprime l'année et en cascade toutes les données associées (Notes, Affectations) [ADMIN] [WARNING]
 */
public class AnneeScolaireDAO {

    public static void addAnneeScolaire(AnneeScolaire annee) throws SQLException {
        String sql = "INSERT INTO AnneeScolaire (nom, estActive) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, annee.getNom());
            stmt.setBoolean(2, annee.isEstActive());
            stmt.executeUpdate();
            
            AnneeScolaire inserted = getAnneeScolaireByNom(annee.getNom());
            if (inserted != null) {
                DatabaseConnection.seedClassesForYear(inserted.getIdAnnee());
            }

            if (annee.isEstActive() && inserted != null) {
                setAsActive(inserted.getIdAnnee());
            }
        }
    }

    public static void setAsActive(int idAnnee) throws SQLException {
        String resetSql = "UPDATE AnneeScolaire SET estActive = 0";
        String setActiveSql = "UPDATE AnneeScolaire SET estActive = 1 WHERE idAnnee = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement resetStmt = conn.createStatement();
                 PreparedStatement setActiveStmt = conn.prepareStatement(setActiveSql)) {
                
                resetStmt.executeUpdate(resetSql);
                
                setActiveStmt.setInt(1, idAnnee);
                setActiveStmt.executeUpdate();
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static List<AnneeScolaire> getAllAnneesScolaires() throws SQLException {
        List<AnneeScolaire> list = new ArrayList<>();
        String sql = "SELECT * FROM AnneeScolaire ORDER BY nom DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new AnneeScolaire(
                    rs.getInt("idAnnee"),
                    rs.getString("nom"),
                    rs.getBoolean("estActive")
                ));
            }
        }
        return list;
    }

    public static AnneeScolaire getActiveAnneeScolaire() throws SQLException {
        String sql = "SELECT * FROM AnneeScolaire WHERE estActive = 1 LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new AnneeScolaire(
                    rs.getInt("idAnnee"),
                    rs.getString("nom"),
                    rs.getBoolean("estActive")
                );
            }
        }
        return null;
    }

    public static AnneeScolaire getAnneeScolaireById(int idAnnee) throws SQLException {
        String sql = "SELECT * FROM AnneeScolaire WHERE idAnnee = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAnnee);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AnneeScolaire(
                        rs.getInt("idAnnee"),
                        rs.getString("nom"),
                        rs.getBoolean("estActive")
                    );
                }
            }
        }
        return null;
    }

    private static AnneeScolaire getAnneeScolaireByNom(String nom) throws SQLException {
        String sql = "SELECT * FROM AnneeScolaire WHERE nom = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AnneeScolaire(
                        rs.getInt("idAnnee"),
                        rs.getString("nom"),
                        rs.getBoolean("estActive")
                    );
                }
            }
        }
        return null;
    }

    public static void deleteAnneeScolaire(int idAnnee) throws SQLException {
        // Prevent deleting the active year if we can check easily, 
        // but we'll do the check in the UI to be safe.
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Note WHERE idAnnee = ?")) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Inscription WHERE idAnnee = ?")) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM TeacherAssignment WHERE idClasse IN (SELECT idClasse FROM Classe WHERE idAnnee = ?)"
                )) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM AffectationEleve WHERE idClasse IN (SELECT idClasse FROM Classe WHERE idAnnee = ?)"
                )) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Bulletin WHERE idAnnee = ?")) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Classe WHERE idAnnee = ?")) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM AnneeScolaire WHERE idAnnee = ?")) {
                    stmt.setInt(1, idAnnee);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
