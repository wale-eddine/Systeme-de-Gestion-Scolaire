package application;

/**
 * [MODEL] Entité représentant une Matière enseignée (ex: Mathématiques, Français).
 * Utilisé pour :
 * - Définir le programme de chaque niveau [ADMIN] [LOGIC]
 * - Permettre aux enseignants d'entrer des notes spécifiques [TEACHER] [ACTION]
 */
public class Subject {
    private int idMatiere;
    private String nomMatiere;
    private int idNiveau;

    public Subject() {}

    public Subject(int idMatiere, String nomMatiere, int idNiveau) {
        this.idMatiere = idMatiere;
        this.nomMatiere = nomMatiere;
        this.idNiveau = idNiveau;
    }

    public Subject(String nomMatiere, int idNiveau) {
        this.nomMatiere = nomMatiere;
        this.idNiveau = idNiveau;
    }

    // Getters and Setters
    public int getIdMatiere() { return idMatiere; }
    public void setIdMatiere(int idMatiere) { this.idMatiere = idMatiere; }

    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public int getIdNiveau() { return idNiveau; }
    public void setIdNiveau(int idNiveau) { this.idNiveau = idNiveau; }

    @Override
    public String toString() {
        return nomMatiere;
    }
}
