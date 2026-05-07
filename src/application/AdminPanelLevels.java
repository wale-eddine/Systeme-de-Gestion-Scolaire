package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * [ADMIN] [UI] Panneau de gestion des Niveaux.
 * Rôle : Gérer les différents niveaux scolaires (1ère année, 2ème année, etc.).
 * 
 * Composants principaux :
 * - `tableView` simple pour lister les niveaux [VUE].
 * 
 * Logique clé :
 * - Interface basique de CRUD utilisant `LevelDAO` [DATABASE].
 * - Modifie la structure de base qui impacte les autres panneaux [LOGIC].
 */
public class AdminPanelLevels {
    private BorderPane mainPane;
    private TableView<Level> tableView;
    private ObservableList<Level> levelList;

    public AdminPanelLevels(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Gestion des niveaux");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 10;");
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddLevelDialog());
        editButton.setOnAction(e -> openEditLevelDialog());
        deleteButton.setOnAction(e -> deleteLevel());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        tableView = new TableView<>();
        createColumns();
        loadLevels();

        contentBox.getChildren().addAll(titleLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<Level, String> nameCol = new TableColumn<>("Niveau");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nomNiveau"));

        tableView.getColumns().addAll(nameCol);
    }

    private void loadLevels() {
        try {
            List<Level> levels = LevelDAO.getAllLevels();
            levelList = FXCollections.observableArrayList(levels);
            tableView.setItems(levelList);
        } catch (Exception e) {
            showError("Erreur de chargement des niveaux : " + e.getMessage());
        }
    }

    private void openAddLevelDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter un niveau");
        dialog.setHeaderText("Saisir le nom du niveau :");
        dialog.setContentText("Niveau :");

        dialog.showAndWait().ifPresent(levelName -> {
            if (!levelName.trim().isEmpty()) {
                try {
                    Level level = new Level(levelName);
                    LevelDAO.addLevel(level);
                    loadLevels();
                    showInfo("Niveau ajouté avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de l'ajout du niveau : " + e.getMessage());
                }
            }
        });
    }

    private void openEditLevelDialog() {
        Level selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un niveau à modifier.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getNomNiveau());
        dialog.setTitle("Modifier un niveau");
        dialog.setHeaderText("Modifier le nom du niveau :");
        dialog.setContentText("Niveau :");

        dialog.showAndWait().ifPresent(levelName -> {
            if (!levelName.trim().isEmpty()) {
                try {
                    selected.setNomNiveau(levelName);
                    LevelDAO.updateLevel(selected);
                    loadLevels();
                    showInfo("Niveau modifié avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la modification du niveau : " + e.getMessage());
                }
            }
        });
    }

    private void deleteLevel() {
        Level selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un niveau à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer ce niveau ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    LevelDAO.deleteLevel(selected.getIdNiveau());
                    loadLevels();
                    showInfo("Niveau supprimé avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression du niveau : " + e.getMessage());
                }
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
