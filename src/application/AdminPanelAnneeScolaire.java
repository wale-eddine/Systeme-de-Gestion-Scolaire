package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.List;

/**
 * [ADMIN] [UI] Panneau de gestion des Années Scolaires.
 * Rôle : Créer de nouvelles années scolaires et définir l'année active du système.
 * 
 * Composants principaux :
 * - `tableView` : Affiche l'historique des années scolaires et indique laquelle est active [VUE].
 * - Bouton "Définir comme active" : Change l'année globale du système [ACTION] [LOGIC].
 * 
 * Logique clé :
 * - Lors de la création d'une nouvelle année, la structure des classes de l'année précédente est copiée automatiquement via `AnneeScolaireDAO` [DATABASE].
 * - Attention : La suppression d'une année efface en cascade toutes les notes et affectations liées [WARNING].
 */
public class AdminPanelAnneeScolaire {
    private BorderPane mainPane;
    private TableView<AnneeScolaire> tableView;
    private ObservableList<AnneeScolaire> anneeList;

    public AdminPanelAnneeScolaire(BorderPane mainPane) {
        this.mainPane = mainPane;
    }

    public void load() {
        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 16; -fx-background-color: linear-gradient(to bottom, #f8fbff, #eef4fb); -fx-background-radius: 16; -fx-border-color: #c9d8e6; -fx-border-radius: 16;");

        Label titleLabel = new Label("Gestion des Années Scolaires");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 4 0 4 0;");
        
        Button addButton = new Button("Ajouter");
        Button setActiveButton = new Button("Définir comme active");
        Button deleteButton = new Button("Supprimer");

        addButton.setOnAction(e -> openAddDialog());
        setActiveButton.setOnAction(e -> setActive());
        deleteButton.setOnAction(e -> deleteAnnee());

        buttonBox.getChildren().addAll(addButton, setActiveButton, deleteButton);

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();
        loadData();

        contentBox.getChildren().addAll(titleLabel, buttonBox, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        mainPane.setCenter(contentBox);
    }

    private void createColumns() {
        TableColumn<AnneeScolaire, String> nomCol = new TableColumn<>("Année Scolaire");
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<AnneeScolaire, String> activeCol = new TableColumn<>("Statut");
        activeCol.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().isEstActive();
            return new SimpleStringProperty(isActive ? "Active" : "-");
        });
        activeCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: green;");

        tableView.getColumns().addAll(nomCol, activeCol);
    }

    private void loadData() {
        try {
            List<AnneeScolaire> annees = AnneeScolaireDAO.getAllAnneesScolaires();
            anneeList = FXCollections.observableArrayList(annees);
            tableView.setItems(anneeList);
        } catch (SQLException e) {
            showError("Erreur de chargement des années scolaires: " + e.getMessage());
        }
    }

    private void openAddDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une Année Scolaire");
        dialog.setHeaderText("Entrez l'année scolaire (format YYYY-YYYY)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nomField = new TextField();
        nomField.setPromptText("Ex: 2026-2027");

        grid.add(new Label("Année:"), 0, 0);
        grid.add(nomField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return nomField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nom -> {
            if (!nom.matches("^\\d{4}-\\d{4}$")) {
                showError("Le format doit être YYYY-YYYY (ex: 2026-2027)");
                return;
            }
            try {
                // Check if the current year string logic is valid
                String[] parts = nom.split("-");
                int y1 = Integer.parseInt(parts[0]);
                int y2 = Integer.parseInt(parts[1]);
                if (y2 != y1 + 1) {
                    showError("Les deux années doivent se suivre (ex: 2026-2027)");
                    return;
                }
                
                AnneeScolaire annee = new AnneeScolaire(nom, false);
                AnneeScolaireDAO.addAnneeScolaire(annee);
                loadData();
                showInfo("Année scolaire ajoutée.");
            } catch (Exception e) {
                showError("Erreur lors de l'ajout: " + e.getMessage());
            }
        });
    }

    private void setActive() {
        AnneeScolaire selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une année scolaire.");
            return;
        }

        try {
            AnneeScolaireDAO.setAsActive(selected.getIdAnnee());
            loadData();
            showInfo("L'année " + selected.getNom() + " est maintenant active.");
        } catch (SQLException e) {
            showError("Erreur lors de la modification du statut: " + e.getMessage());
        }
    }

    private void deleteAnnee() {
        AnneeScolaire selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Veuillez sélectionner une année scolaire à supprimer.");
            return;
        }

        if (selected.isEstActive()) {
            showError("Impossible de supprimer l'année scolaire active. Veuillez d'abord définir une autre année comme active.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Voulez-vous vraiment supprimer cette année scolaire ? Toutes les classes, notes, et inscriptions associées seront également supprimées.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    AnneeScolaireDAO.deleteAnneeScolaire(selected.getIdAnnee());
                    loadData();
                    showInfo("Année scolaire supprimée avec succès !");
                } catch (SQLException e) {
                    showError("Erreur lors de la suppression de l'année scolaire: " + e.getMessage());
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
