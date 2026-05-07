package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * [TEACHER] [UI] Panneau affichant les classes assignées à un enseignant.
 * Rôle : Permet à l'enseignant de voir la liste de ses classes pour l'année scolaire en cours.
 * 
 * Composants principaux :
 * - `tableView` : Liste des classes de l'enseignant connecté [VUE].
 * 
 * Logique clé :
 * - Appelle `TeacherDAO.getTeacherClassesByYear` avec l'ID de l'enseignant actif et l'année sélectionnée [LOGIC] [DATABASE].
 */
public class TeacherPanelClasses {
    private BorderPane mainPane;
    private int teacherId;
    private TableView<SchoolClass> tableView;

    public TeacherPanelClasses(BorderPane mainPane, int teacherId) {
        this.mainPane = mainPane;
        this.teacherId = teacherId;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Mes classes");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        tableView = new TableView<>();
        createColumns();
        loadClasses();

        contentBox.getChildren().addAll(titleLabel, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<SchoolClass, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("idClasse"));

        TableColumn<SchoolClass, String> nameCol = new TableColumn<>("Classe");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nomClasse"));

        TableColumn<SchoolClass, Integer> capacityCol = new TableColumn<>("Capacité");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));

        tableView.getColumns().addAll(idCol, nameCol, capacityCol);
    }

    private void loadClasses() {
        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            if (currentAnnee == null) {
                showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
                return;
            }
            List<SchoolClass> classes = TeacherDAO.getTeacherClassesByYear(teacherId, currentAnnee.getIdAnnee());
            ObservableList<SchoolClass> classList = FXCollections.observableArrayList(classes);
            tableView.setItems(classList);
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
