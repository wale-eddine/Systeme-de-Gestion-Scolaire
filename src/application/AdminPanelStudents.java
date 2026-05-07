package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import java.time.LocalDate;
import java.util.List;

/**
 * [ADMIN] [UI] Panneau de gestion des Élèves.
 * Rôle : Permet de gérer le profil des élèves (inscription, modification, suppression).
 * 
 * Composants principaux :
 * - `tableView` : Liste complète des élèves avec recherche et filtres par niveau/classe [VUE] [FILTER].
 * - Formulaire de dialogue pour saisir les détails (Nom, Date de naissance, etc.) [UI] [EVENT].
 * 
 * Logique clé :
 * - Interactions avec `StudentDAO` pour persister les données [DATABASE].
 */
public class AdminPanelStudents {
    private BorderPane mainPane;
    private TableView<Student> tableView;
    private ObservableList<Student> studentList;
    private FilteredList<Student> filteredList;
    private TextField searchField;
    private ComboBox<String> assignmentFilterCombo;
    private ComboBox<SchoolClass> classFilterCombo;
    private ComboBox<Level> levelFilterCombo;
    private ObservableList<SchoolClass> allClassFilters;
    private boolean syncingFilters = false;

    public AdminPanelStudents(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        // Title
        Label titleLabel = new Label("Gestion des élèves");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Search Box
        HBox searchBox = new HBox(10);
        searchBox.setStyle("-fx-padding: 4 0 4 0;");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Rechercher:");
        searchField = new TextField();
        searchField.setPromptText("Rechercher par nom ou prénom...");
        searchField.setPrefWidth(250);

        assignmentFilterCombo = new ComboBox<>();
        assignmentFilterCombo.setItems(FXCollections.observableArrayList("Tous", "Affectés", "Non affectés"));
        assignmentFilterCombo.setValue("Tous");

        classFilterCombo = new ComboBox<>();
        classFilterCombo.setPrefWidth(220);
        loadClassFilters();

        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.setPrefWidth(180);
        loadLevelFilters();

        searchBox.getChildren().addAll(
            searchLabel,
            searchField,
            new Label("Statut :"),
            assignmentFilterCombo,
            new Label("Niveau :"),
            levelFilterCombo,
            new Label("Classe :"),
            classFilterCombo
        );

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 2 0 4 0;");
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddStudentDialog());
        editButton.setOnAction(e -> openEditStudentDialog());
        deleteButton.setOnAction(e -> deleteStudent());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        // Table
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();
        loadStudents();

        // Setup filters
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyTextFilter());
        assignmentFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingFilters) {
                return;
            }
            if (newVal != null && !"Tous".equals(newVal)) {
                resetOtherFilters("statut");
            }
            reloadStudentsFromFilters();
        });
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingFilters) {
                return;
            }
            if (newVal != null && newVal.getIdNiveau() != -1) {
                refreshClassFiltersForLevel(newVal);
                resetOtherFilters("niveau");
            } else {
                refreshClassFiltersForLevel(null);
            }
            reloadStudentsFromFilters();
        });
        classFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingFilters) {
                return;
            }
            if (newVal != null && newVal.getIdClasse() != -1) {
                resetOtherFilters("classe");
            }
            reloadStudentsFromFilters();
        });

        contentBox.getChildren().addAll(titleLabel, searchBox, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<Student, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> prenomCol = new TableColumn<>("Prénom");
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        prenomCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, LocalDate> dateCol = new TableColumn<>("Date de naissance");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateNaissance"));
        dateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> adresseCol = new TableColumn<>("Adresse");
        adresseCol.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        adresseCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> telCol = new TableColumn<>("Téléphone parent");
        telCol.setCellValueFactory(new PropertyValueFactory<>("telephoneParent"));
        telCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> classesCol = new TableColumn<>("Classe(s) affectées");
        classesCol.setCellValueFactory(new PropertyValueFactory<>("classesAffectees"));
        classesCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().addAll(nomCol, prenomCol, dateCol, adresseCol, telCol, classesCol);
    }

    private void loadStudents() {
        try {
            List<Student> students = StudentDAO.getAllStudents();
            studentList = FXCollections.observableArrayList(students);
            filteredList = new FilteredList<>(studentList, p -> true);
            tableView.setItems(filteredList);
            applyTextFilter();
        } catch (Exception e) {
            showError("Erreur de chargement des élèves : " + e.getMessage());
        }
    }

    private void loadClassFilters() {
        try {
            List<SchoolClass> classes = ClassDAO.getAllClasses();
            allClassFilters = FXCollections.observableArrayList();
            allClassFilters.add(new SchoolClass(-1, "Toutes les classes", 0, 0, 0));
            allClassFilters.addAll(classes);
            classFilterCombo.setItems(allClassFilters);
            classFilterCombo.setValue(allClassFilters.get(0));
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
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

    private void resetOtherFilters(String changedFilter) {
        syncingFilters = true;
        if (!"statut".equals(changedFilter)) {
            assignmentFilterCombo.setValue("Tous");
        }
        if (!"classe".equals(changedFilter)) {
            classFilterCombo.getItems().stream()
                .filter(c -> c.getIdClasse() == -1)
                .findFirst()
                .ifPresent(classFilterCombo::setValue);
        }
        if (!"niveau".equals(changedFilter)) {
            levelFilterCombo.getItems().stream()
                .filter(l -> l.getIdNiveau() == -1)
                .findFirst()
                .ifPresent(levelFilterCombo::setValue);
        }
        if ("classe".equals(changedFilter)) {
            refreshClassFiltersForLevel(null);
        }
        syncingFilters = false;
    }

    private void refreshClassFiltersForLevel(Level selectedLevel) {
        try {
            ObservableList<SchoolClass> filterItems = FXCollections.observableArrayList();
            filterItems.add(new SchoolClass(-1, "Toutes les classes", 0, 0, 0));
            if (selectedLevel == null || selectedLevel.getIdNiveau() == -1) {
                filterItems.addAll(allClassFilters == null ? FXCollections.observableArrayList() : allClassFilters.filtered(c -> c.getIdClasse() != -1));
            } else {
                filterItems.addAll(ClassDAO.getClassesByLevel(selectedLevel.getIdNiveau()));
            }

            syncingFilters = true;
            classFilterCombo.setItems(filterItems);
            classFilterCombo.setValue(filterItems.get(0));
            syncingFilters = false;
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void reloadStudentsFromFilters() {
        try {
            List<Student> students;
            SchoolClass selectedClass = classFilterCombo.getValue();
            String assignmentFilter = assignmentFilterCombo.getValue();
            Level selectedLevel = levelFilterCombo.getValue();

            if (selectedClass != null && selectedClass.getIdClasse() != -1) {
                students = StudentDAO.getStudentsByClass(selectedClass.getIdClasse());
            } else if ("Non affectés".equals(assignmentFilter)) {
                students = StudentDAO.getUnassignedStudents();
            } else if (selectedLevel != null && selectedLevel.getIdNiveau() != -1) {
                students = StudentDAO.getStudentsByLevel(selectedLevel.getIdNiveau());
            } else if ("Affectés".equals(assignmentFilter)) {
                students = StudentDAO.getAssignedStudents();
            } else {
                students = StudentDAO.getAllStudents();
            }

            studentList = FXCollections.observableArrayList(students);
            filteredList = new FilteredList<>(studentList, p -> true);
            tableView.setItems(filteredList);
            applyTextFilter();
        } catch (Exception e) {
            showError("Erreur de filtrage des élèves : " + e.getMessage());
        }
    }

    private void applyTextFilter() {
        if (filteredList == null) {
            return;
        }
        String query = searchField.getText();
        filteredList.setPredicate(student -> {
            if (query == null || query.isBlank()) {
                return true;
            }
            String lowerCaseFilter = query.toLowerCase();
            return student.getNom().toLowerCase().contains(lowerCaseFilter) ||
                   student.getPrenom().toLowerCase().contains(lowerCaseFilter);
        });
    }

    private void openAddStudentDialog() {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un élève");
        dialog.setHeaderText("Saisir les informations de l'élève");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nomField = new TextField();
        TextField prenomField = new TextField();
        DatePicker dateField = new DatePicker();
        TextField adresseField = new TextField();
        TextField telField = new TextField();

        grid.add(new Label("Nom :"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prénom :"), 0, 1);
        grid.add(prenomField, 1, 1);
        grid.add(new Label("Date de naissance :"), 0, 2);
        grid.add(dateField, 1, 2);
        grid.add(new Label("Adresse :"), 0, 3);
        grid.add(adresseField, 1, 3);
        grid.add(new Label("Téléphone parent :"), 0, 4);
        grid.add(telField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new Student(nomField.getText(), prenomField.getText(), 
                                 dateField.getValue(), adresseField.getText(), telField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(student -> {
            try {
                StudentDAO.addStudent(student);
                reloadStudentsFromFilters();
                showInfo("Élève ajouté avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de l'ajout de l'élève : " + e.getMessage());
            }
        });
    }

    private void openEditStudentDialog() {
        Student selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un élève à modifier.");
            return;
        }

        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Modifier un élève");
        dialog.setHeaderText("Modifier les informations de l'élève");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nomField = new TextField(selected.getNom());
        TextField prenomField = new TextField(selected.getPrenom());
        DatePicker dateField = new DatePicker(selected.getDateNaissance());
        TextField adresseField = new TextField(selected.getAdresse());
        TextField telField = new TextField(selected.getTelephoneParent());

        grid.add(new Label("Nom :"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prénom :"), 0, 1);
        grid.add(prenomField, 1, 1);
        grid.add(new Label("Date de naissance :"), 0, 2);
        grid.add(dateField, 1, 2);
        grid.add(new Label("Adresse :"), 0, 3);
        grid.add(adresseField, 1, 3);
        grid.add(new Label("Téléphone parent :"), 0, 4);
        grid.add(telField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                selected.setNom(nomField.getText());
                selected.setPrenom(prenomField.getText());
                selected.setDateNaissance(dateField.getValue());
                selected.setAdresse(adresseField.getText());
                selected.setTelephoneParent(telField.getText());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(student -> {
            try {
                StudentDAO.updateStudent(student);
                reloadStudentsFromFilters();
                showInfo("Élève modifié avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de la modification de l'élève : " + e.getMessage());
            }
        });
    }

    private void deleteStudent() {
        Student selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un élève à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer cet élève ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    StudentDAO.deleteStudent(selected.getIdEleve());
                    reloadStudentsFromFilters();
                    showInfo("Élève supprimé avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de l'élève : " + e.getMessage());
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
