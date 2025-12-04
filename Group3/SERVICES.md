# Ingestion and Model Services Documentation

This document provides an overview of the `ingestion` and `model` packages within the project, detailing their responsibilities, components, and associated unit tests.

## 1. Model Package (`com.unipd.dei.sp.model`)

The `model` package defines the core data structures used across the application.

### Components

#### `Document`
- **Type**: Java Record
- **Description**: Represents a crawled document or article.
- **Fields**:
    - `id` (String): Unique identifier for the document.
    - `url` (String): Source URL of the document.
    - `content` (String): The main text content of the article.
    - `source` (String): The source of the article (e.g., "The Guardian").
    - `timestamp` (Instant): Time when the document was crawled or published.
    - `metadata` (Map<String, String>): Additional metadata (e.g., section name).

### Unit Tests (`com.unipd.dei.sp.model`)

#### `DocumentTest`
- **`testDocumentCreation`**: Verifies that a `Document` record is correctly created with all fields initialized.
- **`testDocumentEquality`**: Verifies that two `Document` records with identical fields are considered equal.

---

## 2. Ingestion Package (`com.unipd.dei.sp.ingestion`)

The `ingestion` package is responsible for gathering data from external sources, filtering it, and sending relevant data to the core service.

### Components

#### `IngestionController`
- **Type**: REST Controller
- **Description**: Exposes endpoints to trigger the ingestion pipeline manually.
- **Endpoints**:
    - `POST /api/trigger?topic={topic}`: Triggers the crawling and ingestion process for a specific topic. Defaults to "science".
- **Dependencies**: `CrawlerService`, `RelevanceFilterService`, `RestTemplate`.

#### `GuardianCrawler`
- **Type**: Service (implements `CrawlerService`)
- **Description**: Fetches articles from The Guardian API.
- **Configuration**: Requires `guardian.api.key` in `application.properties`.
- **Key Methods**:
    - `crawl(String topic)`: Queries The Guardian API for articles related to the topic and maps the response to a list of `Document` objects.

#### `RelevanceFilterService`
- **Type**: Service
- **Description**: Evaluates the relevance of a document to a given topic based on a weighted scoring system.
- **Constants**:
    - `THRESHOLD`: **15** (Minimum score required for a document to be considered relevant)
    - `URL_MATCH_SCORE`: **20**
    - `METADATA_MATCH_SCORE`: **10**
    - `CONTENT_MATCH_SCORE`: **1**
- **Scoring Logic**:
    1. **URL Match**: Checks if the topic (case-insensitive) is present in the document URL. If found, adds `URL_MATCH_SCORE`.
    2. **Metadata Match**: Iterates through the document's metadata values. If the topic is found in any value, adds `METADATA_MATCH_SCORE`. This is counted at most once per document to prevent inflation.
    3. **Content Match**: Counts the number of occurrences of the topic in the document content.
        - Uses Regex `\b` word boundaries to match whole words only.
        - Case-insensitive matching.
        - Adds `CONTENT_MATCH_SCORE` for *each* occurrence.
- **Result**: Returns `true` if the total calculated score is greater than or equal to `THRESHOLD`.

### Unit Tests (`com.unipd.dei.sp.ingestion`)

#### `IngestionControllerTest`
- **`testManualTrigger`**: Mocks the dependencies to verify that the controller correctly calls the crawler, filter, and sends relevant documents to the core service.

#### `GuardianCrawlerTest`
- **`testCrawl_Success`**: Mocks `RestTemplate` to simulate a successful API response from The Guardian and verifies that `Document` objects are correctly parsed.
- **`testCrawl_EmptyResponse`**: Verifies behavior when the API returns no results.

#### `RelevanceFilterServiceTest`
- **`testIsRelevant_HighRelevance`**: Verifies that a document with sufficient score is marked as relevant.
- **`testIsRelevant_LowRelevance`**: Verifies that a document with insufficient score is marked as irrelevant.
- **`testIsRelevant_NullTopic`**: Verifies that the service handles null topics gracefully.

