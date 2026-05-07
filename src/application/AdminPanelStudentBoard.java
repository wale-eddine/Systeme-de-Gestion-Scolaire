package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * [ADMIN] [UI] Tableau de bord d'affectation des Élèves.
 * Rôle : Assigner rapidement les élèves à leurs classes respectives pour l'année en cours.
 * 
 * Composants principaux :
 * - Interface double (Drag & Drop ou sélection) pour déplacer les élèves de "Non affectés" vers une "Classe cible" [VUE] [ACTION].
 * - Filtres par niveau pour faciliter la recherche [FILTER].
 * 
 * Logique clé :
 * - Mise à jour massive via `ClassDAO.assignStudentToClass` pour affecter les élèves [LOGIC] [DATABASE].
 */
public class AdminPanelStudentBoard {
    private static final String[] ASSIGN_ARROWS = {"↖", "↗", "←", "→", "↙", "↘"};
    private static final String[] REMOVE_ARROWS = {"↘", "↙", "→", "←", "↗", "↖"};
    private static final Pos[] BUTTON_ALIGNMENT = {
        Pos.CENTER_RIGHT, Pos.CENTER_LEFT,
        Pos.CENTER_RIGHT, Pos.CENTER_LEFT,
        Pos.CENTER_RIGHT, Pos.CENTER_LEFT
    };
    private static final boolean[] ASSIGN_LAST = {
        true, false,
        true, false,
        true, false
    };

    private final BorderPane mainPane;
    private ComboBox<Level> levelCombo;
    private TableView<Student> unassignedTable;
    private VBox boardContainer;

    public AdminPanelStudentBoard(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f7fbff, #edf3f9);");

        Label titleLabel = new Label("Tableau d'affectation des élèves");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        levelCombo = new ComboBox<>();
        levelCombo.setPrefWidth(220);
        loadLevels();

        Button refreshButton = new Button("↻");
        refreshButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> refreshBoard());

        HBox headerRow = new HBox(12, new Label("Level"), levelCombo, refreshButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        boardContainer = new VBox(10);
        boardContainer.setPadding(new Insets(4));

        ScrollPane boardScroll = new ScrollPane(boardContainer);
        boardScroll.setFitToWidth(true);
        boardScroll.setFitToHeight(true);
        boardScroll.setPannable(false);
        boardScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardScroll.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(titleLabel, headerRow, boardScroll);
        VBox.setVgrow(boardScroll, Priority.ALWAYS);

        levelCombo.setOnAction(e -> refreshBoard());
        if (!levelCombo.getItems().isEmpty() && levelCombo.getValue() == null) {
            levelCombo.getSelectionModel().selectFirst();
        }
        refreshBoard();

        mainPane.setCenter(root);
    }

    private void refreshBoard() {
        Level selectedLevel = levelCombo.getValue();
        if (selectedLevel == null) {
            boardContainer.getChildren().clear();
            return;
        }

        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            if (currentAnnee == null) {
                showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
                return;
            }
            List<SchoolClass> classes = ClassDAO.getClassesByLevelAndYear(selectedLevel.getIdNiveau(), currentAnnee.getIdAnnee());
            List<Student> unassignedStudents = StudentDAO.getUnassignedStudents();
            boardContainer.getChildren().clear();

            VBox centerCard = buildCenterCard(unassignedStudents);
            HBox topRow = new HBox(12,
                createSlotCard(classes, 0),
                spacer(),
                createSlotCard(classes, 1)
            );
            topRow.setAlignment(Pos.CENTER);

            HBox middleRow = new HBox(12,
                createSlotCard(classes, 2),
                centerCard,
                createSlotCard(classes, 3)
            );
            middleRow.setAlignment(Pos.CENTER);

            HBox bottomRow = new HBox(12,
                createSlotCard(classes, 4),
                spacer(),
                createSlotCard(classes, 5)
            );
            bottomRow.setAlignment(Pos.CENTER);

            boardContainer.getChildren().addAll(topRow, middleRow, bottomRow);

            if (classes.size() > 6) {
                HBox extraRow = new HBox(14);
                extraRow.setAlignment(Pos.CENTER);
                for (int i = 6; i < classes.size(); i++) {
                    extraRow.getChildren().add(createClassCard(classes.get(i), "↓", "↑", Pos.CENTER, false));
                }
                boardContainer.getChildren().add(extraRow);
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement du tableau d'affectation : " + e.getMessage());
        }
    }

    private Region spacer() {
        Region r = new Region();
        r.setPrefWidth(300);
        r.setMinWidth(280);
        return r;
    }

    private VBox createSlotCard(List<SchoolClass> classes, int index) throws Exception {
        if (index >= classes.size()) {
            VBox empty = new VBox();
            empty.setPrefSize(250, 230);
            empty.setStyle("-fx-border-color: transparent;");
            return empty;
        }
        return createClassCard(
            classes.get(index),
            ASSIGN_ARROWS[index],
            REMOVE_ARROWS[index],
            BUTTON_ALIGNMENT[index],
            ASSIGN_LAST[index]
        );
    }

    private VBox buildCenterCard(List<Student> unassignedStudents) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(8));
        card.setPrefWidth(300);
        card.setMinWidth(280);
        card.setMinHeight(230);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #1e88e5; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label("Non affectés");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

        unassignedTable = new TableView<>();
        configureStudentTable(unassignedTable);
        unassignedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unassignedTable.setItems(FXCollections.observableArrayList(unassignedStudents));
        unassignedTable.setPrefHeight(170);

        card.getChildren().addAll(title, unassignedTable);
        VBox.setVgrow(unassignedTable, Priority.ALWAYS);
        return card;
    }

    private VBox createClassCard(SchoolClass schoolClass, String assignSymbol, String removeSymbol,
                                 Pos buttonAlignment, boolean assignLast) throws Exception {
        List<Student> currentStudents = StudentDAO.getStudentsByClass(schoolClass.getIdClasse());
        ObservableList<Student> classStudents = FXCollections.observableArrayList(currentStudents);

        VBox card = new VBox(8);
        card.setPadding(new Insets(8));
        card.setPrefWidth(250);
        card.setMinWidth(240);
        card.setMinHeight(230);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b0bec5; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label(schoolClass.getNomClasse() + " (" + currentStudents.size() + "/" + schoolClass.getCapaciteMax() + ")");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        TableView<Student> classTable = new TableView<>();
        configureStudentTable(classTable);
        classTable.setItems(classStudents);
        classTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        classTable.setPrefHeight(150);

        Button assignButton = new Button(assignSymbol);
        assignButton.setPrefWidth(54);
        assignButton.setPrefHeight(34);
        assignButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
        assignButton.setOnAction(e -> assignSelectedToClass(schoolClass));

        Button removeButton = new Button(removeSymbol);
        removeButton.setPrefWidth(54);
        removeButton.setPrefHeight(34);
        removeButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #ffebee; -fx-text-fill: #c62828;");
        removeButton.setOnAction(e -> removeSelectedFromClass(classTable, schoolClass));

        HBox buttonRow = assignLast
            ? new HBox(8, removeButton, assignButton)
            : new HBox(8, assignButton, removeButton);
        buttonRow.setAlignment(buttonAlignment);

        card.getChildren().addAll(title, classTable, buttonRow);
        VBox.setVgrow(classTable, Priority.ALWAYS);
        return card;
    }

    private void configureStudentTable(TableView<Student> table) {
        TableColumn<Student, String> lastNameCol = new TableColumn<>("Last Name");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("nom"));

        TableColumn<Student, String> firstNameCol = new TableColumn<>("First Name");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));

        TableColumn<Student, String> phoneCol = new TableColumn<>("Parent Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("telephoneParent"));

        table.getColumns().setAll(lastNameCol, firstNameCol, phoneCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Aucun élève"));
    }

    private void assignSelectedToClass(SchoolClass schoolClass) {
        ObservableList<Student> selectedStudents = unassignedTable.getSelectionModel().getSelectedItems();
        if (selectedStudents == null || selectedStudents.isEmpty()) {
            showError("Sélectionnez d'abord un ou plusieurs élèves non affectés.");
            return;
        }

        try {
            int currentCount = ClassDAO.getStudentCountInClass(schoolClass.getIdClasse());
            if (currentCount + selectedStudents.size() > schoolClass.getCapaciteMax()) {
                showError("Cette classe n'a pas assez de place pour tous les élèves sélectionnés.");
                return;
            }

            for (Student student : new ArrayList<>(selectedStudents)) {
                ClassDAO.moveStudentToClass(student.getIdEleve(), schoolClass.getIdClasse());
            }

            refreshBoard();
            showInfo("Élèves affectés à " + schoolClass.getNomClasse() + " .");
        } catch (Exception e) {
            showError("Erreur lors de l'affectation des élèves : " + e.getMessage());
        }
    }

    private void removeSelectedFromClass(TableView<Student> classTable, SchoolClass schoolClass) {
        ObservableList<Student> selectedStudents = classTable.getSelectionModel().getSelectedItems();
        if (selectedStudents == null || selectedStudents.isEmpty()) {
            showError("Sélectionnez d'abord un ou plusieurs élèves de la classe.");
            return;
        }

        try {
            for (Student student : new ArrayList<>(selectedStudents)) {
                ClassDAO.removeStudentFromAllClasses(student.getIdEleve());
            }

            refreshBoard();
            showInfo("Les élèves sélectionnés ont été remis dans la liste des non affectés.");
        } catch (Exception e) {
            showError("Erreur lors du retrait des élèves : " + e.getMessage());
        }
    }

    private void loadLevels() {
        try {
            List<Level> levels = LevelDAO.getAllLevels();
            levelCombo.setItems(FXCollections.observableArrayList(levels));
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
