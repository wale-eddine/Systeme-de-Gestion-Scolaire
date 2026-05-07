package application;

/**
 * [MODEL] Entité représentant un Enseignant (Professeur) dans le système.
 * Utilisé principalement pour :
 * - L'authentification au démarrage [LOGIC]
 * - L'affichage dans la table de gestion des enseignants [ADMIN] [UI]
 * - L'affectation à des matières et des classes [ADMIN] [ACTION]
 */
public class Teacher {
    private int idEnseignant;
    private String code;
    private String nom;
    private String prenom;
    private String telephone;
    private String nomUtilisateur;
    private String motDePasse;
    private String classesAffectees = "Non assigne";
    private String matieresAffectees = "Non assignee";

    public Teacher() {}

    public Teacher(int idEnseignant, String code, String nom, String prenom, 
                   String telephone, String nomUtilisateur, String motDePasse) {
        this.idEnseignant = idEnseignant;
        this.code = code;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
    }

    // Getters and Setters
    public int getIdEnseignant() { return idEnseignant; }
    public void setIdEnseignant(int idEnseignant) { this.idEnseignant = idEnseignant; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getClassesAffectees() { return classesAffectees; }
    public void setClassesAffectees(String classesAffectees) {
        this.classesAffectees = (classesAffectees == null || classesAffectees.isBlank())
            ? "Non assigne"
            : classesAffectees;
    }

    public String getMatieresAffectees() { return matieresAffectees; }
    public void setMatieresAffectees(String matieresAffectees) {
        this.matieresAffectees = (matieresAffectees == null || matieresAffectees.isBlank())
            ? "Non assignee"
            : matieresAffectees;
    }

    public String getStatutAffectation() {
        return "Non assigne".equalsIgnoreCase(classesAffectees) ? "Non assigne" : "Assigne";
    }

    public String getFullName() {
        return prenom + " " + nom;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
