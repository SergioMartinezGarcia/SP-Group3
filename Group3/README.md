# Group 3 - Software Platforms Course Project

This project is a data ingestion and analysis pipeline built with Spring Boot, MongoDB, and Elasticsearch. It fetches news articles from The Guardian, indexes them for search, and performs topic modeling and representation extraction.

## 📋 Prerequisites

Before you begin, ensure you have the following installed on your system:
- **Docker**: [Install Docker](https://docs.docker.com/get-docker/)
- **Docker Compose**: Usually included with Docker Desktop.

## 🚀 Getting Started

To get the entire system up and running, follow these steps:

### 1. Clone the Repository
```bash
git clone <repository-url>
cd SP-Group3/Group3
```

### 2. Configuration (Optional)
The application uses The Guardian API to fetch articles. A default API key is provided in `src/main/resources/application.properties`, but you can use your own:
1. Open `src/main/resources/application.properties`.
2. Update the `guardian.api.key` field.

### 3. Run the Application
The easiest way to run the project is using Docker Compose. This will build the application and start the MongoDB and Elasticsearch instances.

```bash
docker-compose up --build
```

- **Application URL (Docker)**: `http://localhost:8082` (mapped from container port 8080)
- **Local Application URL**: `http://localhost:8081` (when running locally)
- **MongoDB**: `localhost:27017`
- **Elasticsearch**: `localhost:9200`

## 🛠️ Usage Guide

The application provides a feature-rich Web Dashboard for interacting with the data pipeline.

### Web Dashboard
Access the dashboard at `http://localhost:8082` (Docker) or `http://localhost:8081` (Local).

- **Crawl & Train Documents**: Found in the sidebar under **"Crawl & Filter Documents"**.
    - **Search keyword**: Enter a topic, for example  "science" to fetch new articles from The Guardian.
    - **Filter by topics (optional)**: Provide a comma-separated list of topic IDs, for example `1,3,5` to filter documents before saving to the database.
    - **Crawl & Train Topics**: This button triggers the pipeline to fetch articles, automatically train the MALLET topic model, and index results.
- **Topic Exploration**: View discovered topics in the sidebar under **"Available Topics"**.
    - Click any topic to filter the main document list for that theme.
    - Each topic shows its top representative keywords.
- **Real-time Search**: Use the search bar at the top with the placeholder **"Search documents by title..."** to filter results instantly.
- **Topic Enrichment**: Each document card displays color-coded badges indicating its primary topics and confidence weights, for example Topic 6: 68%.
- **Data Management**: Under **"Admin Actions"** in the sidebar, use the **"Delete All Data"** button to clear the Elasticsearch index and MongoDB collections for a fresh start.

### 🔌 Advanced API Usage (CLI)

If you prefer using the command line, you can interact with the services directly:

#### Ingestion & Pipeline
Trigger the unified pipeline (crawl -> train -> index):
```bash
curl -X POST "http://localhost:8082/api/trigger/crawl-and-index?topic=science"
```

#### Search & Data Retrieval
Query the discovery status or fetch topic distributions:
```bash
curl "http://localhost:8082/api/gateway/topics"
```

#### Admin Actions
Synchronize from MongoDB to Elasticsearch or clear all data:
```bash
curl -X POST "http://localhost:8082/admin/index"
curl -X DELETE "http://localhost:8082/admin/all"
```

## 🧪 Running Tests

To run the automated test suite, you need to have Java 17 and Maven installed, or use the provided Maven wrapper:

#### Run All Tests
```bash
./mvnw clean test
```

#### Only Unit Tests
```bash
./mvnw test -Dtest="*Test"
```

#### Run Integration Tests
To run tests that require external resources (MongoDB/Elasticsearch via Testcontainers):
```bash
./mvnw test -Dtest="*IntegrationTest"
```

#### Run Specific Tests
```bash
./mvnw test -Dtest=RelevanceFilterServiceTest (Example)
```

## 📂 Project Structure
- `src/main/java`: Backend source code.
- `compose.yaml`: Docker Compose configuration.
- `Dockerfile`: Multi-stage build for the Spring Boot app.
