package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [ADMIN] [UI] Panneau de gestion des Enseignants.
 * Rôle : Permet à l'administrateur de voir, ajouter, modifier et supprimer des enseignants.
 * 
 * Composants principaux :
 * - `tableView` : Affiche la liste des enseignants et leurs affectations [VUE].
 * - `searchField`, `assignmentFilterCombo`, `levelFilterCombo`, `classFilterCombo` : Filtres dynamiques [FILTER].
 * 
 * Logique clé :
 * - Le filtrage par classe et niveau est dynamique selon l'Année Scolaire active [LOGIC].
 * - Les données sont récupérées via `TeacherDAO`.
 */
public class AdminPanelTeachers {
    private BorderPane mainPane;
    private TableView<Teacher> tableView;
    private ObservableList<Teacher> teacherList;
    private FilteredList<Teacher> filteredList;
    private ComboBox<Level> levelFilterCombo;
    private ComboBox<SchoolClass> classFilterCombo;
    private ObservableList<SchoolClass> allClassFilters;
    private boolean syncingFilters = false;

    public AdminPanelTeachers(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Gestion des enseignants");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Search Box
        HBox searchBox = new HBox(10);
        searchBox.setStyle("-fx-padding: 4 0 4 0;");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Rechercher:");
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher par nom ou prénom...");
        searchField.setPrefWidth(250);

        ComboBox<String> assignmentFilterCombo = new ComboBox<>();
        assignmentFilterCombo.setItems(FXCollections.observableArrayList("Tous", "Assigne", "Non assigne"));
        assignmentFilterCombo.setValue("Tous");

        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.setPrefWidth(180);
        loadLevelFilters();

        classFilterCombo = new ComboBox<>();
        classFilterCombo.setPrefWidth(220);
        loadClassFilters();

        searchBox.getChildren().addAll(
            searchLabel,
            searchField,
            new Label("Statut :"), assignmentFilterCombo,
            new Label("Niveau :"), levelFilterCombo,
            new Label("Classe :"), classFilterCombo
        );

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 2 0 4 0;");
        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddTeacherDialog());
        editButton.setOnAction(e -> openEditTeacherDialog());
        deleteButton.setOnAction(e -> deleteTeacher());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();
        loadTeachers();

        // Setup filters
        Runnable applyFilters = () -> {
            String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String status = assignmentFilterCombo.getValue();
            Level selectedLevel = levelFilterCombo.getValue();
            SchoolClass selectedClass = classFilterCombo.getValue();

            final Set<String> levelClassNames;
            if (selectedLevel != null && selectedLevel.getIdNiveau() != -1 && allClassFilters != null) {
                levelClassNames = allClassFilters.stream()
                    .filter(c -> c.getIdClasse() != -1 && c.getIdNiveau() == selectedLevel.getIdNiveau())
                    .map(SchoolClass::getNomClasse)
                    .collect(Collectors.toSet());
            } else {
                levelClassNames = null;
            }

            filteredList.setPredicate(teacher -> {
                boolean matchesText = query.isBlank()
                    || teacher.getNom().toLowerCase().contains(query)
                    || teacher.getPrenom().toLowerCase().contains(query);

                boolean matchesStatus;
                if ("Assigne".equals(status)) {
                    matchesStatus = "Assigne".equals(teacher.getStatutAffectation());
                } else if ("Non assigne".equals(status)) {
                    matchesStatus = "Non assigne".equals(teacher.getStatutAffectation());
                } else {
                    matchesStatus = true;
                }

                String teacherClasses = teacher.getClassesAffectees() == null ? "" : teacher.getClassesAffectees();
                boolean matchesClass = selectedClass == null
                    || selectedClass.getIdClasse() == -1
                    || teacherClasses.contains(selectedClass.getNomClasse());

                boolean matchesLevel = selectedLevel == null
                    || selectedLevel.getIdNiveau() == -1
                    || (levelClassNames != null && levelClassNames.stream().anyMatch(teacherClasses::contains));

                return matchesText && matchesStatus && matchesClass && matchesLevel;
            });
        };
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        assignmentFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (syncingFilters) {
                return;
            }
            refreshClassFiltersForLevel(newVal);
            applyFilters.run();
        });
        classFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());

        contentBox.getChildren().addAll(titleLabel, searchBox, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<Teacher, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Teacher, String> prenomCol = new TableColumn<>("Prénom");
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        prenomCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Teacher, String> telCol = new TableColumn<>("Téléphone");
        telCol.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        telCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Teacher, String> userCol = new TableColumn<>("Identifiant");
        userCol.setCellValueFactory(new PropertyValueFactory<>("nomUtilisateur"));
        userCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Teacher, String> classesCol = new TableColumn<>("Classes assignees");
        classesCol.setCellValueFactory(new PropertyValueFactory<>("classesAffectees"));
        classesCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().addAll(nomCol, prenomCol, telCol, userCol, classesCol);
    }

    private void loadTeachers() {
        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            List<Teacher> teachers;
            if (currentAnnee != null) {
                teachers = TeacherDAO.getAllTeachersByYear(currentAnnee.getIdAnnee());
            } else {
                teachers = TeacherDAO.getAllTeachers();
            }
            teacherList = FXCollections.observableArrayList(teachers);
            filteredList = new FilteredList<>(teacherList, p -> true);
            tableView.setItems(filteredList);
        } catch (Exception e) {
            showError("Erreur de chargement des enseignants : " + e.getMessage());
        }
    }

    private void loadClassFilters() {
        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            List<SchoolClass> classes;
            if (currentAnnee != null) {
                classes = ClassDAO.getClassesByYear(currentAnnee.getIdAnnee());
            } else {
                classes = ClassDAO.getAllClasses();
            }
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
            ObservableList<Level> levelItems = FXCollections.observableArrayList();
            levelItems.add(new Level(-1, "Tous les niveaux"));
            levelItems.addAll(levels);
            levelFilterCombo.setItems(levelItems);
            levelFilterCombo.setValue(levelItems.get(0));
        } catch (Exception e) {
            showError("Erreur de chargement des niveaux : " + e.getMessage());
        }
    }

    private void refreshClassFiltersForLevel(Level selectedLevel) {
        if (allClassFilters == null) {
            return;
        }

        ObservableList<SchoolClass> classItems = FXCollections.observableArrayList();
        classItems.add(new SchoolClass(-1, "Toutes les classes", 0, 0, 0));

        if (selectedLevel == null || selectedLevel.getIdNiveau() == -1) {
            classItems.addAll(allClassFilters.filtered(c -> c.getIdClasse() != -1));
        } else {
            classItems.addAll(allClassFilters.filtered(
                c -> c.getIdClasse() != -1 && c.getIdNiveau() == selectedLevel.getIdNiveau()
            ));
        }

        syncingFilters = true;
        classFilterCombo.setItems(classItems);
        classFilterCombo.setValue(classItems.get(0));
        syncingFilters = false;
    }

    private void openAddTeacherDialog() {
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un enseignant");
        dialog.setHeaderText("Saisir les informations de l'enseignant");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nomField = new TextField();
        TextField prenomField = new TextField();
        TextField telField = new TextField();
        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        PasswordField confirmPassField = new PasswordField();

        grid.add(new Label("Nom :"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prénom :"), 0, 1);
        grid.add(prenomField, 1, 1);
        grid.add(new Label("Téléphone :"), 0, 2);
        grid.add(telField, 1, 2);
        grid.add(new Label("Identifiant :"), 0, 3);
        grid.add(userField, 1, 3);
        grid.add(new Label("Mot de passe :"), 0, 4);
        grid.add(passField, 1, 4);
        grid.add(new Label("Confirmer le mot de passe :"), 0, 5);
        grid.add(confirmPassField, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String password = passField.getText().trim();
                String confirmPassword = confirmPassField.getText().trim();
                if (password.isEmpty()) {
                    showError("Le mot de passe ne peut pas être vide.");
                    return null;
                }
                if (!password.equals(confirmPassword)) {
                    showError("Les mots de passe ne correspondent pas.");
                    return null;
                }
                return new Teacher(0, "", nomField.getText().trim(), prenomField.getText().trim(),
                                 telField.getText().trim(), userField.getText().trim(), password);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(teacher -> {
            try {
                TeacherDAO.addTeacher(teacher);
                loadTeachers();
                showInfo("Enseignant ajouté avec succès !\n\nIdentifiant : " + teacher.getNomUtilisateur() + "\nMot de passe : " + teacher.getMotDePasse());
            } catch (Exception e) {
                showError("Erreur lors de l'ajout de l'enseignant : " + e.getMessage());
            }
        });
    }

    private void openEditTeacherDialog() {
        Teacher selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un enseignant à modifier.");
            return;
        }

        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Modifier un enseignant");
        dialog.setHeaderText("Modifier les informations de l'enseignant");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nomField = new TextField(selected.getNom());
        TextField prenomField = new TextField(selected.getPrenom());
        TextField telField = new TextField(selected.getTelephone());
        TextField userField = new TextField(selected.getNomUtilisateur());
        PasswordField passField = new PasswordField();
        passField.setPromptText("Laisser vide pour conserver le mot de passe");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Laisser vide pour conserver le mot de passe");

        grid.add(new Label("Nom :"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Prénom :"), 0, 1);
        grid.add(prenomField, 1, 1);
        grid.add(new Label("Téléphone :"), 0, 2);
        grid.add(telField, 1, 2);
        grid.add(new Label("Identifiant :"), 0, 3);
        grid.add(userField, 1, 3);
        grid.add(new Label("Mot de passe :"), 0, 4);
        grid.add(passField, 1, 4);
        grid.add(new Label("Confirmer le mot de passe :"), 0, 5);
        grid.add(confirmPassField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String newPassword = passField.getText().trim();
                String confirmPassword = confirmPassField.getText().trim();
                if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
                    if (!newPassword.equals(confirmPassword)) {
                        showError("Les mots de passe ne correspondent pas.");
                        return null;
                    }
                    selected.setMotDePasse(newPassword);
                }
                selected.setNom(nomField.getText());
                selected.setPrenom(prenomField.getText());
                selected.setTelephone(telField.getText());
                selected.setNomUtilisateur(userField.getText());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(teacher -> {
            try {
                TeacherDAO.updateTeacher(teacher);
                loadTeachers();
                showInfo("Enseignant modifié avec succès !");
            } catch (Exception e) {
                showError("Erreur lors de la modification de l'enseignant : " + e.getMessage());
            }
        });
    }

    private void deleteTeacher() {
        Teacher selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner un enseignant à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer cet enseignant ?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    TeacherDAO.deleteTeacher(selected.getIdEnseignant());
                    loadTeachers();
                    showInfo("Enseignant supprimé avec succès !");
                } catch (Exception e) {
                    showError("Erreur lors de la suppression de l'enseignant : " + e.getMessage());
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
