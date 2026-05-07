package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

/**
 * [MAIN] [LOGIC] Point d'entrée de l'application JavaFX.
 * Rôle : Initialise la fenêtre principale et démarre l'application.
 * 
 * Logique clé :
 * - Appelle `DatabaseConnection.getConnection()` pour initialiser SQLite au démarrage [DATABASE].
 * - Lance l'interface `Authentification` [UI].
 */
public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			Authentification a= new Authentification(primaryStage);
			a.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
		launch(args);
	}
}
