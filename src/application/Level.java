package application;

/**
 * [MODEL] Entité représentant un Niveau scolaire (ex: CP, CE1, 6ème).
 * Utilisé pour :
 * - Organiser les classes et les matières par degré de scolarité [ADMIN] [LOGIC]
 * - Filtrer les affichages dans les listes déroulantes [UI] [FILTER]
 */
public class Level {
    private int idNiveau;
    private String nomNiveau;

    public Level() {}

    public Level(int idNiveau, String nomNiveau) {
        this.idNiveau = idNiveau;
        this.nomNiveau = nomNiveau;
    }

    public Level(String nomNiveau) {
        this.nomNiveau = nomNiveau;
    }

    // Getters and Setters
    public int getIdNiveau() { return idNiveau; }
    public void setIdNiveau(int idNiveau) { this.idNiveau = idNiveau; }

    public String getNomNiveau() { return nomNiveau; }
    public void setNomNiveau(String nomNiveau) { this.nomNiveau = nomNiveau; }

    @Override
    public String toString() {
        return nomNiveau;
    }
}
