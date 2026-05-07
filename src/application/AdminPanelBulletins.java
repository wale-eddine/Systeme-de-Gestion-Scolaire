package application;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * [ADMIN] [UI] Panneau de génération des Bulletins Scolaires.
 * Rôle : Calculer les moyennes et générer un rapport (bulletin) pour chaque élève.
 * 
 * Composants principaux :
 * - Choix de la classe et du trimestre pour générer les bulletins [UI].
 * - Zone de texte ou tableau pour afficher un aperçu du bulletin (notes, moyenne, appréciation) [VUE].
 * 
 * Logique clé :
 * - Récupération de toutes les notes via `GradeDAO` [DATABASE].
 * - Algorithme de calcul de la moyenne générale en tenant compte des coefficients [LOGIC].
 */
public class AdminPanelBulletins {
    private BorderPane mainPane;

    public AdminPanelBulletins(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-padding: 20; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Générer un bulletin");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-border: 1px solid #ccc; -fx-padding: 10;");

        ComboBox<SchoolClass> classCombo = new ComboBox<>();
        ComboBox<Student> studentCombo = new ComboBox<>();
        ComboBox<Integer> semesterCombo = new ComboBox<>();
        Button generateButton = new Button("Générer");
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);

        loadClasses(classCombo);
        loadSemesters(semesterCombo);

        classCombo.setOnAction(e -> {
            SchoolClass selected = classCombo.getValue();
            if (selected != null) {
                loadStudentsByClass(selected, studentCombo);
            }
        });

        generateButton.setOnAction(e -> generateBulletin(classCombo, studentCombo, semesterCombo, resultArea));

        grid.add(new Label("Classe :"), 0, 0);
        grid.add(classCombo, 1, 0);
        grid.add(new Label("Élève :"), 0, 1);
        grid.add(studentCombo, 1, 1);
        grid.add(new Label("Trimestre :"), 0, 2);
        grid.add(semesterCombo, 1, 2);
        grid.add(generateButton, 0, 3, 2, 1);

        resultArea.setPrefHeight(400); // Increase default height

        contentBox.getChildren().addAll(titleLabel, grid, new Label("Aperçu du bulletin :"), resultArea);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void generateBulletin(ComboBox<SchoolClass> classCombo, ComboBox<Student> studentCombo,
                                 ComboBox<Integer> semesterCombo, TextArea resultArea) {
        SchoolClass selectedClass = classCombo.getValue();
        Student selectedStudent = studentCombo.getValue();
        Integer semester = semesterCombo.getValue();

        if (selectedClass == null) {
            showError("Veuillez sélectionner une classe.");
            return;
        }

        if (semester == null) {
            showError("Veuillez sélectionner un trimestre.");
            return;
        }

        AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
        if (currentAnnee == null) {
            showError("Veuillez d'abord sélectionner une année scolaire en haut à droite.");
            return;
        }

        StringBuilder result = new StringBuilder();
        try {
            List<Subject> subjects = SubjectDAO.getSubjectsByLevel(selectedClass.getIdNiveau());

            if (selectedStudent == null || selectedStudent.getIdEleve() == -1) {
                List<Student> classStudents = StudentDAO.getStudentsByClass(selectedClass.getIdClasse());
                for (Student student : classStudents) {
                    result.append(buildBulletinForStudent(student, selectedClass, currentAnnee, semester, subjects));
                    result.append("\n");
                }
                resultArea.setText(result.toString());
                showInfo("Bulletins de la classe générés avec succès !");
                return;
            }

            result.append(buildBulletinForStudent(selectedStudent, selectedClass, currentAnnee, semester, subjects));
            resultArea.setText(result.toString());
            showInfo("Bulletin généré avec succès !");

        } catch (Exception e) {
            showError("Erreur lors de la génération du bulletin : " + e.getMessage());
            resultArea.setText("Erreur : " + e.getMessage());
        }
    }

    private String buildBulletinForStudent(Student student, SchoolClass selectedClass, AnneeScolaire currentAnnee,
                                           int semester, List<Subject> subjects) {
        StringBuilder result = new StringBuilder();
        result.append("================================================\n");
        result.append("BULLETIN - ").append(student.getFullName()).append("\n");
        result.append("Année scolaire : ").append(currentAnnee.getNom()).append("\n");
        result.append("Classe : ").append(selectedClass.getNomClasse()).append("\n");
        result.append("Trimestre : ").append(semester).append("\n");
        result.append("================================================\n\n");

        double totalScore = 0;
        int count = 0;

        for (Subject subject : subjects) {
            try {
                Grade grade = GradeDAO.getStudentGradeForSubjectAndTrimester(
                    student.getIdEleve(), subject.getIdMatiere(), currentAnnee.getIdAnnee(), semester);

                if (grade != null) {
                    result.append(subject.getNomMatiere()).append(": ").append(grade.getValeur()).append("/20\n");
                    totalScore += grade.getValeur();
                    count++;
                } else {
                    result.append(subject.getNomMatiere()).append(": Non noté\n");
                }
            } catch (Exception e) {
                result.append(subject.getNomMatiere()).append(": Non noté\n");
            }
        }

        if (count > 0) {
            double average = totalScore / count;
            result.append("\nMoyenne générale : ").append(String.format("%.2f", average)).append("/20\n");

            try {
                List<Student> classStudents = StudentDAO.getStudentsByClass(selectedClass.getIdClasse());
                int rank = calculateRank(classStudents, subjects, semester, student, average);
                result.append("Rang dans la classe : ").append(rank).append("/").append(classStudents.size()).append("\n");
            } catch (Exception e) {
                result.append("Rang : N/A\n");
            }
        }

        return result.toString();
    }

    private int calculateRank(List<Student> students, List<Subject> subjects, int semester, 
                             Student currentStudent, double currentAverage) {
        int rank = 1;

        for (Student student : students) {
            if (student.getIdEleve() == currentStudent.getIdEleve()) {
                continue;
            }

            try {
                double totalScore = 0;
                int count = 0;

                for (Subject subject : subjects) {
                    try {
                        AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
                        Grade grade = GradeDAO.getStudentGradeForSubjectAndTrimester(
                            student.getIdEleve(), subject.getIdMatiere(), currentAnnee.getIdAnnee(), semester);

                        if (grade != null) {
                            totalScore += grade.getValeur();
                            count++;
                        }
                    } catch (Exception e) {
                        // Skip if no grade
                    }
                }

                if (count > 0) {
                    double average = totalScore / count;
                    if (average > currentAverage) {
                        rank++;
                    }
                }
            } catch (Exception e) {
                // Skip this student
            }
        }

        return rank;
    }

    private void loadClasses(ComboBox<SchoolClass> combo) {
        try {
            AnneeScolaire currentAnnee = MenuGeneral.getCurrentAnnee();
            if (currentAnnee == null) return;
            List<SchoolClass> classes = ClassDAO.getClassesByYear(currentAnnee.getIdAnnee());
            ObservableList<SchoolClass> items = FXCollections.observableArrayList(classes);
            combo.setItems(items);
        } catch (Exception e) {
            showError("Erreur de chargement des classes : " + e.getMessage());
        }
    }

    private void loadStudentsByClass(SchoolClass schoolClass, ComboBox<Student> combo) {
        try {
            List<Student> students = StudentDAO.getStudentsByClass(schoolClass.getIdClasse());
            ObservableList<Student> items = FXCollections.observableArrayList();
            items.add(new Student(-1, "les eleves", "Tous", null, "", ""));
            items.addAll(students);
            combo.setItems(items);
            combo.setValue(null);
        } catch (Exception e) {
            showError("Erreur de chargement des élèves : " + e.getMessage());
        }
    }

    private void loadSemesters(ComboBox<Integer> combo) {
        ObservableList<Integer> semesters = FXCollections.observableArrayList(1, 2, 3);
        combo.setItems(semesters);
        combo.setValue(1);
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
