package application;

/**
 * [MODEL] Entité représentant une Note attribuée à un élève pour une matière.
 * Utilisé pour :
 * - Stocker la note, le trimestre et le coefficient [DATABASE]
 * - L'affichage des notes saisies par l'enseignant [TEACHER] [UI]
 * - La génération des bulletins scolaires [ADMIN] [ACTION]
 */
public class Grade {
    private int idNote;
    private double valeur;
    private int trimestre;
    private int coefficient;
    private int idEleve;
    private int idMatiere;
    private int idAnnee;
    private String nomMatiere;
    private String nomEleve;

    public Grade() {}

    public Grade(int idNote, double valeur, int trimestre, int coefficient, 
                 int idEleve, int idMatiere, int idAnnee) {
        this.idNote = idNote;
        this.valeur = valeur;
        this.trimestre = trimestre;
        this.coefficient = coefficient;
        this.idEleve = idEleve;
        this.idMatiere = idMatiere;
        this.idAnnee = idAnnee;
    }

    public Grade(double valeur, int trimestre, int coefficient, int idEleve, 
                 int idMatiere, int idAnnee) {
        this.valeur = valeur;
        this.trimestre = trimestre;
        this.coefficient = coefficient;
        this.idEleve = idEleve;
        this.idMatiere = idMatiere;
        this.idAnnee = idAnnee;
    }

    // Getters and Setters
    public int getIdNote() { return idNote; }
    public void setIdNote(int idNote) { this.idNote = idNote; }

    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = valeur; }

    public int getTrimestre() { return trimestre; }
    public void setTrimestre(int trimestre) { this.trimestre = trimestre; }

    public int getCoefficient() { return coefficient; }
    public void setCoefficient(int coefficient) { this.coefficient = coefficient; }

    public int getIdEleve() { return idEleve; }
    public void setIdEleve(int idEleve) { this.idEleve = idEleve; }

    public int getIdMatiere() { return idMatiere; }
    public void setIdMatiere(int idMatiere) { this.idMatiere = idMatiere; }

    public int getIdAnnee() { return idAnnee; }
    public void setIdAnnee(int idAnnee) { this.idAnnee = idAnnee; }

    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public String getNomEleve() { return nomEleve; }
    public void setNomEleve(String nomEleve) { this.nomEleve = nomEleve; }
}
