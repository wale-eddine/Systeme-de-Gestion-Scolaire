package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * [MAIN] [UI] Barre de navigation et menu principal.
 * Rôle : Gère le routage entre les différents panneaux et conserve le contexte global (Année active, Utilisateur connecté).
 * 
 * Composants principaux :
 * - Boutons de navigation (Profil, Élèves, Notes...) [VUE].
 * - `ComboBox` Année Scolaire : Située en haut à droite pour basculer entre les années (visible uniquement pour Admin) [FILTER].
 * 
 * Logique clé :
 * - `getCurrentAnnee()` : Fournit l'année active à tous les autres panneaux pour le filtrage [LOGIC].
 * - Affiche des menus différents selon que `user.isAdmin()` ou `user.isTeacher()` [SECURITY].
 */
public class MenuGeneral {
	private static final double APP_WIDTH = 1360;
	private static final double APP_HEIGHT = 860;
	private static final double APP_MIN_WIDTH = 1180;
	private static final double APP_MIN_HEIGHT = 760;
	
	// Extended size for student board to accommodate 6 classes without scrolling
	private static final double STUDENT_BOARD_WIDTH = 1600;
	private static final double STUDENT_BOARD_HEIGHT = 1000;

	private Stage stage;
	private User currentUser;
	private MenuBar menuBar = new MenuBar();
	private Button logoutButton;
	private HBox adminActions;
	private BorderPane mainPane;
	private Label welcomeLabel;
	private ComboBox<AnneeScolaire> anneeCombo;
	private static AnneeScolaire currentAnnee;
	
	public static AnneeScolaire getCurrentAnnee() {
		return currentAnnee;
	}
	
	// Store original dimensions to restore later
	private double originalWidth = APP_WIDTH;
	private double originalHeight = APP_HEIGHT;
	
	public MenuGeneral(Stage stage, User user) {
		this.stage = stage;
		this.currentUser = user;
		try {
			LevelDAO.initializePredefinedLevels();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initializeUI();
	}
	
	private void initializeUI() {
		if (currentUser.isAdmin()) {
			createAdminMenu();
		} else if (currentUser.isTeacher()) {
			createTeacherMenu();
		}
	}
	
	private void createAdminMenu() {
		MenuButton bulletinsButton = new MenuButton("Bulletins");
		MenuItem generateBulletinsItem = new MenuItem("Générer un bulletin");
		generateBulletinsItem.setOnAction(e -> openBulletinGeneration());
		bulletinsButton.getItems().add(generateBulletinsItem);

		MenuButton teachersButton = new MenuButton("Enseignants");
		MenuItem manageTeachersItem = new MenuItem("Gérer les enseignants");
		MenuItem assignTeachersItem = new MenuItem("Affecter les enseignants");
		manageTeachersItem.setOnAction(e -> openTeacherManagement());
		assignTeachersItem.setOnAction(e -> openAssignmentManagement());
		teachersButton.getItems().addAll(manageTeachersItem, new SeparatorMenuItem(), assignTeachersItem);

		MenuButton studentsButton = new MenuButton("Élèves");
		MenuItem manageStudentsItem = new MenuItem("Gérer les élèves");
		MenuItem assignStudentsItem = new MenuItem("Affecter aux classes");
		manageStudentsItem.setOnAction(e -> openStudentManagement());
		assignStudentsItem.setOnAction(e -> openStudentAssignment());
		studentsButton.getItems().addAll(manageStudentsItem, new SeparatorMenuItem(), assignStudentsItem);

		Button classesButton = new Button("Gérer les classes");
		classesButton.setOnAction(e -> openClassManagement());
		
		Button anneeButton = new Button("Années Scolaires");
		anneeButton.setOnAction(e -> openAnneeManagement());

		applySegmentStyle(bulletinsButton, "left");
		applySegmentStyle(teachersButton, "middle");
		applySegmentStyle(studentsButton, "middle");
		applySegmentStyle(classesButton, "middle");
		applySegmentStyle(anneeButton, "right");

		adminActions = new HBox(0, bulletinsButton, teachersButton, studentsButton, classesButton, anneeButton);
		adminActions.setAlignment(Pos.CENTER_LEFT);
	}

	private void applySegmentStyle(ButtonBase button, String position) {
		String radius;
		String borderWidth;
		if ("left".equals(position)) {
			radius = "8 0 0 8";
			borderWidth = "1 0.5 1 1";
		} else if ("right".equals(position)) {
			radius = "0 8 8 0";
			borderWidth = "1 1 1 0.5";
		} else {
			radius = "0";
			borderWidth = "1 0.5 1 0.5";
		}

		final String baseStyle =
			"-fx-font-weight: bold;"
			+ "-fx-padding: 6 16 6 16;"
			+ "-fx-min-height: 42;"
			+ "-fx-pref-height: 42;"
			+ "-fx-background-color: #d7e8ff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: " + borderWidth + ";"
			+ "-fx-background-radius: " + radius + ";"
			+ "-fx-border-radius: " + radius + ";"
			+ "-fx-cursor: hand;";

		final String hoverStyle =
			"-fx-font-weight: bold;"
			+ "-fx-padding: 6 16 6 16;"
			+ "-fx-min-height: 42;"
			+ "-fx-pref-height: 42;"
			+ "-fx-background-color: #bddbff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: " + borderWidth + ";"
			+ "-fx-background-radius: " + radius + ";"
			+ "-fx-border-radius: " + radius + ";"
			+ "-fx-cursor: hand;";

		button.setStyle(baseStyle);
		button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
		button.setOnMouseExited(e -> button.setStyle(baseStyle));
	}
	
	private void createTeacherMenu() {
		menuBar.getMenus().clear();
	}
	
	// Helper methods for window resizing
	private void enlargeForStudentBoard() {
		// Store current dimensions before resizing
		originalWidth = stage.getWidth();
		originalHeight = stage.getHeight();
		
		// Enlarge window
		stage.setWidth(STUDENT_BOARD_WIDTH);
		stage.setHeight(STUDENT_BOARD_HEIGHT);
		
		// Center on screen after resize
		stage.centerOnScreen();
	}
	
	private void restoreNormalSize() {
		// Restore original dimensions
		stage.setWidth(originalWidth);
		stage.setHeight(originalHeight);
		
		// Center on screen after restore
		stage.centerOnScreen();
	}
	
	// Admin Functions
	private void openStudentManagement() {
		restoreNormalSize();
		AdminPanelStudents panel = new AdminPanelStudents(mainPane);
		panel.load();
	}
	
	private void openAnneeManagement() {
		restoreNormalSize();
		AdminPanelAnneeScolaire panel = new AdminPanelAnneeScolaire(mainPane);
		panel.load();
	}
	
	private void openClassManagement() {
		restoreNormalSize();
		AdminPanelClasses panel = new AdminPanelClasses(mainPane);
		panel.load();
	}
	
	private void openTeacherManagement() {
		restoreNormalSize();
		AdminPanelTeachers panel = new AdminPanelTeachers(mainPane);
		panel.load();
	}
	
	private void openSubjectManagement() {
		restoreNormalSize();
		AdminPanelSubjects panel = new AdminPanelSubjects(mainPane);
		panel.load();
	}
	
	private void openAssignmentManagement() {
		restoreNormalSize();
		AdminPanelAssignments panel = new AdminPanelAssignments(mainPane);
		panel.load();
	}
	
	private void openStudentAssignment() {
		// Enlarge window specifically for student board to accommodate 6 classes
		enlargeForStudentBoard();
		AdminPanelStudentBoard panel = new AdminPanelStudentBoard(mainPane);
		panel.load();
	}
	
	private void openBulletinGeneration() {
		restoreNormalSize();
		AdminPanelBulletins panel = new AdminPanelBulletins(mainPane);
		panel.load();
	}
	
	// Teacher Functions
	private void openTeacherClasses() {
		restoreNormalSize();
		TeacherPanelClasses panel = new TeacherPanelClasses(mainPane, currentUser.getId());
		panel.load();
	}
	
	private void openGradeEntry() {
		restoreNormalSize();
		TeacherPanelGrades panel = new TeacherPanelGrades(mainPane, currentUser.getId());
		panel.load();
	}
	
	private void openTeacherGradeView() {
		restoreNormalSize();
		TeacherPanelViewGrades panel = new TeacherPanelViewGrades(mainPane, currentUser.getId());
		panel.load();
	}
	
	private void showAbout() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("À propos");
		alert.setHeaderText("Gestion scolaire");
		alert.setContentText("Version 1.0\nApplication de gestion de l'école primaire");
		alert.showAndWait();
	}
	
	private void logout() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Déconnexion");
		alert.setHeaderText(null);
		alert.setContentText("Voulez-vous vraiment vous déconnecter ?");
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				Authentification auth = new Authentification(stage);
				auth.show();
			}
		});
	}
	
	public void show() {
		mainPane = new BorderPane();
		Button enterGradesButton = new Button("Saisir les notes");
		enterGradesButton.setOnAction(e -> openGradeEntry());
		enterGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #d7e8ff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 0.5 1 1;"
			+ "-fx-background-radius: 8 0 0 8;"
			+ "-fx-border-radius: 8 0 0 8;"
			+ "-fx-cursor: hand;"
		);
		enterGradesButton.setOnMouseEntered(e -> enterGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #bddbff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 0.5 1 1;"
			+ "-fx-background-radius: 8 0 0 8;"
			+ "-fx-border-radius: 8 0 0 8;"
			+ "-fx-cursor: hand;"
		));
		enterGradesButton.setOnMouseExited(e -> enterGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #d7e8ff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 0.5 1 1;"
			+ "-fx-background-radius: 8 0 0 8;"
			+ "-fx-border-radius: 8 0 0 8;"
			+ "-fx-cursor: hand;"
		));

		Button viewGradesButton = new Button("Consulter les notes");
		viewGradesButton.setOnAction(e -> openTeacherGradeView());
		viewGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #d7e8ff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 1 1 0.5;"
			+ "-fx-background-radius: 0 8 8 0;"
			+ "-fx-border-radius: 0 8 8 0;"
			+ "-fx-cursor: hand;"
		);
		viewGradesButton.setOnMouseEntered(e -> viewGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #bddbff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 1 1 0.5;"
			+ "-fx-background-radius: 0 8 8 0;"
			+ "-fx-border-radius: 0 8 8 0;"
			+ "-fx-cursor: hand;"
		));
		viewGradesButton.setOnMouseExited(e -> viewGradesButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-padding: 8 16 8 16;"
			+ "-fx-background-color: #d7e8ff;"
			+ "-fx-text-fill: #0f3d6e;"
			+ "-fx-border-color: #6f98c5;"
			+ "-fx-border-width: 1 1 1 0.5;"
			+ "-fx-background-radius: 0 8 8 0;"
			+ "-fx-border-radius: 0 8 8 0;"
			+ "-fx-cursor: hand;"
		));

		logoutButton = new Button("Déconnexion");
		logoutButton.setOnAction(e -> logout());
		logoutButton.setStyle(
			"-fx-font-weight: bold;"
			+ "-fx-background-color: #d64545;"
			+ "-fx-text-fill: white;"
			+ "-fx-background-radius: 8;"
			+ "-fx-padding: 8 14 8 14;"
			+ "-fx-cursor: hand;"
		);
		logoutButton.setOnMouseEntered(e ->
			logoutButton.setStyle(
				"-fx-font-weight: bold;"
				+ "-fx-background-color: #bf3636;"
				+ "-fx-text-fill: white;"
				+ "-fx-background-radius: 8;"
				+ "-fx-padding: 8 14 8 14;"
				+ "-fx-cursor: hand;"
			)
		);
		logoutButton.setOnMouseExited(e ->
			logoutButton.setStyle(
				"-fx-font-weight: bold;"
				+ "-fx-background-color: #d64545;"
				+ "-fx-text-fill: white;"
				+ "-fx-background-radius: 8;"
				+ "-fx-padding: 8 14 8 14;"
				+ "-fx-cursor: hand;"
			)
		);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		
		VBox centerBox = new VBox();
		centerBox.setStyle("-fx-padding: 20; -fx-alignment: center;");
		welcomeLabel = new Label("Bienvenue, " + currentUser.getFullName() + " !");
		welcomeLabel.setStyle("-fx-font-size: 24;");
		centerBox.getChildren().add(welcomeLabel);
		
		// Setup AnneeScolaire Combo
		anneeCombo = new ComboBox<>();
		try {
			List<AnneeScolaire> annees = AnneeScolaireDAO.getAllAnneesScolaires();
			anneeCombo.setItems(FXCollections.observableArrayList(annees));
			AnneeScolaire active = AnneeScolaireDAO.getActiveAnneeScolaire();
			if (active != null) {
				for (AnneeScolaire a : annees) {
					if (a.getIdAnnee() == active.getIdAnnee()) {
						anneeCombo.setValue(a);
						currentAnnee = a;
						break;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		anneeCombo.setOnAction(e -> {
			currentAnnee = anneeCombo.getValue();
			// Optionally refresh the current panel, or let the user click the menu again.
			// For simplicity, we just clear the center or let them re-click.
			mainPane.setCenter(centerBox);
		});
		
		HBox anneeBox = new HBox(10, new Label("Année Scolaire:"), anneeCombo);
		anneeBox.setAlignment(Pos.CENTER_LEFT);
		anneeBox.setStyle("-fx-padding: 0 20 0 20;");

		HBox topBar;
		if (currentUser.isTeacher()) {
			HBox teacherActions = new HBox(0, enterGradesButton, viewGradesButton);
			teacherActions.setAlignment(Pos.CENTER_LEFT);
			topBar = new HBox(12, teacherActions, spacer, logoutButton);
		} else {
			topBar = new HBox(12, adminActions, spacer, anneeBox, logoutButton);
		}
		topBar.setAlignment(Pos.CENTER_LEFT);
		topBar.setStyle("-fx-padding: 0 10 0 0;");
		mainPane.setTop(topBar);
		
		mainPane.setCenter(centerBox);
		
		Scene scene = new Scene(mainPane, APP_WIDTH, APP_HEIGHT);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		stage.setScene(scene);
		stage.setTitle("Gestion scolaire - " + currentUser.getFullName());
		stage.setMinWidth(APP_MIN_WIDTH);
		stage.setMinHeight(APP_MIN_HEIGHT);
		stage.centerOnScreen();
		stage.show();
	}
}