# Keycloak and MySQL

This directory contains a `docker-compose.yml` file to set up Keycloak and a MySQL database.

## Prerequisites

* Docker
* Docker Compose

## Usage

1.  **Start the services:**

    ```bash
    docker-compose up -d
    ```

2.  **Access Keycloak:**

    Open your browser and navigate to `http://localhost:8080`.

3.  **Admin Console:**

    *   Username: `admin`
    *   Password: `admin`

4.  **Database:**

    A MySQL database named `keycloak` will be running on port `3306`.

## Realm Configuration

The `docker-compose.yml` is configured to import a realm configuration from the `realm-config` directory. You will need to create this directory and place your realm export file (e.g., `realm-export.json`) in it.
