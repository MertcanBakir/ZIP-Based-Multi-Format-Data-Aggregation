# ZIP-Based Multi-Format Data Aggregation

A Java (Maven) project that **generates**, **zips**, **reads**, and **summarizes** synthetic sales data across **CSV**, **TXT**, and **JSON** formats.  
It auto-detects file types in a ZIP, validates data, and produces statistical summaries with malformed-record tracking.

---

## Technologies Used
- Java 17+
- Maven
- Gson
- SLF4J + Logback
- JUnit 5

---

## Features

- Generate **10,000-row** datasets with ~5% simulated corruption
- Auto-zip outputs into `input_dataset.zip`
- Stream-read ZIP contents without extraction
- Detect file format (**CSV / TXT / JSON**) automatically
- Compute per-column **min**, **max**, **avg**, **sum**
- Track malformed or invalid fields
- Output a summary report to a configurable location
- Includes **JUnit 5** tests and **SLF4J / Logback** logging

---

## How to Run

```bash
# 1. Build and run tests
mvn clean test

# 2. Run the main program
mvn clean compile exec:java -Dexec.mainClass="projects.Main"

# 3. Results
# - Generated data: Sales####.csv / Sales####.txt / Sales####.json
# - Zipped archive: input_dataset.zip
# - Summary output: src/main/resources/output/summary.txt
```
