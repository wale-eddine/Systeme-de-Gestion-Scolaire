package application;

import java.time.LocalDate;

/**
 * [MODEL] Entité représentant un Élève dans le système.
 * Utilisé principalement pour :
 * - L'inscription et la modification des données de l'élève [ADMIN] [ACTION]
 * - L'affectation des élèves à une classe spécifique [ADMIN] [ACTION]
 * - La saisie et consultation des notes [TEACHER] [ADMIN]
 */
public class Student {
    private int idEleve;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String adresse;
    private String telephoneParent;
    private String classesAffectees = "Non affecté";

    public Student() {}

    public Student(int idEleve, String nom, String prenom, LocalDate dateNaissance, 
                   String adresse, String telephoneParent) {
        this.idEleve = idEleve;
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.adresse = adresse;
        this.telephoneParent = telephoneParent;
    }

    public Student(String nom, String prenom, LocalDate dateNaissance, 
                   String adresse, String telephoneParent) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.adresse = adresse;
        this.telephoneParent = telephoneParent;
    }

    // Getters and Setters
    public int getIdEleve() { return idEleve; }
    public void setIdEleve(int idEleve) { this.idEleve = idEleve; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephoneParent() { return telephoneParent; }
    public void setTelephoneParent(String telephoneParent) { this.telephoneParent = telephoneParent; }

    public String getClassesAffectees() { return classesAffectees; }
    public void setClassesAffectees(String classesAffectees) {
        this.classesAffectees = (classesAffectees == null || classesAffectees.isBlank())
            ? "Non affecté"
            : classesAffectees;
    }

    public String getFullName() {
        return prenom + " " + nom;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
