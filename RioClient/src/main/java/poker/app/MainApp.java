package poker.app;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import netgame.common.Client;
import poker.app.model.PokerHub;
import poker.app.view.ClientServerStartController;
import poker.app.view.PokerTableController;
import poker.app.view.RootLayoutController;
import pokerBase.Action;
import pokerBase.GamePlay;
import pokerBase.Player;
import pokerBase.Table;
import pokerEnums.eAction;

public class MainApp extends Application {

	private Stage primaryStage;
	private BorderPane rootLayout;

	private PokerHub pHub = null;
	private PokerClient pClient = null;

	private PokerTableController pokerController = null;
	private RootLayoutController rootController = null;

	private boolean isServer = false;

	private Player appPlayer;

	public int GetPlayerID() {
		return pClient.getID();
	}

	public Player getPlayer() {
		return appPlayer;
	}

	public void setPlayer(Player player) {
		this.appPlayer = player;
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void init() throws Exception {

	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		BorderPane root = new BorderPane();
		Scene scene = new Scene(root, 500, 500);

		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Poker");



		this.primaryStage.setScene(scene);
		this.primaryStage.show();

		showClientServer();
	}

	public void showPoker(boolean bStartHub, String strComputerName, int iPort, String strPlayerName) {

		if (bStartHub) {
			try {
				pHub = new PokerHub(iPort);
			} catch (Exception e) {
				System.out.println("Error: Can't listen on port " + iPort);
			}
		}
		try {
			pClient = new PokerClient(strComputerName, iPort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		setPlayer(new Player(strPlayerName, pClient.getID()));

		initRootLayout();

		showPokerTable();
	}

	public void showClientServer() {
		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();

			loader = new FXMLLoader(getClass().getResource("view/ClientServerStart.fxml"));

			BorderPane ClientServerOverview = (BorderPane) loader.load();

			Scene scene = new Scene(ClientServerOverview);

			primaryStage.setScene(scene);

			// Controller access to the main app.
			ClientServerStartController controller = loader.getController();
			controller.setMainApp(this);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initRootLayout() {
		try {

			Screen screen = Screen.getPrimary();
			Rectangle2D bounds = screen.getVisualBounds();

			primaryStage.setX(bounds.getMinX());
			primaryStage.setY(bounds.getMinY());
			primaryStage.setWidth(bounds.getWidth());
			primaryStage.setHeight(bounds.getHeight());

			// Load root layout
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			//root layout.
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);

			//Controller access to the main app.
			rootController = loader.getController();

			rootController.setMainApp(this);

			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void showPokerTable() {
		try {
			// Load person overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/PokerTable.fxml"));
			BorderPane pokerOverview = (BorderPane) loader.load();

			rootLayout.setCenter(pokerOverview);

			// Controller access to the main app.
			pokerController = loader.getController();
			pokerController.setMainApp(this);

			getPlayer().setiPlayerPosition(0);
			Action act = new Action(eAction.TableState, getPlayer());
			messageSend(act);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void EndPoker() {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});
	}

	@Override
	public void stop() throws Exception {
		// connection.closeConnection();
	}

	public void messageSend(final Object message) {
		System.out.println("Message Sent");
		pClient.messageSend(message);
	}

	public String getRuleName() {
		return rootController.getRuleName();
	}

	private class PokerClient extends Client {

		public PokerClient(String hubHostName, int hubPort) throws IOException {
			super(hubHostName, hubPort);
		}

		/*
		 * messageSend - One single place to send messages
		 */
		protected void messageSend(Object message) {
			System.out.println("Message sent from MainApp.Client");
			resetOutput();
			super.send(message);
		}

		@Override
		protected void messageReceived(final Object message) {
			Platform.runLater(() -> {
				System.out.println(message);

				if (message instanceof String) {
					System.out.println(message);
				} else if (message instanceof Table) {
					pokerController.Handle_TableState((Table) message);
				}
				pokerController.MessageFromMainApp((String) message);
			});
		}

		@Override
		
		/*
		 * serverShutdown 
		 *  Call exit.
		 */
		protected void serverShutdown(String message) {

			Platform.runLater(() -> {
				Platform.exit();
				System.exit(0);
			});
		}

	}
}