package fr.utc.sr06.CryptokiExplorer;/**
 * Created by raphael on 23/12/15.
 */

import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class Window extends Application {

    private VBox root;
    private final int SIZE = 60;

    private ModuleManager manager;

    private StringProperty pathProperty;
    private MessageIndicator messageIndicator;
    private Pane functionZone;
    private ListView<Slot> slotsSidebar;
    private ObservableList<Slot> slots;
    private ListView<UIFunction> functionsSidebar;
    private ObservableList<UIFunction> functions;

    private ResourceBundle translations;

    @Override
    public void start(Stage stage) {

        loadTranslations();
        initUI(stage);
        manager = new ModuleManager();
    }

    @Override
    /* Appelé quand l'application se termine (appel de Platform.exit() ou fermeture de la dernière fenêtre */
    public void stop() {
        unloadCurrentModule();
    }

    public static void main(String[] args) {
        Window win = new Window();
        win.launch(args);
    }

    private void loadTranslations() {
        translations = ResourceBundle.getBundle("translations/main", Locale.getDefault());
    }

    private String t_(String key) {
        return translations.getString(key);
    }

    private void initUI(Stage stage) {
        root = new VBox();
        BorderPane subroot = new BorderPane();

        Label label1 = new Label(t_("modulePathLabel"));

        pathProperty = new SimpleStringProperty();
        TextField moduleField = new TextField();
        moduleField.promptTextProperty().set(t_("modulePathPlaceholder"));
        moduleField.textProperty().bindBidirectional(pathProperty);
        moduleField.setOnAction((event) -> loadModule(moduleField.getText()));
        moduleField.prefWidthProperty().bind(root.widthProperty().divide(2));

        Button loadModuleButton = new Button();
        loadModuleButton.setText(t_("chooseModuleButton"));
        loadModuleButton.setOnAction((event) -> chooseAndLoadModule());

        HBox hb = new HBox();
        hb.getStyleClass().add("module-path-bar");
        hb.prefHeightProperty().bind(root.heightProperty().divide(12));
        hb.getChildren().addAll(label1, moduleField, loadModuleButton);
        hb.setAlignment(Pos.CENTER);

        messageIndicator = new MessageIndicator();
        subroot.setLeft(getLeftLists());
        subroot.setCenter(getFunctionZone());

        MenuBar menuBar = createMenus();
        root.getChildren().addAll(menuBar, hb, messageIndicator, subroot);


        Scene scene = new Scene(root, 1000 , 800);
        scene.getStylesheets().add("css/stylesheet.css");

        stage.getIcons().add(new Image("css/key-xxl.png"));

        stage.setTitle(t_("mainWindowTitle"));
        stage.setScene(scene);
        stage.show();

        loadModuleButton.requestFocus();
    }

    private MenuBar createMenus() {
        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu(t_("fileMenu"));

        MenuItem module = new MenuItem(t_("chooseModuleButton"));
        module.setOnAction((event) -> chooseAndLoadModule());

        MenuItem quit = new MenuItem(t_("quitMenuItem"));
        quit.setOnAction((event) -> Platform.exit());

        menuFile.getItems().addAll(module, quit);

        Menu menuEdit = new Menu("Edit");

         MenuItem CreateToken = new MenuItem("CreateToken");
        CreateToken.setOnAction((event) -> CreateToken());

        MenuItem InitToken = new MenuItem("InitToken");
        InitToken.setOnAction((event) -> InitTokenM());


        MenuItem editPIN = new MenuItem("EDIT PIN");
        editPIN.setOnAction((event) -> EditPIN());



        MenuItem editSOPIN = new MenuItem("EDIT SOPIN");
        editSOPIN.setOnAction((event) -> EditPIN());


        menuEdit.getItems().addAll(CreateToken,InitToken,editPIN,editSOPIN);


        Menu menuView = new Menu("View");

        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

        return menuBar;
    }

    private Pane getFunctionZone() {
        functionZone = new HBox();
        functionZone.setPrefWidth(SIZE);
        functionZone.prefHeightProperty().bind(root.heightProperty().subtract(100));

        return functionZone;
    }

    private Node getLeftLists() {
        slotsSidebar = new ListView<>();
        slots = FXCollections.observableArrayList();
        slotsSidebar.setItems(slots);
        slotsSidebar.setPlaceholder(new Label(t_("placeholderSlotsSidebar")));
        slotsSidebar.setCellFactory((view) ->
                new SlotListCell()
        );
        slotsSidebar.getSelectionModel().selectedItemProperty().addListener((ob, oldValue, newValue) -> {
            if  (newValue == null) {
                return;
            }

            UIFunction function = getCurrentFunction();
            if  (function != null) {
                function.unload();
                function.load(newValue);
                functionZone.getChildren().setAll(function.getUI());
            }
        });

        functionsSidebar = new ListView<>();
        functionsSidebar.prefWidthProperty().bind(root.widthProperty().divide(5));
        functionsSidebar.prefHeightProperty().bind(root.heightProperty().divide(3));
        functionsSidebar.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            newValue.load(getCurrentSlot());
            functionZone.getChildren().setAll(newValue.getUI());

            if (oldValue != null) {
                oldValue.unload();
            }
        });
        functions = FXCollections.observableArrayList();
        functionsSidebar.setItems(functions);
        functionsSidebar.setPlaceholder(new Label(t_("placeholderFunctionsSidebar")));

        VBox box = new VBox();
        box.getChildren().addAll(slotsSidebar, functionsSidebar);
        return box;
    }

    private Slot getCurrentSlot() {
        return slotsSidebar.getSelectionModel().getSelectedItem();
    }

    private UIFunction getCurrentFunction() {
        return functionsSidebar.getSelectionModel().getSelectedItem();
    }

    private class SlotListCell extends ListCell<Slot> {
        @Override
        protected void updateItem(Slot item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
            } else {
                try {
                    Token token = item.getToken();

                    String description = item.getSlotInfo().getSlotDescription().trim();
                    if (token != null) {
                        setText(String.format(t_("tokenPresentCell"), description));
                    } else {
                        setText(String.format(t_("noTokenCell"), description));
                    }
                } catch (TokenException e) {
                    e.printStackTrace();
                    showDialog(e.toString());
                    setText(null);
                }
            }
        }
    }


    private void loadModule(String path) {
        try {
            unloadCurrentModule();

            manager.setPathModule(path);
            pathProperty.set(path);

            slots.setAll(manager.getAllSlots());
            functions.setAll(new InfoFunction(t_("infoFunction"), manager),
                    new MechanismsFunction(t_("mechanismsFunction"), manager),
                    new ObjectsToken (t_("ObjectsToken"), manager),
                    new RSAFunction(t_("rsaFunction"), manager));

            Platform.runLater(() -> { // Select first slot and info function
                if (!slots.isEmpty()) {
                    slotsSidebar.getSelectionModel().select(0);
                    functionsSidebar.getSelectionModel().select(0);
                }
            });
        } catch (IOException e) {
            messageIndicator.error(t_("moduleOpenIOError"), e.getLocalizedMessage());
            e.printStackTrace();
            showDialog(e.toString());

        } catch (TokenException e) {
            messageIndicator.error(t_("moduleOpenInitError"), e.getLocalizedMessage());
            e.printStackTrace();
            showDialog(e.toString());

        }
    }

    private void unloadCurrentModule() {
        try {
            manager.end();
        } catch (TokenException e) {
            e.printStackTrace(); // TODO propager aux appelants ?
            showDialog(e.toString());

        }
    }

    private void chooseAndLoadModule() {
        askModule().ifPresent((file) -> loadModule(file.getAbsolutePath()));
    }

    private Optional<File> askModule() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Module Library");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Module Files", "*.so", "*.dll", "*.dylib"));
        return Optional.ofNullable(fileChooser.showOpenDialog(root.getScene().getWindow()));
    }

    private void CreateToken() {

        VBox InitTokenWin= new VBox();

        Stage stage1 = new Stage();
        stage1.setTitle("Initialize Token");
        stage1.setScene(new Scene(InitTokenWin, 300, 200));
        stage1.getScene().getStylesheets().add("css/stylesheet.css");
        TextField AskLabel = new TextField();
        Label labelLab = new Label("Label:");

        HBox hbLab = new HBox();
        hbLab.getChildren().addAll(labelLab, AskLabel);
        hbLab.setAlignment(Pos.CENTER);
        TextField AskSOPIN = new TextField();
        Label labelSOPIN = new Label("SOPIN:");

        HBox hbSOPIN = new HBox();
        hbSOPIN.getChildren().addAll(labelSOPIN, AskSOPIN);
        hbSOPIN.setAlignment(Pos.CENTER);

        TextField AskPIN = new TextField();
        Label labelPIN = new Label("PIN:");

        HBox hbPIN = new HBox();
        hbPIN.getChildren().addAll(labelPIN, AskPIN);
        hbPIN.setAlignment(Pos.CENTER);

        Button Cancel = new Button();
        Cancel.setText("Cancel");
        Cancel.setOnAction((event) -> stage1.hide());


        Button Initialize = new Button();
        Initialize.setText("Create");
        Initialize.setOnAction((event) ->{

            String LABELl = AskLabel.getText();
            String PINl = AskPIN.getText();
            String SOPINl = AskSOPIN.getText();
            try {
                manager.createToken(LABELl,SOPINl,PINl);
            } catch (TokenException e) {
                e.printStackTrace();
                showDialog(e.toString());

            } catch (IOException e) {
                e.printStackTrace();
                showDialog(e.toString());

            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
                showDialog(e.toString());

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                showDialog(e.toString());

            }
              stage1.hide();

        });

        HBox hbBUT = new HBox();
        hbBUT.getChildren().addAll(Cancel, Initialize);
        hbBUT.setAlignment(Pos.CENTER);


        InitTokenWin.getChildren().addAll(hbLab,hbSOPIN,hbPIN,hbBUT);
        stage1.show();


    }
    private void InitTokenM() {
        Slot item = getCurrentSlot();
        Label labelToken = null;
        Label PINinit = null;

        try {
            labelToken = new Label("Token Label: " + item.getToken().getTokenInfo().getLabel());
            PINinit = new Label("Token initialized: " + item.getToken().getTokenInfo().isTokenInitialized());
        } catch (TokenException e) {
            e.printStackTrace();
            showDialog(e.toString());
        }
        VBox EditPinWind= new VBox();
        Stage stage1 = new Stage();
        stage1.setTitle("Change PIN");
        stage1.setScene(new Scene(EditPinWind, 300, 200));
        stage1.getScene().getStylesheets().add("css/stylesheet.css");

        TextField AskLabel = new TextField();
        Label labelLab = new Label("Label:");

        HBox hbLab = new HBox();
        hbLab.getChildren().addAll(labelLab, AskLabel);
        hbLab.setAlignment(Pos.CENTER);

        TextField AskPIN = new TextField();
        Label labelPIN = new Label("PIN:");

        HBox hbPIN = new HBox();
        hbPIN.getChildren().addAll(labelPIN, AskPIN);
        hbPIN.setAlignment(Pos.CENTER);

        Button Cancel = new Button();
        Cancel.setText("Cancel");
        Cancel.setOnAction((event) -> stage1.hide());

        Button Change = new Button();
        Change.setText("Change");
        Change.setOnAction((event) -> {

            try {
                item.getToken().initToken(AskPIN.getText().toCharArray(),AskLabel.getText());
            } catch (TokenException e) {
                e.printStackTrace();
                showDialog(e.toString());
            }

            stage1.hide();}


        );

        HBox hbBUT = new HBox();
        hbBUT.getChildren().addAll(Cancel, Change);
        hbBUT.setAlignment(Pos.CENTER);


        EditPinWind.getChildren().addAll(labelToken,PINinit,hbLab,hbPIN,hbBUT);
        stage1.show();
    }
    private void EditPIN() {
        Slot item = getCurrentSlot();
        Label labelToken = null;
        Label PINinit = null;
        try {
            labelToken = new Label("Min PIN Length: " + item.getToken().getTokenInfo().getMinPinLen());
            PINinit = new Label("Max PIN Length: " + item.getToken().getTokenInfo().getMaxPinLen());
        } catch (TokenException e) {
            e.printStackTrace();
            showDialog(e.toString());
        }
        VBox EditPinWind= new VBox();
        Stage stage1 = new Stage();
        stage1.setTitle("Change PIN");
        stage1.setScene(new Scene(EditPinWind, 300, 200));
        stage1.getScene().getStylesheets().add("css/stylesheet.css");
        TextField AskPIN = new TextField();
        Label labelLab = new Label("PIN:");

        HBox hbPIN = new HBox();
        hbPIN.getChildren().addAll(labelLab, AskPIN);
        hbPIN.setAlignment(Pos.CENTER);


        TextField AskOldPIN = new TextField();
        Label labelOldPIN = new Label("OldPIN:");

        HBox hbOldPIN = new HBox();
        hbOldPIN.getChildren().addAll(labelOldPIN, AskOldPIN);
        hbOldPIN.setAlignment(Pos.CENTER);

        Button Cancel = new Button();
        Cancel.setText("Cancel");
        Cancel.setOnAction((event) -> stage1.hide());

        Button Change = new Button();
        Change.setText("Change");
        Change.setOnAction((event) -> {

            try {
                manager.changeUserPin(item.getToken(),AskOldPIN.getText(),AskPIN.getText());
            } catch (TokenException e) {
                showDialog(e.toString());
                e.printStackTrace();
            }

            stage1.hide();}


        );

        HBox hbBUT = new HBox();
        hbBUT.getChildren().addAll(Cancel, Change);
        hbBUT.setAlignment(Pos.CENTER);


        EditPinWind.getChildren().addAll(labelToken,PINinit,hbOldPIN,hbPIN,hbBUT);
        stage1.show();
    }


    private void showDialog (String Dialog) {

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);


        Button Ok = new Button("Ok");
        Ok.setOnAction((event) -> dialogStage.hide());

        dialogStage.setScene(new Scene(VBoxBuilder.create().
        children(new Text("Exception"+ Dialog), Ok).
                alignment(Pos.CENTER).padding(new Insets(5)).build()));
        dialogStage.show();

    }


    }

