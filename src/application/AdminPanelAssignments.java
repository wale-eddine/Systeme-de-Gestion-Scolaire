package application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * [ADMIN] [UI] Panneau d'affectation des Enseignants.
 * Rôle : Permet à l'administrateur d'assigner des enseignants à des matières spécifiques pour des classes spécifiques.
 * 
 * Composants principaux :
 * - Listes déroulantes pour choisir le niveau et la classe [UI].
 * - Cases à cocher (CheckBox) dans un TableView pour sélectionner rapidement l'enseignant pour chaque matière [VUE].
 * 
 * Logique clé :
 * - Chargement dynamique des matières selon la classe sélectionnée [LOGIC].
 * - Mise à jour instantanée en base de données lors du cochage/décochage via `SubjectDAO` [EVENT] [DATABASE].
 */
public class AdminPanelAssignments {
    private BorderPane mainPane;
    private ComboBox<Teacher> teacherCombo;
    private ComboBox<Level> levelCombo;
    private ComboBox<SchoolClass> classFilterCombo;
    private ComboBox<Subject> subjectFilterCombo;
    private TableView<AssignmentRow> tableView;
    private ObservableList<AssignmentRow> assignmentList;
    private FilteredList<AssignmentRow> filteredList;

    public AdminPanelAssignments(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 20;");

        Label titleLabel = new Label("Affectation des enseignants");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10 0 10 0;");

        teacherCombo = new ComboBox<>();
        teacherCombo.setPrefWidth(200);
        levelCombo = new ComboBox<>();
        levelCombo.setPrefWidth(200);
        classFilterCombo = new ComboBox<>();
        classFilterCombo.setPrefWidth(200);
        subjectFilterCombo = new ComboBox<>();
        subjectFilterCombo.setPrefWidth(200);

        loadTeachers();
        loadLevels();

        teacherCombo.setOnAction(e -> loadTableData());
        levelCombo.setOnAction(e -> {
            updateFiltersForLevel();
            loadTableData();
        });
        classFilterCombo.setOnAction(e -> applyFilters());
        subjectFilterCombo.setOnAction(e -> applyFilters());

        grid.add(new Label("Enseignant :"), 0, 0); 
        grid.add(teacherCombo, 1, 0);
        grid.add(new Label("Niveau :"), 2, 0);
        grid.add(levelCombo, 3, 0);
        grid.add(new Label("Classe :"), 0, 1);
        grid.add(classFilterCombo, 1, 1);
        grid.add(new Label("Matière :"), 2, 1);
        grid.add(subjectFilterCombo, 3, 1);

        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();

        contentBox.getChildren().addAll(titleLabel, grid, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<AssignmentRow, Boolean> checkCol = new TableColumn<>("Affecté");
        checkCol.setCellValueFactory(cellData -> cellData.getValue().assignedProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setPrefWidth(80);
        checkCol.setMaxWidth(80);
        checkCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<AssignmentRow, String> matCol = new TableColumn<>("Matière");
        matCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSubject().getNomMatiere()));
        matCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<AssignmentRow, String> nivCol = new TableColumn<>("Niveau");
        nivCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLevel().getNomNiveau()));
        nivCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<AssignmentRow, String> classCol = new TableColumn<>("Classe");
        classCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSchoolClass().getNomClasse()));
        classCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().addAll(checkCol, matCol, nivCol, classCol);
    }

    private void loadTeachers() {
        try {
            List<Teacher> teachers = TeacherDAO.getAllTeachers();
            teacherCombo.setItems(FXCollections.observableArrayList(teachers));
        } catch (Exception e) {
            showError("Erreur de chargement des enseignants : " + e.getMessage());
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

    private void loadTableData() {
        Teacher teacher = teacherCombo.getValue();
        Level level = levelCombo.getValue();
        AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();

        if (teacher == null || level == null) {
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }

        if (currentAnnee == null) {
            showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
            return;
        }

        try {
            List<AssignmentRow> rows = new ArrayList<>();
            List<SchoolClass> classes = ClassDAO.getClassesByLevelAndYear(level.getIdNiveau(), currentAnnee.getIdAnnee());
            List<Subject> subjects = SubjectDAO.getSubjectsByLevel(level.getIdNiveau());

            for (SchoolClass sc : classes) {
                for (Subject sub : subjects) {
                    boolean isAssigned = SubjectDAO.isTeacherAssignedToClassSubject(teacher.getIdEnseignant(), sc.getIdClasse(), sub.getIdMatiere());
                    AssignmentRow row = new AssignmentRow(isAssigned, teacher, sub, sc, level);
                    
                    row.assignedProperty().addListener((obs, wasAssigned, isNowAssigned) -> {
                        try {
                            if (isNowAssigned) {
                                SubjectDAO.assignTeacherToClassSubject(teacher.getIdEnseignant(), sc.getIdClasse(), sub.getIdMatiere());
                            } else {
                                SubjectDAO.removeTeacherFromClassSubject(teacher.getIdEnseignant(), sc.getIdClasse(), sub.getIdMatiere());
                            }
                        } catch (SQLException e) {
                            showError("Erreur d'affectation : " + e.getMessage());
                            // Revert checkbox state
                            row.setAssigned(wasAssigned);
                        }
                    });
                    
                    rows.add(row);
                }
            }

            assignmentList = FXCollections.observableArrayList(rows);
            filteredList = new FilteredList<>(assignmentList, p -> true);
            tableView.setItems(filteredList);
            applyFilters();

        } catch (SQLException e) {
            showError("Erreur lors du chargement des affectations : " + e.getMessage());
        }
    }

    private void updateFiltersForLevel() {
        Level level = levelCombo.getValue();
        AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
        if (level == null || currentAnnee == null) {
            classFilterCombo.setItems(FXCollections.observableArrayList());
            subjectFilterCombo.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            List<SchoolClass> classes = ClassDAO.getClassesByLevelAndYear(level.getIdNiveau(), currentAnnee.getIdAnnee());
            ObservableList<SchoolClass> classItems = FXCollections.observableArrayList();
            classItems.add(new SchoolClass(-1, "Toutes les classes", 0, 0, 0));
            classItems.addAll(classes);
            classFilterCombo.setItems(classItems);
            classFilterCombo.setValue(classItems.get(0));

            List<Subject> subjects = SubjectDAO.getSubjectsByLevel(level.getIdNiveau());
            ObservableList<Subject> subjectItems = FXCollections.observableArrayList();
            subjectItems.add(new Subject(-1, "Toutes les matières", -1));
            subjectItems.addAll(subjects);
            subjectFilterCombo.setItems(subjectItems);
            subjectFilterCombo.setValue(subjectItems.get(0));
        } catch (Exception e) {
            showError("Erreur de mise à jour des filtres : " + e.getMessage());
        }
    }

    private void applyFilters() {
        if (filteredList == null) return;
        SchoolClass selClass = classFilterCombo.getValue();
        Subject selSub = subjectFilterCombo.getValue();
        
        filteredList.setPredicate(row -> {
            boolean matchClass = selClass == null || selClass.getIdClasse() == -1 || row.getSchoolClass().getIdClasse() == selClass.getIdClasse();
            boolean matchSub = selSub == null || selSub.getIdMatiere() == -1 || row.getSubject().getIdMatiere() == selSub.getIdMatiere();
            return matchClass && matchSub;
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class AssignmentRow {
        private final BooleanProperty assigned;
        private final Teacher teacher;
        private final Subject subject;
        private final SchoolClass schoolClass;
        private final Level level;

        public AssignmentRow(boolean assigned, Teacher teacher, Subject subject, SchoolClass schoolClass, Level level) {
            this.assigned = new SimpleBooleanProperty(assigned);
            this.teacher = teacher;
            this.subject = subject;
            this.schoolClass = schoolClass;
            this.level = level;
        }

        public boolean isAssigned() { return assigned.get(); }
        public void setAssigned(boolean value) { assigned.set(value); }
        public BooleanProperty assignedProperty() { return assigned; }

        public Teacher getTeacher() { return teacher; }
        public Subject getSubject() { return subject; }
        public SchoolClass getSchoolClass() { return schoolClass; }
        public Level getLevel() { return level; }
    }
}
