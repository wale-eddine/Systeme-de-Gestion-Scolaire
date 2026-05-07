package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [TEACHER] [UI] Panneau de saisie des Notes.
 * Rôle : Permet à l'enseignant d'attribuer ou modifier des notes pour ses élèves.
 * 
 * Composants principaux :
 * - `ComboBox` pour choisir la Classe et la Matière (limitées aux affectations du prof) [FILTER].
 * - `tableView` : Liste des élèves de la classe sélectionnée [VUE].
 * - Formulaire pour entrer la note, le trimestre et le coefficient [UI] [EVENT].
 * 
 * Logique clé :
 * - Enregistre ou met à jour la note dans la base via `GradeDAO` [ACTION] [DATABASE].
 */
public class TeacherPanelGrades {
    private final BorderPane mainPane;
    private final int teacherId;
    private ComboBox<SchoolClass> classCombo;
    private ComboBox<Subject> subjectCombo;
    private ComboBox<Integer> semesterCombo;
    private VBox gradeEntriesBox;
    private Button saveButton;
    private Button editButton;
    private final Map<Integer, Spinner<Double>> gradeSpinners = new HashMap<>();
    private final Map<Integer, Grade> loadedGradesByStudent = new HashMap<>();
    private boolean hasLoadedExistingGrades;
    private boolean editModeEnabled;

    public TeacherPanelGrades(BorderPane mainPane, int teacherId) {
        this.mainPane = mainPane;
        this.teacherId = teacherId;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Saisie des notes");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        HBox selectionBox = new HBox(10);
        selectionBox.setStyle("-fx-padding: 10; -fx-border: 1px solid #ccc;");

        classCombo = new ComboBox<>();
        classCombo.setPromptText("Sélectionner une classe");
        subjectCombo = new ComboBox<>();
        subjectCombo.setPromptText("Sélectionner une matière");
        semesterCombo = new ComboBox<>();
        semesterCombo.setPromptText("Sélectionner un trimestre");
        semesterCombo.setItems(FXCollections.observableArrayList(1, 2, 3));

        loadClasses();
        classCombo.setOnAction(e -> {
            loadSubjectsForSelectedClass();
            maybeAutoLoadStudents();
        });
        subjectCombo.setOnAction(e -> maybeAutoLoadStudents());
        semesterCombo.setOnAction(e -> maybeAutoLoadStudents());

        selectionBox.getChildren().addAll(
            new Label("Classe :"), classCombo,
            new Label("Matière :"), subjectCombo,
            new Label("Trimestre :"), semesterCombo
        );

        gradeEntriesBox = new VBox(10);
        gradeEntriesBox.setStyle("-fx-padding: 10;");
        ScrollPane scrollPane = new ScrollPane(gradeEntriesBox);
        scrollPane.setFitToWidth(true);

        saveButton = new Button("Enregistrer toutes les notes");
        saveButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        saveButton.setOnAction(e -> saveGrades());
        editButton = new Button("Modifier");
        editButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
        editButton.setDisable(true);
        editButton.setOnAction(e -> enableEditMode());

        HBox actionsBox = new HBox(10, editButton, saveButton);

        contentBox.getChildren().addAll(titleLabel, selectionBox, new Label("Saisir les notes :"), scrollPane, actionsBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void loadClasses() {
        try {
            List<SchoolClass> classes = TeacherDAO.getTeacherClasses(teacherId);
            ObservableList<SchoolClass> items = FXCollections.observableArrayList(classes);
            classCombo.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void maybeAutoLoadStudents() {
        if (classCombo.getValue() != null && subjectCombo.getValue() != null && semesterCombo.getValue() != null) {
            loadStudentsForGrades();
        }
    }

    private void loadStudentsForGrades() {
        SchoolClass selectedClass = classCombo.getValue();
        Subject selectedSubject = subjectCombo.getValue();
        Integer semester = semesterCombo.getValue();
        if (selectedClass == null) {
            showError("Veuillez sélectionner une classe.");
            return;
        }
        if (selectedSubject == null || semester == null) {
            showError("Veuillez sélectionner la matière et le trimestre.");
            return;
        }
        try {
            List<Student> students = StudentDAO.getStudentsByClass(selectedClass.getIdClasse());
            gradeEntriesBox.getChildren().clear();
            gradeSpinners.clear();
            loadedGradesByStudent.clear();
            boolean existingGradesFound = false;
            for (Student student : students) {
                HBox studentBox = new HBox(10);
                studentBox.setStyle("-fx-padding: 5; -fx-border: 1px solid #eee;");
                Label nameLabel = new Label(student.getFullName());
                nameLabel.setPrefWidth(150);
                Spinner<Double> gradeSpinner = new Spinner<>(0, 20, 0, 0.5);
                gradeSpinner.setEditable(true);
                gradeSpinner.setPrefWidth(100);
                gradeSpinner.setId("grade_" + student.getIdEleve());
                AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
                Grade existing = GradeDAO.getStudentGradeForSubjectAndTrimester(
                    student.getIdEleve(), selectedSubject.getIdMatiere(), currentAnnee != null ? currentAnnee.getIdAnnee() : 1, semester
                );
                if (existing != null) {
                    gradeSpinner.getValueFactory().setValue(existing.getValeur());
                    loadedGradesByStudent.put(student.getIdEleve(), existing);
                    existingGradesFound = true;
                }
                gradeSpinners.put(student.getIdEleve(), gradeSpinner);
                studentBox.getChildren().addAll(nameLabel, new Label("/20"), gradeSpinner);
                gradeEntriesBox.getChildren().add(studentBox);
            }

            hasLoadedExistingGrades = existingGradesFound;
            editModeEnabled = !existingGradesFound;
            setSpinnersEditable(editModeEnabled);
            editButton.setDisable(!existingGradesFound);

            if (existingGradesFound) {
                // Existing grades are loaded locked; user can choose to unlock with Modifier.
            }
        } catch (Exception e) {
            showError("Erreur de chargement des élèves : " + e.getMessage());
        }
    }

    private void loadSubjectsForSelectedClass() {
        SchoolClass selectedClass = classCombo.getValue();
        if (selectedClass == null) {
            subjectCombo.getItems().clear();
            subjectCombo.setValue(null);
            return;
        }
        try {
            List<Subject> subjects = TeacherDAO.getTeacherSubjectsByClass(teacherId, selectedClass.getIdClasse());
            ObservableList<Subject> items = FXCollections.observableArrayList(subjects);
            subjectCombo.setItems(items);
            subjectCombo.setValue(null);
            if (items.isEmpty()) {
                gradeEntriesBox.getChildren().clear();
                gradeSpinners.clear();
                loadedGradesByStudent.clear();
            }
        } catch (Exception e) {
            showError("Erreur de chargement des matières : " + e.getMessage());
        }
    }

    private void saveGrades() {
        SchoolClass selectedClass = classCombo.getValue();
        Subject selectedSubject = subjectCombo.getValue();
        Integer semester = semesterCombo.getValue();

        if (selectedClass == null || selectedSubject == null || semester == null) {
            showError("Veuillez sélectionner la classe, la matière et le trimestre.");
            return;
        }
        if (gradeSpinners.isEmpty()) {
            showError("Aucun élève chargé pour cette sélection.");
            return;
        }
        if (hasLoadedExistingGrades && !editModeEnabled) {
            showError("Les notes existent deja. Cliquez sur 'Modifier' pour autoriser les changements.");
            return;
        }

        int saved = 0;
        try {
            for (Map.Entry<Integer, Spinner<Double>> entry : gradeSpinners.entrySet()) {
                int studentId = entry.getKey();
                Spinner<Double> spinner = entry.getValue();
                double gradeValue = spinner.getValue();
                if (gradeValue > 0) {
                    AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
                    Grade grade = new Grade(gradeValue, semester, 1, studentId, selectedSubject.getIdMatiere(), currentAnnee != null ? currentAnnee.getIdAnnee() : 1);
                    Grade existing = loadedGradesByStudent.get(studentId);
                    if (existing != null) {
                        grade.setIdNote(existing.getIdNote());
                        GradeDAO.updateGrade(grade);
                    } else {
                        GradeDAO.addGrade(grade);
                        loadedGradesByStudent.put(studentId, grade);
                    }
                    saved++;
                }
            }

            hasLoadedExistingGrades = true;
            editModeEnabled = false;
            setSpinnersEditable(false);
            editButton.setDisable(false);
            showInfo("Notes enregistrées avec succès ! (" + saved + " notes). Cliquez sur 'Modifier' pour changer a nouveau.");
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement des notes : " + e.getMessage());
        }
    }

    private void enableEditMode() {
        if (gradeSpinners.isEmpty()) {
            showError("Veuillez charger les eleves avant de modifier.");
            return;
        }
        editModeEnabled = true;
        setSpinnersEditable(true);
    }

    private void setSpinnersEditable(boolean editable) {
        for (Spinner<Double> spinner : gradeSpinners.values()) {
            spinner.setDisable(!editable);
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
