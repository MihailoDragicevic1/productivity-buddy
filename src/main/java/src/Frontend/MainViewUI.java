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
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import src.Backend.main.Back;
import src.Backend.main.ProcessData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainViewUI extends Application {

    private Back back;

    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();

    private TableView<ProcessRow> tableView;
    private PieChart pieChart;

    private Label workTimeLabel;
    private Label funTimeLabel;
    private Label otherTimeLabel;

    private Timeline refreshTimeline;

    public MainViewUI() {
        this.back = new Back();
    }

    public MainViewUI(Back back) {
        this.back = back;
    }

    @Override
    public void start(Stage stage) {
        if (back == null) {
            back = new Back();
        }

        if (!back.isBackendReady()) {
            back.startBack();
        }

        stage.setTitle("Productivity Buddy - Main View");
        stage.setScene(createScene(stage));
        stage.show();

        refreshView();
        startAutoRefresh();
    }

    public Scene createScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        HBox topBar = createTopBar(stage);
        HBox mainContent = createMainContent(stage);

        root.setTop(topBar);
        root.setCenter(mainContent);

        refreshView();

        return new Scene(root, 1000, 600);
    }

    private HBox createTopBar(Stage stage) {
        Label title = new Label("Productivity Buddy");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

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
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }

            back.shutdown();
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, title, spacer, saveButton, loadButton, shutdownButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 15, 0));

        return topBar;
    }

    private HBox createMainContent(Stage stage) {
        VBox leftPanel = createProcessTableSection(stage);
        VBox rightPanel = createAnalyticsSection(stage);

        leftPanel.setPrefWidth(500);
        rightPanel.setPrefWidth(400);

        HBox mainContent = new HBox(20, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        return mainContent;
    }

    private VBox createProcessTableSection(Stage stage) {
        Label sectionTitle = new Label("Active Processes");
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

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    ProcessRow clickedProcess = row.getItem();

                    ProcessDetailViewUI detailView =
                            new ProcessDetailViewUI(
                                    back,
                                    clickedProcess.getProcessName(),
                                    clickedProcess.getCategory()
                            );

                    stage.setTitle("Productivity Buddy - Process Detail View");
                    stage.setScene(detailView.createScene(stage));
                }
            });

            return row;
        });

        VBox leftPanel = new VBox(10, sectionTitle, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return leftPanel;
    }

    private VBox createAnalyticsSection(Stage stage) {
        pieChart = new PieChart();
        pieChart.setTitle("Overview");
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);

        workTimeLabel = new Label("0h 0m 0s");
        funTimeLabel = new Label("0h 0m 0s");
        otherTimeLabel = new Label("0h 0m 0s");

        VBox workRow = createCategoryRow("Work", workTimeLabel, stage);
        VBox funRow = createCategoryRow("Fun", funTimeLabel, stage);
        VBox otherRow = createCategoryRow("Other", otherTimeLabel, stage);

        VBox rightPanel = new VBox(15, pieChart, workRow, funRow, otherRow);
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        return rightPanel;
    }

    private VBox createCategoryRow(String categoryName, Label timeLabel, Stage stage) {
        Label nameLbl = new Label(categoryName);
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        timeLabel.setStyle("-fx-font-size: 13px;");

        Button detailsBtn = new Button("Details");
        detailsBtn.setOnAction(e -> {
            SpecificCategoryViewUI categoryView = new SpecificCategoryViewUI(back,categoryName);
            stage.setTitle("Productivity Buddy - Specific Category View");
            stage.setScene(categoryView.createScene(stage));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, nameLbl, spacer, timeLabel, detailsBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle(
                "-fx-border-color: lightgray;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;"
        );

        return new VBox(row);
    }

    private void refreshView() {
        refreshProcesses();
        refreshAnalytics();
    }

    private void refreshProcesses() {
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

    private void refreshAnalytics() {
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList(
                new PieChart.Data("Work", back.getWorkTimeSeconds()),
                new PieChart.Data("Fun", back.getFunTimeSeconds()),
                new PieChart.Data("Other", back.getOtherTimeSeconds())
        );

        pieChart.setData(chartData);

        workTimeLabel.setText(back.getFormattedWorkTime());
        funTimeLabel.setText(back.getFormattedFunTime());
        otherTimeLabel.setText(back.getFormattedOtherTime());
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

    private String generateManualSnapshotFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        return "manual_snapshot_" + LocalDateTime.now().format(formatter) + ".csv";
    }

    public static void main(String[] args) {
        launch(args);
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
}