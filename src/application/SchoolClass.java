package application;

/**
 * [MODEL] Entité représentant une Classe (ex: 1A, 2B) dans une année scolaire donnée.
 * Utilisé pour :
 * - Créer l'architecture de l'école (Niveaux -> Classes) [ADMIN] [LOGIC]
 * - Regrouper les élèves et assigner les professeurs [ADMIN] [ACTION]
 */
public class SchoolClass {
    private int idClasse;
    private String nomClasse;
    private int capaciteMax;
    private int idNiveau;
    private int idAnnee;

    public SchoolClass() {}

    public SchoolClass(int idClasse, String nomClasse, int capaciteMax, int idNiveau, int idAnnee) {
        this.idClasse = idClasse;
        this.nomClasse = nomClasse;
        this.capaciteMax = capaciteMax;
        this.idNiveau = idNiveau;
        this.idAnnee = idAnnee;
    }

    public SchoolClass(String nomClasse, int capaciteMax, int idNiveau, int idAnnee) {
        this.nomClasse = nomClasse;
        this.capaciteMax = capaciteMax;
        this.idNiveau = idNiveau;
        this.idAnnee = idAnnee;
    }

    // Getters and Setters
    public int getIdClasse() { return idClasse; }
    public void setIdClasse(int idClasse) { this.idClasse = idClasse; }

    public String getNomClasse() { return nomClasse; }
    public void setNomClasse(String nomClasse) { this.nomClasse = nomClasse; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public int getIdNiveau() { return idNiveau; }
    public void setIdNiveau(int idNiveau) { this.idNiveau = idNiveau; }

    public int getIdAnnee() { return idAnnee; }
    public void setIdAnnee(int idAnnee) { this.idAnnee = idAnnee; }

    @Override
    public String toString() {
        return nomClasse;
    }
}
