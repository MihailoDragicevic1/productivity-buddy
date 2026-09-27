# Productivity Buddy

A concurrent desktop application built with **Java and JavaFX** for real-time system monitoring, process analysis, and productivity tracking.

Productivity Buddy monitors active operating-system processes, collects resource-usage information, classifies processes based on user-defined rules, and presents the collected data through a responsive graphical interface.

The project focuses on applying **concurrent programming concepts** to a real-world monitoring system while keeping background processing independent from the JavaFX user interface.

---

## Overview

System monitoring involves continuously collecting and processing data while keeping the user interface responsive.

Productivity Buddy separates these responsibilities across multiple concurrent components. Process scanning, data processing, analytics, snapshot management, and UI visualization can operate independently without blocking the main JavaFX application thread.

The application provides:

- Real-time process monitoring
- System resource analysis
- Process classification and tagging
- Productivity tracking
- Historical snapshot generation
- Multiple data visualization views
- Configurable monitoring behavior
- Concurrent background processing

---

## Features

### Real-Time Process Monitoring

The application periodically scans running system processes and collects information used for monitoring and analysis.

Process information is obtained using **OSHI**, allowing the application to interact with operating-system metrics from Java.

### Process Classification

Processes can be categorized using configurable rules.

This allows monitored applications to be associated with different productivity categories and enables the system to analyze how time is distributed across running processes.

### Productivity Analysis

Collected process information is processed to provide insight into user activity and resource usage.

The application maintains monitoring information independently from the UI so that analytics can continue while different views are displayed.

### Snapshot System

Productivity Buddy supports generating snapshots of collected monitoring data.

Snapshots provide a historical representation of system activity and can be generated periodically according to application configuration.

### Responsive JavaFX Interface

The graphical interface is implemented with **JavaFX** and provides multiple views for displaying monitoring and productivity information.

Background operations are kept separate from the JavaFX application thread to prevent intensive monitoring tasks from freezing the interface.

---

## Concurrency Model

Concurrency is a central part of the project.

The application uses several Java concurrency mechanisms, including:

- `Thread`
- `ExecutorService`
- `ForkJoinPool`
- `ConcurrentHashMap`

Different responsibilities are executed independently so that process scanning, data processing, analytics, and visualization do not unnecessarily block one another.

A simplified architecture looks like this:

```text
                     Operating System
                            |
                            v
                    Process Scanner
                            |
                            v
                  Process Information
                            |
             +--------------+--------------+
             |                             |
             v                             v
      Classification                 Resource Analysis
             |                             |
             +--------------+--------------+
                            |
                            v
                     Shared Data
                            |
               +------------+------------+
               |                         |
               v                         v
        Snapshot System             JavaFX Views
                                         |
                                         v
                                  User Interface
```

The UI remains responsible for presentation, while monitoring and processing operations execute in background components.

---

## Architecture

The project is organized around a separation between backend monitoring logic and frontend visualization.

```text
src/main/java/
└── src/
    ├── Backend/
    │   └── ...
    │
    └── Frontend/
        └── ...
```

### Backend

The backend is responsible for tasks such as:

- Scanning active processes
- Collecting system information
- Processing monitoring data
- Managing shared state
- Process classification
- Productivity analytics
- Snapshot generation

### Frontend

The frontend is implemented using JavaFX and is responsible for:

- Displaying monitored processes
- Presenting collected statistics
- Showing productivity information
- Providing interaction with monitoring functionality

This separation allows background monitoring operations to continue independently from interface rendering.

---

## Configuration

Application behavior is controlled through `config.properties`.

Example:

```properties
monitor.interval=3000
mapping.file=data/process_info.json
file=novi.fajl.txt
snapshot.interval=60
```

The configuration controls values such as the process monitoring interval, mapping data location, classification input, and snapshot behavior.

---

## Tech Stack

- **Java 21**
- **JavaFX 21**
- **Maven**
- **OSHI**
- **Java Concurrency API**
- **Fork/Join Framework**

---

## Getting Started

### Prerequisites

Make sure the following are installed:

- Java Development Kit 21
- Apache Maven

Verify the installation:

```bash
java -version
mvn -version
```

### Clone the Repository

```bash
git clone https://github.com/MihailoDragicevic1/productivity-buddy.git
cd productivity-buddy
```

### Build the Project

```bash
mvn clean test
```

### Run the Application

```bash
mvn javafx:run
```

The JavaFX application should open and begin monitoring system processes according to the configured monitoring interval.

---

## Generated Data

During execution, the application may generate runtime data and CSV snapshots.

These files are intentionally excluded from version control because they represent local monitoring sessions rather than source code required to build the project.

The repository `.gitignore` excludes:

```text
target/
.idea/
.vscode/
data/
snapshot_*.csv
manual_snapshot_*.csv
```

---

## Build Verification

The project can be compiled and verified using:

```bash
mvn clean test
```

The Maven build compiles the Java sources against Java 21 and resolves the required JavaFX and OSHI dependencies.

---

## Current Scope

Productivity Buddy was developed as a concurrent desktop monitoring application with an emphasis on:

- Concurrent programming
- Separation of responsibilities
- Responsive user interfaces
- Real-time process monitoring
- System-resource analysis
- Productivity classification

The project is intended as an exploration of concurrent application architecture and desktop system monitoring rather than a production-grade operating-system monitoring platform.

---

## Future Improvements

Possible extensions include:

- More advanced productivity analytics
- Configurable categories directly through the UI
- Persistent historical analytics
- Interactive charts and reporting
- Improved process-search and filtering
- Exportable productivity reports
- Cross-platform behavior validation
- Additional system-resource metrics
- Automated tests for monitoring and analytics components

---

## Author

**Mihailo Dragićević**

Computer Science student at the Faculty of Computing (RAF), Union University in Belgrade.

[GitHub](https://github.com/MihailoDragicevic1) · [LinkedIn](https://www.linkedin.com/in/mihailo-dragicevic-3aaa81308/)