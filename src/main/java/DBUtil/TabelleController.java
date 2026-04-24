package DBUtil;
import User.Model.User;
import User.Repository.JPAUserRepository;
import User.Repository.RepositoryException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TabelleController implements Initializable {

	private JPAUserRepository db = new JPAUserRepository();

	@FXML private TableView<User> userTable;
	@FXML private TableColumn<User, String> usernameColumn;
	@FXML private TableColumn<User, String> displaynameColumn;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		try {
			// CellValueFactory mit Callbacks für korrekte Anzeige
			usernameColumn.setCellValueFactory(cellData ->
				new SimpleStringProperty(cellData.getValue().getUsername()));
			displaynameColumn.setCellValueFactory(cellData ->
				new SimpleStringProperty(cellData.getValue().getDisplayname() != null ?
					cellData.getValue().getDisplayname() : ""));

			// Nur eine Zeile auswählbar machen
			userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

			ladeUserListe();
		} catch (Exception e) {
			System.err.println("Fehler bei der Initialisierung: " + e.getMessage());
			e.printStackTrace();
			showError("Fehler beim Laden der Benutzerliste: " + e.getMessage());
		}
	}

	private void ladeUserListe() {
		try {
			List<User> users = db.getAllUsers();

			if (users.isEmpty()) {
				System.out.println("Keine User in der Datenbank gefunden.");
			}

			ObservableList<User> userListe = FXCollections.observableArrayList(users);
			userTable.setItems(userListe);
		} catch (RepositoryException e) {
			System.err.println("Datenbankfehler: " + e.getMessage());
			e.printStackTrace();
			showError("Datenbankfehler beim Laden der Benutzer: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unerwarteter Fehler: " + e.getMessage());
			e.printStackTrace();
			showError("Unerwarteter Fehler: " + e.getMessage());
		}
	}

	@FXML
	private void handleNewUser() {
		try {
			// db-verwaltung-view.fxml laden
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/DBUtil/db-verwaltung-view.fxml"));
			Parent root = loader.load();

			Stage stage = new Stage();
			stage.setTitle("Neuer Benutzer");
			stage.setScene(new Scene(root, 400, 350));
			stage.showAndWait();

			// Nach dem Fenster schließen: Liste neu laden
			ladeUserListe();
		} catch (IOException e) {
			System.err.println("FXML-Fehler: " + e.getMessage());
			e.printStackTrace();
			showError("Benutzer-Fenster konnte nicht geöffnet werden: " + e.getMessage());
		}
	}

	@FXML
	private void handleDeleteUser() {
		User selectedUser = userTable.getSelectionModel().getSelectedItem();
		if (selectedUser == null) {
			showError("Bitte wähle einen Benutzer zum Löschen aus.");
			return;
		}

		// Bestätigungsdialog
		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
		confirmation.setTitle("Löschen bestätigen");
		confirmation.setHeaderText("Benutzer löschen?");
		confirmation.setContentText("Möchtest du den Benutzer '" + selectedUser.getUsername() + "' wirklich löschen?");

		Optional<ButtonType> result = confirmation.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			try {
				db.deleteUser(selectedUser.getUsername()); // Repository-Methode verwenden
				ladeUserListe(); // Liste neu laden
			} catch (RepositoryException e) {
				showError("Fehler beim Löschen: " + e.getMessage());
			}
		}
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Fehler");
		alert.setContentText(message);
		alert.show();
	}

	public JPAUserRepository getDb() {
		return db;
	}

	public void setDb(JPAUserRepository db) {
		this.db = db;
	}
}
