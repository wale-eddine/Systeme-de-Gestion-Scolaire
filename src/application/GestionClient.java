package application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class GestionClient 
{
	Stage stage=new Stage();
	Label lblTitre=new Label("Gestion des Clients");
	Label lblRaisonSOcial=new Label("Raison Social");
	Label lblType=new Label("Type");
	Label lblDateCreation=new Label("Date de Création");
	Label lblAdresse=new Label("Adresse");
	Label lblVille=new Label("Ville");
	
	TextField txtRaisonSocial=new TextField();
	
	RadioButton rbSociete=new RadioButton("Sociétè");
	RadioButton rbPersonnePhysique=new RadioButton("Personne Physique");
	
	ToggleGroup groupe1=new ToggleGroup();
	 
	DatePicker txtDateCreation=new DatePicker();
	
	TextArea txtAdresse=new TextArea();
	
	ComboBox<String> cmbVille=new ComboBox<>();
	ListView<String> lvVille=new ListView<>();
	
	CheckBox cbTVA =new CheckBox("Assujettie à la TVA");
	
	Button btnAjouter=new Button("Ajouter");
	Button btnModifier=new Button("Modifier");
	Button btnSupprimer=new Button("Supprimer");
	Button btnQuitter=new Button("Quitter");
	
	public void show()
	{
		rbSociete.setToggleGroup(groupe1);
		rbPersonnePhysique.setToggleGroup(groupe1);
		
		cmbVille.getItems().addAll("tunis","Sousse","Sfax");
		lvVille.getItems().addAll("tunis","Sousse","Sfax");
		
		GridPane b1=new GridPane();
		b1.setAlignment(Pos.CENTER);
		b1.add(lblRaisonSOcial, 0, 0);
		b1.add(txtRaisonSocial, 1, 0);
		
		b1.add(lblType, 0, 1);
		b1.add(rbPersonnePhysique, 1, 1);
		b1.add(rbSociete, 2, 1);
		
		b1.add(lblDateCreation, 0, 2);
		b1.add(txtDateCreation, 1, 2);
		
		b1.add(lblAdresse, 0, 3);
		b1.add(txtAdresse, 1, 3);
		
		b1.add(lblVille, 0, 4);
		b1.add(cmbVille, 1, 4);
		
		b1.add(cbTVA, 1, 5);
		
		GridPane b2=new GridPane();
		b2.add(btnAjouter, 0, 0);
		b2.add(btnModifier, 1, 0);
		b2.add(btnSupprimer, 2, 0);
		b2.add(btnQuitter, 3, 0);
		
		b1.add(b2, 1, 6);
		
		HBox h1=new HBox();
		h1.setAlignment(Pos.CENTER);
		h1.getChildren().add(lblTitre);
		
		BorderPane B=new BorderPane();
		
		B.setTop(h1);
		
		B.setCenter(b1);
		
		//B.setBottom(Tableau);
		
		Scene scene=new Scene(B,900,600);
		stage.setScene(scene);
		stage.centerOnScreen();
		stage.show();
	}
}