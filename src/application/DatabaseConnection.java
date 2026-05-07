package application;

import java.sql.*;

/**
 * [DAO] [DATABASE] [SQL] Gestionnaire central de la connexion à la base de données SQLite.
 * Rôles principaux :
 * - `getConnection()` : Ouvre et retourne la connexion à "school_management.db".
 * - `initializeDatabase()` : Crée toutes les tables si elles n'existent pas.
 * - `initializeSampleData()` : Injecte des données de test (Admin, Profs, Élèves).
 * Utilisé par : Toutes les autres classes DAO.
 */
public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:school_management.db";
    private static Connection connection;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(DB_URL);
            initializeDatabase(conn);
            ensureSubjectsSeeded(conn);
            connection = conn;
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.out.println("Error: SQLite JDBC driver not found or DB initialization failed");
        }
    }

    private static void initializeDatabase(Connection conn) throws SQLException {
        String[] sqlCommands = {
            // Levels Table
            "CREATE TABLE IF NOT EXISTS Niveau (" +
            "idNiveau INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nomNiveau TEXT NOT NULL UNIQUE)",

            // Classes Table
            "CREATE TABLE IF NOT EXISTS Classe (" +
            "idClasse INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nomClasse TEXT NOT NULL, " +
            "capaciteMax INTEGER DEFAULT 20, " +
            "idNiveau INTEGER NOT NULL, " +
            "idAnnee INTEGER NOT NULL, " +
            "FOREIGN KEY(idNiveau) REFERENCES Niveau(idNiveau), " +
            "FOREIGN KEY(idAnnee) REFERENCES AnneeScolaire(idAnnee))",

            // Subjects Table
            "CREATE TABLE IF NOT EXISTS Matiere (" +
            "idMatiere INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nomMatiere TEXT NOT NULL UNIQUE, " +
            "idNiveau INTEGER NOT NULL, " +
            "FOREIGN KEY(idNiveau) REFERENCES Niveau(idNiveau))",

            // Students Table
            "CREATE TABLE IF NOT EXISTS Eleve (" +
            "idEleve INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nom TEXT NOT NULL, " +
            "prenom TEXT NOT NULL, " +
            "dateNaissance DATE, " +
            "adresse TEXT, " +
            "telephoneParent TEXT)",

            // School Year Table
            "CREATE TABLE IF NOT EXISTS AnneeScolaire (" +
            "idAnnee INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nom TEXT NOT NULL, " +
            "estActive BOOLEAN DEFAULT 0)",

            // Student Inscription Table
            "CREATE TABLE IF NOT EXISTS Inscription (" +
            "idInscription INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "idEleve INTEGER NOT NULL, " +
            "idAnnee INTEGER NOT NULL, " +
            "niveauEtude TEXT NOT NULL, " +
            "UNIQUE(idEleve, idAnnee), " +
            "FOREIGN KEY(idEleve) REFERENCES Eleve(idEleve), " +
            "FOREIGN KEY(idAnnee) REFERENCES AnneeScolaire(idAnnee))",

            // Student Assignment to Classes
            "CREATE TABLE IF NOT EXISTS AffectationEleve (" +
            "idAffectation INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "idEleve INTEGER NOT NULL, " +
            "idClasse INTEGER NOT NULL, " +
            "dateAffectation DATE NOT NULL, " +
            "FOREIGN KEY(idEleve) REFERENCES Eleve(idEleve), " +
            "FOREIGN KEY(idClasse) REFERENCES Classe(idClasse))",

            // Teachers Table
            "CREATE TABLE IF NOT EXISTS Enseignant (" +
            "idEnseignant INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "code TEXT NOT NULL UNIQUE, " +
            "nom TEXT NOT NULL, " +
            "prenom TEXT NOT NULL, " +
            "telephone TEXT, " +
            "nomUtilisateur TEXT NOT NULL UNIQUE, " +
            "motDePasse TEXT NOT NULL, " +
            "role TEXT DEFAULT 'teacher')",

            // Teacher Assignment to Classes and Subjects
            "CREATE TABLE IF NOT EXISTS TeacherAssignment (" +
            "idAssignment INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "idEnseignant INTEGER NOT NULL, " +
            "idClasse INTEGER NOT NULL, " +
            "idMatiere INTEGER NOT NULL, " +
            "UNIQUE(idEnseignant, idClasse, idMatiere), " +
            "FOREIGN KEY(idEnseignant) REFERENCES Enseignant(idEnseignant), " +
            "FOREIGN KEY(idClasse) REFERENCES Classe(idClasse), " +
            "FOREIGN KEY(idMatiere) REFERENCES Matiere(idMatiere))",

            // Grades Table
            "CREATE TABLE IF NOT EXISTS Note (" +
            "idNote INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "valeur REAL NOT NULL, " +
            "trimestre INTEGER NOT NULL, " +
            "coefficient INTEGER DEFAULT 1, " +
            "idEleve INTEGER NOT NULL, " +
            "idMatiere INTEGER NOT NULL, " +
            "idAnnee INTEGER NOT NULL, " +
            "UNIQUE(idEleve, idMatiere, idAnnee, trimestre), " +
            "FOREIGN KEY(idEleve) REFERENCES Eleve(idEleve), " +
            "FOREIGN KEY(idMatiere) REFERENCES Matiere(idMatiere), " +
            "FOREIGN KEY(idAnnee) REFERENCES AnneeScolaire(idAnnee))",

            // Bulletin Table
            "CREATE TABLE IF NOT EXISTS Bulletin (" +
            "idBulletin INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "moyenneGenerale REAL, " +
            "rangClasse INTEGER, " +
            "appreciation TEXT, " +
            "idEleve INTEGER NOT NULL, " +
            "idAnnee INTEGER NOT NULL, " +
            "trimestre INTEGER NOT NULL, " +
            "dateGeneration DATE, " +
            "UNIQUE(idEleve, idAnnee, trimestre), " +
            "FOREIGN KEY(idEleve) REFERENCES Eleve(idEleve), " +
            "FOREIGN KEY(idAnnee) REFERENCES AnneeScolaire(idAnnee))"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqlCommands) {
                stmt.execute(sql);
            }
            initializeSampleData(conn);
        }
    }

    private static void initializeSampleData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            seedAdmin(stmt);
            seedLevels(stmt);
            seedSchoolYear(stmt);
            seedClassesAndSubjects(stmt);
            seedTeachers(stmt);
            seedStudents(stmt);
            System.out.println("Database initialized successfully");
        }
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    private static void seedAdmin(Statement stmt) throws SQLException {
        stmt.execute(
            "INSERT OR IGNORE INTO Enseignant (code, nom, prenom, telephone, nomUtilisateur, motDePasse, role) " +
            "VALUES ('ADM001', 'Admin', 'System', '0000000000', 'admin', 'admin', 'admin')"
        );
    }

    // ─── Teachers ─────────────────────────────────────────────────────────────
    // username = name (lowercase), password = name (lowercase), no assignments

    private static void seedTeachers(Statement stmt) throws SQLException {
        String[][] teachers = {
            {"TCH001", "Ahmed"},
            {"TCH002", "Mohamed"},
            {"TCH003", "Fatima"},
            {"TCH004", "Aysha"},
            {"TCH005", "Karim"},
            {"TCH006", "Nadia"},
            {"TCH007", "Youssef"},
            {"TCH008", "Leila"},
            {"TCH009", "Omar"},
            {"TCH010", "Salma"}
        };

        for (String[] t : teachers) {
            String code     = t[0];
            String name     = t[1];
            String username = name.toLowerCase();
            stmt.execute(String.format(
                "INSERT OR IGNORE INTO Enseignant (code, nom, prenom, telephone, nomUtilisateur, motDePasse, role) " +
                "VALUES ('%s', '%s', '%s', '', '%s', '%s', 'teacher')",
                escapeSql(code),
                escapeSql(name),
                escapeSql(name),
                escapeSql(username),
                escapeSql(username)
            ));
        }
    }

    // ─── Students ─────────────────────────────────────────────────────────────
    // 3 students per class, spread across one class per level for testing

    private static void seedStudents(Statement stmt) throws SQLException {
        int schoolYearId = getSingleInt(stmt,
            "SELECT idAnnee FROM AnneeScolaire WHERE estActive = 1 LIMIT 1");
        if (schoolYearId < 0) return;

        // {prenom, nom, className, levelName}
        String[][] students = {
            // 1ère année — 1A
            {"Ali",     "Ben Salah",   "1A", "1ère année"},
            {"Ines",    "Trabelsi",    "1A", "1ère année"},
            {"Rami",    "Chaabane",    "1A", "1ère année"},

            // 2ème année — 2A
            {"Sana",    "Mansouri",    "2A", "2ème année"},
            {"Bilel",   "Ghorbel",     "2A", "2ème année"},
            {"Mariam",  "Dridi",       "2A", "2ème année"},

            // 2ème année — 2B
            {"Tarek",   "Jebali",      "2B", "2ème année"},
            {"Houda",   "Ferchichi",   "2B", "2ème année"},
            {"Sirine",  "Hamrouni",    "2B", "2ème année"},

            // 3ème année — 3A
            {"Khalil",  "Ayari",       "3A", "3ème année"},
            {"Rim",     "Hamdi",       "3A", "3ème année"},
            {"Nour",    "Ksouri",      "3A", "3ème année"},

            // 4ème année — 4A
            {"Zied",    "Boughanmi",   "4A", "4ème année"},
            {"Amira",   "Souissi",     "4A", "4ème année"},
            {"Fares",   "Lahmar",      "4A", "4ème année"},

            // 5ème année — 5A
            {"Dorra",   "Mezghani",    "5A", "5ème année"},
            {"Mehdi",   "Khelifi",     "5A", "5ème année"},
            {"Yasmine", "Rebai",       "5A", "5ème année"},

            // 6ème année — 6A
            {"Syrine",  "Benhassine",  "6A", "6ème année"},
            {"Adem",    "Chebbi",      "6A", "6ème année"},
            {"Lina",    "Maaloul",     "6A", "6ème année"}
        };

        int counter = 1;
        for (String[] s : students) {
            String prenom    = s[0];
            String nom       = s[1];
            String className = s[2];
            String levelName = s[3];
            String phone     = String.format("9%07d", counter++);

            // Insert student if not present
            executeIfMissing(
                stmt,
                String.format("SELECT COUNT(*) FROM Eleve WHERE telephoneParent='%s'", escapeSql(phone)),
                String.format(
                    "INSERT INTO Eleve (nom, prenom, dateNaissance, adresse, telephoneParent) " +
                    "VALUES ('%s', '%s', '2017-01-01', 'Tunis', '%s')",
                    escapeSql(nom), escapeSql(prenom), escapeSql(phone)
                )
            );

            int studentId = getSingleInt(stmt,
                String.format("SELECT idEleve FROM Eleve WHERE telephoneParent='%s' LIMIT 1", escapeSql(phone)));
            if (studentId < 0) continue;

            int levelId = getLevelId(stmt, levelName);
            int classId = getSingleInt(stmt, String.format(
                "SELECT idClasse FROM Classe WHERE nomClasse='%s' AND idNiveau=%d LIMIT 1",
                escapeSql(className), levelId));
            if (classId < 0) continue;

            // Assign to class
            executeIfMissing(
                stmt,
                String.format("SELECT COUNT(*) FROM AffectationEleve WHERE idEleve=%d AND idClasse=%d", studentId, classId),
                String.format(
                    "INSERT INTO AffectationEleve (idEleve, idClasse, dateAffectation) VALUES (%d, %d, '2025-09-01')",
                    studentId, classId
                )
            );

            // Enroll in school year
            executeIfMissing(
                stmt,
                String.format("SELECT COUNT(*) FROM Inscription WHERE idEleve=%d AND idAnnee=%d", studentId, schoolYearId),
                String.format(
                    "INSERT INTO Inscription (idEleve, idAnnee, niveauEtude) VALUES (%d, %d, '%s')",
                    studentId, schoolYearId, escapeSql(levelName)
                )
            );
        }
    }

    // ─── Levels ───────────────────────────────────────────────────────────────

    private static void seedLevels(Statement stmt) throws SQLException {
        String[] levels = {
            "1ère année",
            "2ème année",
            "3ème année",
            "4ème année",
            "5ème année",
            "6ème année"
        };
        for (String level : levels) {
            stmt.execute(String.format(
                "INSERT OR IGNORE INTO Niveau (nomNiveau) VALUES ('%s')",
                escapeSql(level)
            ));
        }
    }

    private static void seedSchoolYear(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM AnneeScolaire")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO AnneeScolaire (nom, estActive) VALUES ('2025-2026', 1)");
            }
        }
    }

    // ─── Classes & Subjects ───────────────────────────────────────────────────

    private static void seedClassesAndSubjects(Statement stmt) throws SQLException {
        String[][] classDefinitions = getDemoClassDefinitions();
        
        int schoolYearId = getSingleInt(stmt,
            "SELECT idAnnee FROM AnneeScolaire WHERE estActive = 1 LIMIT 1");
        if (schoolYearId < 0) return;

        String[][] subjectsByLevel = {
            // 1ère année
            {
                "1ère année - الانتاج الكتابي",
                "1ère année - التربية الإسلامية",
                "1ère année - الخط و الاملاء",
                "1ère année - الايقاظ العلمي",
                "1ère année - الرياضيات",
                "1ère année - القراءة",
                "1ère année - التربية البدنية"
            },
            // 2ème année
            {
                "2ème année - الرياضيات",
                "2ème année - القراءة",
                "2ème année - الايقاظ العلمي",
                "2ème année - التربية الاسلامية",
                "2ème année - lecture",
                "2ème année - التربية البدنية",
                "2ème année - production ecrite",
                "2ème année - الانتاج الكتابي",
                "2ème année - dictée",
                "2ème année - قواعد اللغة"
            },
            // 3ème année
            {
                "3ème année - الرياضيات",
                "3ème année - القراءة",
                "3ème année - الايقاظ العلمي",
                "3ème année - التربية الاسلامية",
                "3ème année - lecture",
                "3ème année - التربية البدنية",
                "3ème année - production ecrite",
                "3ème année - dictée",
                "3ème année - قواعد اللغة"
            },
            // 4ème année
            {
                "4ème année - Anglais",
                "4ème année - الجغرافيا",
                "4ème année - التربية المدنية",
                "4ème année - التربية التكنولوجية",
                "4ème année - الرياضيات",
                "4ème année - القراءة",
                "4ème année - الايقاظ العلمي",
                "4ème année - production ecrite",
                "4ème année - الانتاج الكتابي",
                "4ème année - dictée",
                "4ème année - التربية الاسلامية",
                "4ème année - lecture",
                "4ème année - التربية البدنية",
                "4ème année - قواعد اللغة"
            },
            // 5ème année
            {
                "5ème année - Anglais",
                "5ème année - الجغرافيا",
                "5ème année - التربية المدنية",
                "5ème année - التربية التكنولوجية",
                "5ème année - الرياضيات",
                "5ème année - القراءة",
                "5ème année - الايقاظ العلمي",
                "5ème année - production ecrite",
                "5ème année - الانتاج الكتابي",
                "5ème année - dictée",
                "5ème année - التربية الاسلامية",
                "5ème année - lecture",
                "5ème année - التربية البدنية",
                "5ème année - قواعد اللغة"
            },
            // 6ème année
            {
                "6ème année - Anglais",
                "6ème année - الجغرافيا",
                "6ème année - التربية المدنية",
                "6ème année - التربية التكنولوجية",
                "6ème année - الرياضيات",
                "6ème année - القراءة",
                "6ème année - الايقاظ العلمي",
                "6ème année - production ecrite",
                "6ème année - الانتاج الكتابي",
                "6ème année - dictée",
                "6ème année - التربية الاسلامية",
                "6ème année - lecture",
                "6ème année - التربية البدنية",
                "6ème année - قواعد اللغة"
            }
        };

        for (String[] classDefinition : classDefinitions) {
            String className = classDefinition[0];
            String levelName = classDefinition[1];
            int levelId      = getLevelId(stmt, levelName);
            int levelIndex   = getLevelIndex(levelName);
            if (levelIndex < 0 || levelIndex >= subjectsByLevel.length) continue;

            executeIfMissing(
                stmt,
                String.format("SELECT COUNT(*) FROM Classe WHERE nomClasse='%s' AND idNiveau=%d AND idAnnee=%d",
                    escapeSql(className), levelId, schoolYearId),
                String.format("INSERT INTO Classe (nomClasse, capaciteMax, idNiveau, idAnnee) VALUES ('%s', 20, %d, %d)",
                    escapeSql(className), levelId, schoolYearId)
            );

            for (String subjectName : subjectsByLevel[levelIndex]) {
                executeIfMissing(
                    stmt,
                    String.format("SELECT COUNT(*) FROM Matiere WHERE nomMatiere='%s'", escapeSql(subjectName)),
                    String.format("INSERT INTO Matiere (nomMatiere, idNiveau) VALUES ('%s', %d)",
                        escapeSql(subjectName), levelId)
                );
            }
        }
    }

    public static void seedClassesForYear(int idAnnee) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String[][] classDefinitions = getDemoClassDefinitions();
            for (String[] classDefinition : classDefinitions) {
                String className = classDefinition[0];
                String levelName = classDefinition[1];
                int levelId      = getLevelId(stmt, levelName);
                if (levelId < 0) continue;

                executeIfMissing(
                    stmt,
                    String.format("SELECT COUNT(*) FROM Classe WHERE nomClasse='%s' AND idNiveau=%d AND idAnnee=%d",
                        escapeSql(className), levelId, idAnnee),
                    String.format("INSERT INTO Classe (nomClasse, capaciteMax, idNiveau, idAnnee) VALUES ('%s', 20, %d, %d)",
                        escapeSql(className), levelId, idAnnee)
                );
            }
        }
    }

    private static int getLevelIndex(String levelName) {
        if ("1ère année".equals(levelName)) return 0;
        if ("2ème année".equals(levelName)) return 1;
        if ("3ème année".equals(levelName)) return 2;
        if ("4ème année".equals(levelName)) return 3;
        if ("5ème année".equals(levelName)) return 4;
        if ("6ème année".equals(levelName)) return 5;
        return -1;
    }

    private static void ensureSubjectsSeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            seedClassesAndSubjects(stmt);
        }
    }

    private static String[][] getDemoClassDefinitions() {
        return new String[][] {
            {"1A", "1ère année"},

            {"2A", "2ème année"},
            {"2B", "2ème année"},

            {"3A", "3ème année"},
            {"3B", "3ème année"},
            {"3C", "3ème année"},

            {"4A", "4ème année"},
            {"4B", "4ème année"},
            {"4C", "4ème année"},
            {"4D", "4ème année"},

            {"5A", "5ème année"},
            {"5B", "5ème année"},
            {"5C", "5ème année"},
            {"5D", "5ème année"},
            {"5E", "5ème année"},

            {"6A", "6ème année"},
            {"6B", "6ème année"},
            {"6C", "6ème année"},
            {"6D", "6ème année"},
            {"6E", "6ème année"},
            {"6F", "6ème année"}
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static void executeIfMissing(Statement stmt, String existsSql, String insertSql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(existsSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(insertSql);
            }
        }
    }

    private static int getSingleInt(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    private static int getLevelId(Statement stmt, String levelName) throws SQLException {
        return getSingleInt(stmt, String.format(
            "SELECT idNiveau FROM Niveau WHERE nomNiveau='%s' LIMIT 1", escapeSql(levelName)));
    }

    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }

    // ─── Connection Management ────────────────────────────────────────────────

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}