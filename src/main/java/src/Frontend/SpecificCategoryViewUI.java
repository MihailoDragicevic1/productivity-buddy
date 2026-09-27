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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpecificCategoryViewUI extends Application {

    private Stage primaryStage;
    private final String categoryName;
    private Back back;

    private final ObservableList<CategoryProcessRow> categoryRows = FXCollections.observableArrayList();

    private TableView<CategoryProcessRow> tableView;
    private PieChart pieChart;
    private Label totalTimeLabel;

    private Timeline refreshTimeline;

    private List<ChartSnapshotItem> lastChartSnapshot = new ArrayList<>();
    private static final long CHART_UPDATE_THRESHOLD_SECONDS = 5;


    public SpecificCategoryViewUI(Back back, String categoryName) {
        this.categoryName = categoryName;
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

        stage.setTitle("Productivity Buddy - Specific Category View");
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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

        HBox topBar = new HBox(10, backButton, spacer, saveButton, loadButton, shutdownButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 15, 0));

        return topBar;
    }

    private HBox createMainContent() {
        VBox leftPanel = createCategoryTableSection();
        VBox rightPanel = createChartSection();

        leftPanel.setPrefWidth(540);
        rightPanel.setPrefWidth(380);

        HBox mainContent = new HBox(20, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        return mainContent;
    }

    private VBox createCategoryTableSection() {
        Label titleLabel = new Label(categoryName + " Category");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No processes available"));

        TableColumn<CategoryProcessRow, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProcessName()));

        TableColumn<CategoryProcessRow, String> usageColumn = new TableColumn<>("RAM & CPU");
        usageColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsage()));

        TableColumn<CategoryProcessRow, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTime()));

        tableView.getColumns().addAll(nameColumn, usageColumn, timeColumn);
        tableView.setItems(categoryRows);

        VBox leftPanel = new VBox(10, titleLabel, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return leftPanel;
    }

    private VBox createChartSection() {
        Label chartTitle = new Label("Top 10 processes by time spent");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        pieChart = new PieChart();
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(false);
        pieChart.setAnimated(false);

        totalTimeLabel = new Label(categoryName.toLowerCase() + " total time - 0h 0m 0s");
        totalTimeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        VBox rightPanel = new VBox(15, chartTitle, pieChart, totalTimeLabel);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        return rightPanel;
    }

    private void refreshView() {
        refreshCategoryTable();
        refreshChart();
    }

    private void refreshCategoryTable() {
        List<ProcessData> processes = back.getProcessesByCategory(categoryName);

        categoryRows.clear();

        for (ProcessData process : processes) {
            if (process == null) {
                continue;
            }

            categoryRows.add(new CategoryProcessRow(
                    process.getDisplayName(),
                    formatRam(process.getRamUsage()) + " | " + process.getCpuUsage() + "%",
                    back.formatSeconds(process.getTotalTimeSeconds())
            ));
        }
    }

    private String formatRam(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);

        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }

        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    private void refreshChart() {
        List<ProcessData> processes = back.getProcessesByCategory(categoryName);
        int limit = Math.min(processes.size(), 10);

        List<ChartSnapshotItem> currentSnapshot = new ArrayList<>();
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (int i = 0; i < limit; i++) {
            ProcessData process = processes.get(i);

            if (process == null) {
                continue;
            }

            currentSnapshot.add(new ChartSnapshotItem(
                    process.getDisplayName(),
                    process.getTotalTimeSeconds()
            ));

            chartData.add(new PieChart.Data(
                    process.getDisplayName(),
                    process.getTotalTimeSeconds()
            ));
        }

        if (shouldUpdateChart(currentSnapshot)) {
            pieChart.setData(chartData);
            lastChartSnapshot = currentSnapshot;
        }

        totalTimeLabel.setText(categoryName.toLowerCase() + " total time - " + getFormattedCategoryTime());
    }

    private boolean shouldUpdateChart(List<ChartSnapshotItem> currentSnapshot) {
        if (lastChartSnapshot.size() != currentSnapshot.size()) {
            return true;
        }

        for (int i = 0; i < currentSnapshot.size(); i++) {
            ChartSnapshotItem oldItem = lastChartSnapshot.get(i);
            ChartSnapshotItem newItem = currentSnapshot.get(i);

            if (!Objects.equals(oldItem.name, newItem.name)) {
                return true;
            }

            long delta = Math.abs(oldItem.totalTimeSeconds - newItem.totalTimeSeconds);
            if (delta >= CHART_UPDATE_THRESHOLD_SECONDS) {
                return true;
            }
        }

        return false;
    }

    private String getFormattedCategoryTime() {
        if (categoryName.equalsIgnoreCase("Work")) {
            return back.getFormattedWorkTime();
        }

        if (categoryName.equalsIgnoreCase("Fun")) {
            return back.getFormattedFunTime();
        }

        if (categoryName.equalsIgnoreCase("Other")) {
            return back.getFormattedOtherTime();
        }

        return back.getFormattedUncategorizedTime();
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

    private static class ChartSnapshotItem {
        private final String name;
        private final long totalTimeSeconds;

        public ChartSnapshotItem(String name, long totalTimeSeconds) {
            this.name = name;
            this.totalTimeSeconds = totalTimeSeconds;
        }
    }

    public static class CategoryProcessRow {
        private final String processName;
        private final String usage;
        private final String time;

        public CategoryProcessRow(String processName, String usage, String time) {
            this.processName = processName;
            this.usage = usage;
            this.time = time;
        }

        public String getProcessName() {
            return processName;
        }

        public String getUsage() {
            return usage;
        }

        public String getTime() {
            return time;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}