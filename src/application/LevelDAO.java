package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * [DAO] [DATABASE] [SQL] Gère les Niveaux scolaires (1ère année, 2ème année...).
 * Opérations :
 * - CRUD pour les niveaux [ADMIN]
 * - `initializePredefinedLevels` : S'assure que les 6 niveaux primaires de base existent [LOGIC]
 * Utilisé pour peupler les listes déroulantes de filtrage [UI] [FILTER]
 */
public class LevelDAO {
    
    public static void addLevel(Level level) throws SQLException {
        String sql = "INSERT INTO Niveau (nomNiveau) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, level.getNomNiveau());
            stmt.executeUpdate();

            try (Statement idStmt = conn.createStatement();
                 ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    level.setIdNiveau(rs.getInt(1));
                }
            }
        }
    }

    public static void updateLevel(Level level) throws SQLException {
        String sql = "UPDATE Niveau SET nomNiveau=? WHERE idNiveau=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, level.getNomNiveau());
            stmt.setInt(2, level.getIdNiveau());
            stmt.executeUpdate();
        }
    }

    public static void deleteLevel(int idNiveau) throws SQLException {
        String sql = "DELETE FROM Niveau WHERE idNiveau=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            stmt.executeUpdate();
        }
    }

    public static Level getLevelById(int idNiveau) throws SQLException {
        String sql = "SELECT * FROM Niveau WHERE idNiveau=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToLevel(rs);
            }
        }
        return null;
    }

    public static List<Level> getAllLevels() throws SQLException {
        List<Level> levels = new ArrayList<>();
        String sql = "SELECT * FROM Niveau ORDER BY idNiveau";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                levels.add(mapRowToLevel(rs));
            }
        }
        return levels;
    }

    private static Level mapRowToLevel(ResultSet rs) throws SQLException {
        return new Level(
            rs.getInt("idNiveau"),
            rs.getString("nomNiveau")
        );
    }

    public static void initializePredefinedLevels() throws SQLException {
        String[][] migrations = {
            {"6ème", "6ème année"},
            {"5ème", "5ème année"},
            {"4ème", "4ème année"},
            {"3ème", "3ème année"},
            {"6eme", "6ème année"},
            {"5eme", "5ème année"},
            {"4eme", "4ème année"},
            {"3eme", "3ème année"},
            {"6eme annee", "6ème année"},
            {"5eme annee", "5ème année"},
            {"4eme annee", "4ème année"},
            {"3eme annee", "3ème année"}
        };
        String[] predefinedLevels = {"6ème année", "5ème année", "4ème année", "3ème année"};

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String[] migration : migrations) {
                    Integer oldId = getLevelIdByName(conn, migration[0]);
                    if (oldId == null) {
                        continue;
                    }

                    Integer newId = getLevelIdByName(conn, migration[1]);
                    if (newId == null) {
                        String renameSql = "UPDATE Niveau SET nomNiveau=? WHERE idNiveau=?";
                        try (PreparedStatement renameStmt = conn.prepareStatement(renameSql)) {
                            renameStmt.setString(1, migration[1]);
                            renameStmt.setInt(2, oldId);
                            renameStmt.executeUpdate();
                        }
                    } else if (!oldId.equals(newId)) {
                        String moveClassesSql = "UPDATE Classe SET idNiveau=? WHERE idNiveau=?";
                        try (PreparedStatement moveStmt = conn.prepareStatement(moveClassesSql)) {
                            moveStmt.setInt(1, newId);
                            moveStmt.setInt(2, oldId);
                            moveStmt.executeUpdate();
                        }

                        String deleteOldSql = "DELETE FROM Niveau WHERE idNiveau=?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteOldSql)) {
                            deleteStmt.setInt(1, oldId);
                            deleteStmt.executeUpdate();
                        }
                    }
                }

                for (String levelName : predefinedLevels) {
                    if (getLevelIdByName(conn, levelName) == null) {
                        String insertSql = "INSERT INTO Niveau (nomNiveau) VALUES (?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, levelName);
                            insertStmt.executeUpdate();
                        }
                    }
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

    private static Integer getLevelIdByName(Connection conn, String levelName) throws SQLException {
        String sql = "SELECT idNiveau FROM Niveau WHERE nomNiveau=? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, levelName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("idNiveau");
            }
        }
        return null;
    }
}
