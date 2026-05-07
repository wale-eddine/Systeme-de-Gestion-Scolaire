package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Stage;

/**
 * [MAIN] [UI] Panneau de connexion (Login).
 * Rôle : Authentifier l'utilisateur avant d'accéder au système.
 * 
 * Composants principaux :
 * - Champs `Nom d'utilisateur` et `Mot de passe` [VUE].
 * 
 * Logique clé :
 * - Vérifie les identifiants avec la table Enseignant (Admin ou Prof) via `TeacherDAO.authenticate` [SECURITY] [DATABASE].
 * - Ouvre `MenuGeneral` si le login est correct [ACTION].
 */
public class Authentification 
{
	private static final double AUTH_WIDTH = 520;
	private static final double AUTH_HEIGHT = 340;
	private static final double AUTH_MIN_WIDTH = 460;
	private static final double AUTH_MIN_HEIGHT = 300;

	Stage stage;
	
	Label lblLogin = new Label("Identifiant");
	TextField txtLogin = new TextField();
	
	Label lblpwd = new Label("Mot de passe");
	PasswordField txtpwd = new PasswordField();

	Button btnConnecter = new Button("Connexion");
	Button btnQuitter = new Button("Quitter");
	
	public Authentification(Stage stage) 
	{
		this.stage = stage;
	}
	
	private void Connecter() 
 	{
		String username = txtLogin.getText();
		String password = txtpwd.getText();
		
		if (username.isEmpty() || password.isEmpty()) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Erreur de connexion");
			alert.setHeaderText(null);
			alert.setContentText("Veuillez saisir l'identifiant et le mot de passe.");
			alert.showAndWait();
			return;
		}
		
		try {
			User user = TeacherDAO.authenticate(username, password);
			if (user != null) {
				MenuGeneral mg = new MenuGeneral(stage, user);
				mg.show();
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Erreur d'authentification");
				alert.setHeaderText(null);
				alert.setContentText("Identifiant ou mot de passe incorrect.");
				alert.showAndWait();
			}
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Erreur de base de données");
			alert.setHeaderText(null);
				alert.setContentText("Erreur de connexion à la base de données : " + e.getMessage());
			alert.showAndWait();
		}
	}	
	
	public void show()
	{
		HBox h1 = new HBox(12);
		h1.getChildren().addAll(lblLogin, txtLogin);
		h1.setAlignment(Pos.CENTER_LEFT);
		
		HBox h2 = new HBox(12);
		h2.getChildren().addAll(lblpwd, txtpwd);
		h2.setAlignment(Pos.CENTER_LEFT);
		
		HBox h3 = new HBox(12);
		h3.getChildren().addAll(btnConnecter, btnQuitter);
		h3.setAlignment(Pos.CENTER_RIGHT);
		
		lblLogin.setPrefWidth(90);
		lblpwd.setPrefWidth(90);
		txtLogin.setPrefWidth(260);
		txtpwd.setPrefWidth(260);
		btnConnecter.setPrefWidth(110);
		btnQuitter.setPrefWidth(110);
		txtLogin.setPromptText("Saisir l'identifiant");
		txtpwd.setPromptText("Saisir le mot de passe");
		
		btnQuitter.setOnAction(e -> stage.close());
		
		btnConnecter.setOnAction(e -> Connecter());
		txtpwd.setOnAction(e -> Connecter());
		
		VBox V = new VBox(16);
		V.getChildren().addAll(h1, h2, h3);
		V.setAlignment(Pos.CENTER);
		V.setStyle("-fx-padding: 24;");
		
		Scene scene = new Scene(V, AUTH_WIDTH, AUTH_HEIGHT);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		stage.setScene(scene);
		stage.setTitle("Gestion scolaire - Connexion");
		stage.setMaximized(false);
		stage.setWidth(AUTH_WIDTH);
		stage.setHeight(AUTH_HEIGHT);
		stage.setMinWidth(AUTH_MIN_WIDTH);
		stage.setMinHeight(AUTH_MIN_HEIGHT);
		stage.centerOnScreen();
		stage.show();
	
	}
	

}
