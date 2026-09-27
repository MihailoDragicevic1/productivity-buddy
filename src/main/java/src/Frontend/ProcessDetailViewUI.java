package src.Frontend;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import src.Backend.main.Back;
import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ProcessDetailViewUI extends Application {

    private Stage primaryStage;

    private final String selectedProcessName;
    private final String selectedCategory;

    private Back back;

    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();

    private TableView<ProcessRow> tableView;

    private Label titleLabel;
    private Label totalTimeLabel;
    private Label categoryLabel;
    private Label ramUsageLabel;
    private Label ramRankLabel;
    private Label cpuUsageLabel;
    private Label cpuRankLabel;

    private Button freezeButton;

    private Timeline refreshTimeline;


    public ProcessDetailViewUI(String selectedProcessName, String selectedCategory) {
        this.selectedProcessName = selectedProcessName;
        this.selectedCategory = selectedCategory;
        this.back = new Back();
    }

    public ProcessDetailViewUI(Back back, String selectedProcessName, String selectedCategory) {
        this.selectedProcessName = selectedProcessName;
        this.selectedCategory = selectedCategory;
        this.back = back;
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        if (back == null) {
            back = new Back();
        }

        if (!back.isBackendReady()) {
            back.startBack();
        }

        stage.setTitle("Productivity Buddy - Process Detail View");
        stage.setScene(createScene());
        stage.show();

        refreshView();
        startAutoRefresh();
    }

    public Scene createScene(Stage stage) {
        this.primaryStage = stage;

        if (back == null) {
            back = new Back();
        }

        if (!back.isBackendReady()) {
            back.startBack();
        }

        Scene scene = createScene();
        refreshView();
        startAutoRefresh();
        return scene;
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        HBox topBar = createTopBar();
        HBox mainContent = createMainContent();

        root.setTop(topBar);
        root.setCenter(mainContent);

        return new Scene(root, 1000, 600);
    }

    private HBox createTopBar() {
        Button backButton = new Button("← Back");
        backButton.setOnAction(e -> {
            stopAutoRefresh();
            MainViewUI mainView = new MainViewUI(back);
            primaryStage.setTitle("Productivity Buddy - Main View");
            primaryStage.setScene(mainView.createScene(primaryStage));
        });

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String fileName = generateManualSnapshotFileName();
            back.save(fileName);
        });

        Button loadButton = new Button("Load");
        loadButton.setOnAction(e -> {
            back.load();
            refreshView();
        });

        Button shutdownButton = new Button("Shutdown");
        shutdownButton.setOnAction(e -> {
            stopAutoRefresh();
            back.shutdown();
            primaryStage.close();
        });

        HBox topBar = new HBox(10, backButton, spacerLeft, saveButton, loadButton, shutdownButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 15, 0));

        return topBar;
    }

    private HBox createMainContent() {
        VBox leftPanel = createProcessTableSection();
        VBox rightPanel = createDetailsSection();

        leftPanel.setPrefWidth(520);
        rightPanel.setPrefWidth(420);

        HBox mainContent = new HBox(20, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        return mainContent;
    }

    private VBox createProcessTableSection() {
        Label sectionTitle = new Label("Processes");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No processes available"));

        TableColumn<ProcessRow, String> processColumn = new TableColumn<>("Process");
        processColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProcessName()));

        TableColumn<ProcessRow, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        tableView.getColumns().addAll(processColumn, categoryColumn);
        tableView.setItems(processRows);

        tableView.setRowFactory(tv -> {
            TableRow<ProcessRow> row = new TableRow<>();

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else if (newItem.getProcessName().equals(selectedProcessName)) {
                    row.setStyle("-fx-background-color: #d9f7be;");
                } else {
                    row.setStyle("");
                }
            });

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    stopAutoRefresh();

                    ProcessRow clickedProcess = row.getItem();
                    ProcessDetailViewUI detailView =
                            new ProcessDetailViewUI(back, clickedProcess.getProcessName(), clickedProcess.getCategory());

                    primaryStage.setTitle("Productivity Buddy - Process Detail View");
                    primaryStage.setScene(detailView.createScene(primaryStage));
                }
            });

            return row;
        });

        VBox leftPanel = new VBox(10, sectionTitle, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return leftPanel;
    }

    private VBox createDetailsSection() {
        titleLabel = new Label(selectedProcessName);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        totalTimeLabel = new Label("Total time - 0h 0m 0s");
        totalTimeLabel.setStyle("-fx-font-size: 16px;");

        categoryLabel = new Label("Category: " + selectedCategory);
        categoryLabel.setStyle("-fx-font-size: 14px;");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(12);
        statsGrid.setPadding(new Insets(15, 0, 15, 0));

        ramUsageLabel = new Label("RAM usage: 0");
        ramRankLabel = new Label("RAM rank: N/A");
        cpuUsageLabel = new Label("CPU usage: 0.0");
        cpuRankLabel = new Label("CPU rank: N/A");

        ramUsageLabel.setStyle("-fx-font-size: 14px;");
        ramRankLabel.setStyle("-fx-font-size: 14px;");
        cpuUsageLabel.setStyle("-fx-font-size: 14px;");
        cpuRankLabel.setStyle("-fx-font-size: 14px;");

        statsGrid.add(ramUsageLabel, 0, 0);
        statsGrid.add(ramRankLabel, 1, 0);
        statsGrid.add(cpuUsageLabel, 0, 1);
        statsGrid.add(cpuRankLabel, 1, 1);

        Button killButton = new Button("Kill Process");
        killButton.setPrefWidth(140);
        killButton.setPrefHeight(50);
        killButton.setOnAction(e -> handleKillProcess());

        Button changeNameButton = new Button("Change Name");
        changeNameButton.setPrefWidth(140);
        changeNameButton.setPrefHeight(50);
        changeNameButton.setOnAction(e -> handleChangeName());

        freezeButton = new Button("Freeze Tracking");
        freezeButton.setPrefWidth(140);
        freezeButton.setPrefHeight(50);
        freezeButton.setOnAction(e -> handleFreezeTracking());

        Button changeCategoryButton = new Button("Change Category");
        changeCategoryButton.setPrefWidth(140);
        changeCategoryButton.setPrefHeight(50);
        changeCategoryButton.setOnAction(e -> handleChangeCategory());

        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(20);
        actionGrid.setVgap(20);
        actionGrid.add(killButton, 0, 0);
        actionGrid.add(changeNameButton, 1, 0);
        actionGrid.add(freezeButton, 0, 1);
        actionGrid.add(changeCategoryButton, 1, 1);

        VBox rightPanel = new VBox(10, titleLabel, totalTimeLabel, categoryLabel, statsGrid, actionGrid);
        rightPanel.setAlignment(Pos.TOP_LEFT);
        rightPanel.setPadding(new Insets(10));

        return rightPanel;
    }

    private void refreshView() {
        refreshProcessTable();
        refreshDetails();
    }

    private void refreshProcessTable() {
        List<ProcessData> allProcesses = back.getAllProcesses();
        processRows.clear();

        for (ProcessData process : allProcesses) {
            if (process == null) {
                continue;
            }

            processRows.add(new ProcessRow(
                    process.getDisplayName(),
                    normalizeCategory(process.getCategory())
            ));
        }
    }

    private void refreshDetails() {
        ProcessData selectedProcess = getSelectedProcessData();

        if (selectedProcess == null) {
            titleLabel.setText(selectedProcessName);
            totalTimeLabel.setText("Total time - 0h 0m 0s");
            categoryLabel.setText("Category: " + selectedCategory);
            ramUsageLabel.setText("RAM usage: N/A");
            ramRankLabel.setText("RAM rank: N/A");
            cpuUsageLabel.setText("CPU usage: N/A");
            cpuRankLabel.setText("CPU rank: N/A");
            freezeButton.setText("Freeze Tracking");
            return;
        }

        titleLabel.setText(selectedProcess.getDisplayName());
        totalTimeLabel.setText("Total time - " + back.formatSeconds(selectedProcess.getTotalTimeSeconds()));
        categoryLabel.setText("Category: " + normalizeCategory(selectedProcess.getCategory()));
        ramUsageLabel.setText("RAM usage: " + selectedProcess.getRamUsage());
        ramRankLabel.setText(buildRamRankText(selectedProcess));
        cpuUsageLabel.setText("CPU usage: " + selectedProcess.getCpuUsage());
        cpuRankLabel.setText(buildCpuRankText(selectedProcess));

        if (selectedProcess.isTrackingFrozen()) {
            freezeButton.setText("Unfreeze Tracking");
        } else {
            freezeButton.setText("Freeze Tracking");
        }
    }

    private ProcessData getSelectedProcessData() {
        ProcessKey key = back.findProcessKeyByName(selectedProcessName);
        if (key == null) {
            return null;
        }
        return back.getProcess(key);
    }

    private String buildRamRankText(ProcessData selectedProcess) {
        List<ProcessData> allProcesses = back.getAllProcesses();
        allProcesses.sort(Comparator.comparingLong(ProcessData::getRamUsage).reversed());

        for (int i = 0; i < allProcesses.size(); i++) {
            ProcessData current = allProcesses.get(i);
            if (current == null) {
                continue;
            }

            if (sameDisplayedProcess(current, selectedProcess)) {
                return (i + 1) + getOrdinalSuffix(i + 1) + " on RAM usage";
            }
        }

        return "RAM rank: N/A";
    }

    private String buildCpuRankText(ProcessData selectedProcess) {
        List<ProcessData> allProcesses = back.getAllProcesses();
        allProcesses.sort(Comparator.comparingDouble(ProcessData::getCpuUsage).reversed());

        for (int i = 0; i < allProcesses.size(); i++) {
            ProcessData current = allProcesses.get(i);
            if (current == null) {
                continue;
            }

            if (sameDisplayedProcess(current, selectedProcess)) {
                return (i + 1) + getOrdinalSuffix(i + 1) + " on CPU usage";
            }
        }

        return "CPU rank: N/A";
    }

    private boolean sameDisplayedProcess(ProcessData first, ProcessData second) {
        if (first == null || second == null) {
            return false;
        }

        String firstName = first.getDisplayName();
        String secondName = second.getDisplayName();

        if (firstName == null || secondName == null) {
            return false;
        }

        return firstName.equalsIgnoreCase(secondName);
    }

    private String getOrdinalSuffix(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return "th";
        }

        return switch (number % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private void handleKillProcess() {
        ProcessKey key = back.findProcessKeyByName(selectedProcessName);
        if (key == null) {
            showInfo("Process not found.");
            return;
        }

        back.killProcess(key);
        refreshView();
    }

    private void handleChangeName() {
        ProcessKey key = back.findProcessKeyByName(selectedProcessName);
        if (key == null) {
            showInfo("Process not found.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Name");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new alias:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            back.changeName(key, newName);
            refreshView();
        });
    }

    private void handleFreezeTracking() {
        ProcessKey key = back.findProcessKeyByName(selectedProcessName);
        if (key == null) {
            showInfo("Process not found.");
            return;
        }

        back.freezeTracking(key);
        refreshView();
    }

    private void handleChangeCategory() {
        ProcessKey key = back.findProcessKeyByName(selectedProcessName);
        if (key == null) {
            showInfo("Process not found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("Work", Arrays.asList("Work", "Fun", "Other", "uncategorized"));
        dialog.setTitle("Change Category");
        dialog.setHeaderText(null);
        dialog.setContentText("Select category:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(category -> {
            back.changeCategory(key, category);
            refreshView();
        });
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "uncategorized";
        }
        return category;
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> refreshView())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    private String generateManualSnapshotFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        return "manual_snapshot_" + LocalDateTime.now().format(formatter) + ".csv";
    }

    public static class ProcessRow {
        private final String processName;
        private final String category;

        public ProcessRow(String processName, String category) {
            this.processName = processName;
            this.category = category;
        }

        public String getProcessName() {
            return processName;
        }

        public String getCategory() {
            return category;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}