package application;

/**
 * [MODEL] Entité abstraite représentant un Utilisateur connecté (Admin ou Enseignant).
 * Utilisé pour :
 * - Conserver la session de l'utilisateur actif [LOGIC]
 * - Restreindre l'accès à certaines vues selon le rôle (isAdmin, isTeacher) [UI] [FILTER]
 */
public class User {
    private int id;
    private String username;
    private String nom;
    private String prenom;
    private String role; // "admin" or "teacher"

    public User(int id, String username, String nom, String prenom, String role) {
        this.id = id;
        this.username = username;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return prenom + " " + nom;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isTeacher() {
        return "teacher".equalsIgnoreCase(role);
    }
}
