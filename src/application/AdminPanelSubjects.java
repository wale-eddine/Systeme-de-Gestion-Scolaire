package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;

/**
 * [ADMIN] [UI] Panneau de gestion des Matières.
 * Rôle : Définir le programme d'études en ajoutant, modifiant ou supprimant des matières par niveau.
 * 
 * Composants principaux :
 * - `tableView` : Liste les matières [VUE].
 * - Filtre par niveau scolaire [FILTER].
 * 
 * Logique clé :
 * - Les matières sont globales à un niveau, peu importe l'année scolaire [LOGIC].
 * - Gestion via `SubjectDAO` [DATABASE].
 */
public class AdminPanelSubjects {
    private BorderPane mainPane;
    private TableView<Subject> tableView;
    private ObservableList<Subject> subjectList;

    public AdminPanelSubjects(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Gestion des matières");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 10;");
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddSubjectDialog());
        editButton.setOnAction(e -> openEditSubjectDialog());
        deleteButton.setOnAction(e -> deleteSubject());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        tableView = new TableView<>();
        createColumns();
        loadSubjects();

        contentBox.getChildren().addAll(titleLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<Subject, String> nameCol = new TableColumn<>("Matière");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nomMatiere"));

        tableView.getColumns().addAll(nameCol);
    }

    private void loadSubjects() {
        try {
            List<Subject> subjects = SubjectDAO.getAllSubjects();
            subjectList = FXCollections.observableArrayList(subjects);
            tableView.setItems(subjectList);
        } catch (Exception e) {
            showError("Erreur de chargement des matières : " + e.getMessage());
        }
    }

    private void openAddSubjectDialog() {
        Dialog<Subject> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une matière");
        dialog.setHeaderText("Saisir les informations de la matière");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField();
        ComboBox<Level> levelCombo = new ComboBox<>();

        loadLevels(levelCombo);

        grid.add(new Label("Matière :"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Niveau :"), 0, 1);
        grid.add(levelCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && levelCombo.getValue() != null) {
                return new Subject(nameField.getText(), levelCombo.getValue().getIdNiveau());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(subject -> {
            try {
                SubjectDAO.addSubject(subject);
                loadSubjects();
                showInfo("Matière ajoutée avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de l'ajout de la matière : " + e.getMessage());
            }
        });
    }

    private void openEditSubjectDialog() {
        Subject selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une matière à modifier.");
            return;
        }

        Dialog<Subject> dialog = new Dialog<>();
        dialog.setTitle("Modifier une matière");
        dialog.setHeaderText("Modifier les informations de la matière");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField(selected.getNomMatiere());
        ComboBox<Level> levelCombo = new ComboBox<>();
        loadLevels(levelCombo);

        grid.add(new Label("Matière :"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Niveau :"), 0, 1);
        grid.add(levelCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && levelCombo.getValue() != null) {
                selected.setNomMatiere(nameField.getText());
                selected.setIdNiveau(levelCombo.getValue().getIdNiveau());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(subject -> {
            try {
                SubjectDAO.updateSubject(subject);
                loadSubjects();
                showInfo("Matière modifiée avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de la modification de la matière : " + e.getMessage());
            }
        });
    }

    private void deleteSubject() {
        Subject selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une matière à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer cette matière ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    SubjectDAO.deleteSubject(selected.getIdMatiere());
                    loadSubjects();
                    showInfo("Matière supprimée avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de la matière : " + e.getMessage());
                }
            }
        });
    }

    private void loadLevels(ComboBox<Level> combo) {
        try {
            List<Level> levels = LevelDAO.getAllLevels();
            ObservableList<Level> items = FXCollections.observableArrayList(levels);
            combo.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des niveaux : " + e.getMessage());
        }
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
