package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [ADMIN] [UI] Panneau de gestion des Classes.
 * Rôle : Créer, modifier et supprimer les classes (ex: 1A, 2B) pour l'année scolaire en cours.
 * 
 * Composants principaux :
 * - `tableView` : Affiche les classes de l'année active [VUE].
 * - Filtres par niveau [FILTER].
 * 
 * Logique clé :
 * - Les opérations CRUD sont gérées via `ClassDAO` et restreintes par `idAnnee` pour l'isolation des données [LOGIC] [DATABASE].
 */
public class AdminPanelClasses {
    private BorderPane mainPane;
    private TableView<SchoolClass> tableView;
    private ObservableList<SchoolClass> classList;
    private FilteredList<SchoolClass> filteredList;
    private Map<Integer, String> levelNamesById = new HashMap<>();
    private ComboBox<Level> levelFilterCombo;

    public AdminPanelClasses(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Gestion des classes");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Search Box
        HBox searchBox = new HBox(10);
        searchBox.setStyle("-fx-padding: 4 0 4 0;");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Rechercher:");
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher par nom de classe...");
        searchField.setPrefWidth(250);

        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.setPrefWidth(180);
        loadLevelFilters();

        searchBox.getChildren().addAll(searchLabel, searchField, new Label("Niveau :"), levelFilterCombo);

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 2 0 4 0;");
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddClassDialog());
        editButton.setOnAction(e -> openEditClassDialog());
        deleteButton.setOnAction(e -> deleteClass());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();
        loadClasses();

        Runnable applyFilters = () -> {
            String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            Level selectedLevel = levelFilterCombo.getValue();

            filteredList.setPredicate(schoolClass -> {
                boolean matchesText = query.isBlank() || schoolClass.getNomClasse().toLowerCase().contains(query);
                boolean matchesLevel = selectedLevel == null
                    || selectedLevel.getIdNiveau() == -1
                    || schoolClass.getIdNiveau() == selectedLevel.getIdNiveau();
                return matchesText && matchesLevel;
            });
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());

        contentBox.getChildren().addAll(titleLabel, searchBox, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<SchoolClass, String> nameCol = new TableColumn<>("Classe");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nomClasse"));
        nameCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<SchoolClass, String> levelCol = new TableColumn<>("Niveau");
        levelCol.setCellValueFactory(cellData -> {
            String levelName = levelNamesById.getOrDefault(cellData.getValue().getIdNiveau(), "-");
            return new SimpleStringProperty(levelName);
        });
        levelCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<SchoolClass, Integer> capacityCol = new TableColumn<>("Capacité");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capaciteMax"));
        capacityCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().addAll(nameCol, levelCol, capacityCol);
    }

    private void loadClasses() {
        try {
            loadLevelNamesForDisplay();
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            List<SchoolClass> classes;
            if (currentAnnee != null) {
                classes = ClassDAO.getClassesByYear(currentAnnee.getIdAnnee());
            } else {
                classes = ClassDAO.getAllClasses();
            }
            classList = FXCollections.observableArrayList(classes);
            filteredList = new FilteredList<>(classList, p -> true);
            tableView.setItems(filteredList);
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void loadLevelNamesForDisplay() throws SQLException {
        levelNamesById.clear();
        for (Level level : LevelDAO.getAllLevels()) {
            levelNamesById.put(level.getIdNiveau(), level.getNomNiveau());
        }
    }

    private int getClassCountForLevelAndYear(int idNiveau, int idAnnee) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Classe WHERE idNiveau = ? AND idAnnee = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idNiveau);
            stmt.setInt(2, idAnnee);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private void openAddClassDialog() {
        Dialog<SchoolClass> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une classe");
        dialog.setHeaderText("Saisir les informations de la classe");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField();
        Spinner<Integer> capacitySpinner = new Spinner<>(10, 30, 20);
        ComboBox<Level> levelCombo = new ComboBox<>();

        loadLevels(levelCombo);

        grid.add(new Label("Classe :"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Capacité :"), 0, 1);
        grid.add(capacitySpinner, 1, 1);
        grid.add(new Label("Niveau :"), 0, 2);
        grid.add(levelCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && levelCombo.getValue() != null) {
                AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
                if (currentAnnee == null) {
                    showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
                    return null;
                }
                return new SchoolClass(nameField.getText(), capacitySpinner.getValue(), 
                                     levelCombo.getValue().getIdNiveau(), currentAnnee.getIdAnnee());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(schoolClass -> {
            try {
                // Check if the level already has 6 classes
                int classCount = getClassCountForLevelAndYear(schoolClass.getIdNiveau(), schoolClass.getIdAnnee());
                if (classCount >= 6) {
                    showError("Impossible d'ajouter une classe. Ce niveau a déjà 6 classes maximum.");
                    return;
                }
                
                ClassDAO.addClass(schoolClass);
                loadClasses();
                showInfo("Classe ajoutée avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de l'ajout de la classe : " + e.getMessage());
            }
        });
    }

    private void openEditClassDialog() {
        SchoolClass selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une classe à modifier.");
            return;
        }

        Dialog<SchoolClass> dialog = new Dialog<>();
        dialog.setTitle("Modifier une classe");
        dialog.setHeaderText("Modifier les informations de la classe");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField(selected.getNomClasse());
        Spinner<Integer> capacitySpinner = new Spinner<>(10, 30, selected.getCapaciteMax());
        ComboBox<Level> levelCombo = new ComboBox<>();
        loadLevels(levelCombo);
        
        // Store the original level ID to check if level is being changed
        int originalLevelId = selected.getIdNiveau();
        
        levelCombo.getItems().stream()
            .filter(level -> level.getIdNiveau() == originalLevelId)
            .findFirst()
            .ifPresent(levelCombo::setValue);

        grid.add(new Label("Classe :"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Capacité :"), 0, 1);
        grid.add(capacitySpinner, 1, 1);
        grid.add(new Label("Niveau :"), 0, 2);
        grid.add(levelCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && levelCombo.getValue() != null) {
                selected.setNomClasse(nameField.getText());
                selected.setCapaciteMax(capacitySpinner.getValue());
                selected.setIdNiveau(levelCombo.getValue().getIdNiveau());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(schoolClass -> {
            try {
                // If level is being changed, check if the new level already has 6 classes
                if (schoolClass.getIdNiveau() != originalLevelId) {
                    int classCount = getClassCountForLevelAndYear(schoolClass.getIdNiveau(), schoolClass.getIdAnnee());
                    if (classCount >= 6) {
                        showError("Impossible de déplacer cette classe. Ce niveau a déjà 6 classes maximum.");
                        return;
                    }
                }
                
                ClassDAO.updateClass(schoolClass);
                loadClasses();
                showInfo("Classe modifiée avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de la modification de la classe : " + e.getMessage());
            }
        });
    }

    private void deleteClass() {
        SchoolClass selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une classe à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer cette classe ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ClassDAO.deleteClass(selected.getIdClasse());
                    loadClasses();
                    showInfo("Classe supprimée avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de la classe : " + e.getMessage());
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

    private void loadLevelFilters() {
        try {
            List<Level> levels = LevelDAO.getAllLevels();
            ObservableList<Level> filterItems = FXCollections.observableArrayList();
            filterItems.add(new Level(-1, "Tous les niveaux"));
            filterItems.addAll(levels);
            levelFilterCombo.setItems(filterItems);
            levelFilterCombo.setValue(filterItems.get(0));
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