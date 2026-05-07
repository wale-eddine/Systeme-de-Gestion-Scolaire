package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import java.util.ArrayList;
import java.util.List;

/**
 * [TEACHER] [UI] Panneau de consultation des Notes de la classe.
 * Rôle : Permet à l'enseignant d'avoir une vue globale des notes de toute la classe pour une de ses matières.
 * 
 * Composants principaux :
 * - Filtres (Classe, Matière, Trimestre) [FILTER].
 * - `tableView` : Affiche toutes les notes de la classe [VUE].
 * 
 * Logique clé :
 * - Récupération et affichage dynamique des données en utilisant `GradeDAO` [LOGIC] [DATABASE].
 */
public class TeacherPanelViewGrades {
    private BorderPane mainPane;
    private int teacherId;
    private TableView<Grade> tableView;
    private ComboBox<SchoolClass> classCombo;
    private ComboBox<Subject> subjectCombo;
    private ComboBox<Integer> trimesterCombo;

    public TeacherPanelViewGrades(BorderPane mainPane, int teacherId) {
        this.mainPane = mainPane;
        this.teacherId = teacherId;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Consulter les notes");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Selection Panel
        HBox selectionBox = new HBox(10);
        selectionBox.setStyle("-fx-padding: 10; -fx-border: 1px solid #ccc;");

        classCombo = new ComboBox<>();
        subjectCombo = new ComboBox<>();
        trimesterCombo = new ComboBox<>();
        trimesterCombo.setItems(FXCollections.observableArrayList(null, 1, 2, 3));
        trimesterCombo.setValue(null);
        trimesterCombo.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "Tous" : value.toString();
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank() || "Tous".equals(string)) {
                    return null;
                }
                return Integer.valueOf(string);
            }
        });

        loadClasses();

        classCombo.setOnAction(e -> {
            SchoolClass selected = classCombo.getValue();
            if (selected != null) {
                loadSubjects(selected);
            }
        });

        subjectCombo.setOnAction(e -> loadGrades());
        trimesterCombo.setOnAction(e -> loadGrades());

        selectionBox.getChildren().addAll(
            new Label("Classe :"), classCombo,
            new Label("Matière :"), subjectCombo,
            new Label("Trimestre :"), trimesterCombo
        );

        // Table for grades
        tableView = new TableView<>();
        createColumns();

        contentBox.getChildren().addAll(titleLabel, selectionBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<Grade, String> studentCol = new TableColumn<>("Élève");
        studentCol.setCellValueFactory(new PropertyValueFactory<>("nomEleve"));

        TableColumn<Grade, String> subjectCol = new TableColumn<>("Matière");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("nomMatiere"));

        TableColumn<Grade, Double> gradeCol = new TableColumn<>("Note (/20)");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("valeur"));

        TableColumn<Grade, Integer> semesterCol = new TableColumn<>("Trimestre");
        semesterCol.setCellValueFactory(new PropertyValueFactory<>("trimestre"));

        tableView.getColumns().addAll(studentCol, subjectCol, gradeCol, semesterCol);
    }

    private void loadClasses() {
        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            if (currentAnnee == null) {
                showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
                return;
            }
            List<SchoolClass> classes = TeacherDAO.getTeacherClassesByYear(teacherId, currentAnnee.getIdAnnee());
            ObservableList<SchoolClass> items = FXCollections.observableArrayList(classes);
            classCombo.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void loadSubjects(SchoolClass schoolClass) {
        try {
            List<Subject> subjects = TeacherDAO.getTeacherSubjectsByClass(teacherId, schoolClass.getIdClasse());
            ObservableList<Subject> items = FXCollections.observableArrayList(subjects);
            subjectCombo.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des matières : " + e.getMessage());
        }
    }

    private void loadGrades() {
        SchoolClass selectedClass = classCombo.getValue();
        Subject selectedSubject = subjectCombo.getValue();
        Integer selectedTrimester = trimesterCombo.getValue();

        if (selectedClass == null || selectedSubject == null) {
            return;
        }

        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            if (currentAnnee == null) {
                showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
                return;
            }
            List<Grade> grades = GradeDAO.getClassGradesForSubject(selectedClass.getIdClasse(), 
                                                                   selectedSubject.getIdMatiere(), currentAnnee.getIdAnnee());
            List<Grade> filteredGrades = new ArrayList<>();
            for (Grade grade : grades) {
                if (selectedTrimester == null || grade.getTrimestre() == selectedTrimester) {
                    filteredGrades.add(grade);
                }
            }
            ObservableList<Grade> items = FXCollections.observableArrayList(filteredGrades);
            tableView.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des notes : " + e.getMessage());
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
