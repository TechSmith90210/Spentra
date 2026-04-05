# Spentra

Spentra is a full-stack expense tracking application designed to help you manage and track your daily expenses efficiently.

## Project Structure

The project contains both the backend and frontend codebases:

- `spentra-backend/`: The backend REST API built with Spring Boot, Java, and PostgreSQL.
- `spentra-frontend/`: The frontend web application built with Next.js.

## Technologies Used

**Backend:**
- Java 17
- Spring Boot
- PostgreSQL
- Maven

**Frontend:**
- Next.js
- React
- Node.js

## Getting Started

### Prerequisites
- Java 17 or higher
- Node.js 18+ & npm
- PostgreSQL database

### Setting up the Backend
1. Navigate to the backend directory:
   ```bash
   cd spentra-backend
   ```
2. Ensure you have a running PostgreSQL instance and update your database credentials in `src/main/resources/application.properties`.
3. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend API will start on `http://localhost:8080`.

### Setting up the Frontend
1. Navigate to the frontend directory:
   ```bash
   cd spentra-frontend
   ```
2. Install the necessary dependencies:
   ```bash
   npm install
   ```
3. Start the Next.js development server:
   ```bash
   npm run dev
   ```
   The frontend will be accessible at `http://localhost:3000`.
