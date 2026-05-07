package application;

/**
 * [MODEL] Entité représentant une Année Scolaire (ex: 2023-2024).
 * Utilisé pour :
 * - Isoler les données (élèves, classes, notes) par année [LOGIC] [FILTER]
 * - Permettre à l'administrateur de basculer d'une année à l'autre [ADMIN] [ACTION]
 */
public class AnneeScolaire {
    private int idAnnee;
    private String nom;
    private boolean estActive;

    public AnneeScolaire(int idAnnee, String nom, boolean estActive) {
        this.idAnnee = idAnnee;
        this.nom = nom;
        this.estActive = estActive;
    }

    public AnneeScolaire(String nom, boolean estActive) {
        this.nom = nom;
        this.estActive = estActive;
    }

    public int getIdAnnee() {
        return idAnnee;
    }

    public void setIdAnnee(int idAnnee) {
        this.idAnnee = idAnnee;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public boolean isEstActive() {
        return estActive;
    }

    public void setEstActive(boolean estActive) {
        this.estActive = estActive;
    }

    @Override
    public String toString() {
        return nom + (estActive ? " (Active)" : "");
    }
}
