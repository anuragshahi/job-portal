#!/bin/bash

# Define colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

KEYCLOAK_DIR="keycloak"
KEYCLOAK_COMPOSE="docker-compose.yml"
OBSERVABILITY_COMPOSE="docker/docker-compose.yml"

start_services() {
    echo -e "${YELLOW}Starting SEC Microservices Environment...${NC}"

    # ==========================================
    # 1. Start Keycloak (Docker Compose)
    # ==========================================
    echo -e "\n${YELLOW}[1/6] Starting Keycloak...${NC}"
    docker compose -f "$KEYCLOAK_DIR/$KEYCLOAK_COMPOSE" up -d
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Keycloak started/checked successfully.${NC}"
        echo -e "${YELLOW}Waiting for Keycloak to be ready (this might take a moment)...${NC}"
        # Give Keycloak time to initialize app context after port is open
        sleep 10
    else
        echo -e "${RED}Failed to start Keycloak.${NC}"
        exit 1
    fi

    # ==========================================
    # 2. Start Observability (Zipkin)
    # ==========================================
    echo -e "\n${YELLOW}[2/6] Starting Observability (Zipkin)...${NC}"
    docker compose -f "$OBSERVABILITY_COMPOSE" up -d
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Zipkin started successfully. UI: http://localhost:9411${NC}"
    else
        echo -e "${RED}Failed to start Zipkin.${NC}"
        exit 1
    fi

    # ==========================================
    # 3. Start Profile DB (Docker Compose)
    # ==========================================
    echo -e "\n${YELLOW}[3/6] Starting Profile DB...${NC}"
    PROFILE_COMPOSE="profile-service/docker-compose.yml"
    
    docker compose -f "$PROFILE_COMPOSE" up -d
    if [ $? -eq 0 ]; then
         echo -e "${GREEN}Profile DB started/checked successfully.${NC}"
    else
         echo -e "${RED}Failed to start Profile DB.${NC}"
         exit 1
    fi

    # ==========================================
    # 4. Start Order DB (Docker Compose)
    # ==========================================
    echo -e "\n${YELLOW}[4/6] Starting Order DB...${NC}"
    ORDER_COMPOSE="order-service/docker-compose.yml"
    
    docker compose -f "$ORDER_COMPOSE" up -d
    if [ $? -eq 0 ]; then
         echo -e "${GREEN}Order DB started/checked successfully.${NC}"
    else
         echo -e "${RED}Failed to start Order DB.${NC}"
         exit 1
    fi

    # ==========================================
    # 5. Start Spring Boot Services
    # ==========================================
    start_service_app() {
        SERVICE_DIR=$1
        PORT=$2
        NAME=$3

        echo -e "\n${YELLOW}[Checking $NAME]${NC}"
        
        # Check if port is in use
            if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
                echo -e "${GREEN}$NAME is already running on port $PORT.${NC}"
            else
                echo -e "${YELLOW}Starting $NAME on port $PORT...${NC}"
                # Run in background with nohup, redirect output to a log file
                (cd "$SERVICE_DIR" && nohup mvn spring-boot:run > run.log 2>&1 &)
                
                # Give it a split second to fail immediately if something is wrong
                sleep 2
                if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
                     echo -e "${GREEN}$NAME started (PID: $(lsof -Pi :$PORT -sTCP:LISTEN -t)).Logs: $SERVICE_DIR/run.log${NC}"
                else
                     echo -e "${YELLOW}$NAME startup initiated. Check logs at $SERVICE_DIR/run.log${NC}"
                fi
            fi    }

    # Start Services
    # BFF (Port 8081)
    start_service_app "bff" 8081 "BFF Service"

    # Gateway (Port 8888)
    start_service_app "gateway" 8888 "Gateway Service"

    # Profile Service (Port 8082)
    start_service_app "profile-service" 8082 "Profile Service"

    # Order Service (Port 8083)
    start_service_app "order-service" 8083 "Order Service"

    # Keycloak Admin Service (Port 8084)
    start_service_app "keycloak-admin-service" 8084 "Keycloak Admin Service"

    echo -e "\n${GREEN}All services have been processed.${NC}"
    echo -e "${YELLOW}Note: Spring Boot services take some time to fully initialize. Check logs if they are not accessible immediately.${NC}"
}

stop_services() {
    echo -e "${YELLOW}Stopping SEC Microservices Environment...${NC}"

    stop_process_on_port() {
        PORT=$1
        NAME=$2
        PID=$(lsof -Pi :$PORT -sTCP:LISTEN -t)
        if [ -n "$PID" ]; then
            echo -e "${YELLOW}Stopping $NAME (PID: $PID)...${NC}"
            kill $PID
            echo -e "${GREEN}$NAME stopped.${NC}"
        else
            echo -e "${GREEN}$NAME is not running.${NC}"
        fi
    }

    # Stop Spring Boot Apps
    stop_process_on_port 8081 "BFF Service"
    stop_process_on_port 8888 "Gateway Service"
    stop_process_on_port 8082 "Profile Service"
    stop_process_on_port 8083 "Order Service"
    stop_process_on_port 8084 "Keycloak Admin Service"

    # Stop Keycloak
    echo -e "\n${YELLOW}Stopping Keycloak...${NC}"
    docker compose -f "$KEYCLOAK_DIR/$KEYCLOAK_COMPOSE" stop
    echo -e "${GREEN}Keycloak stopped.${NC}"

    # Stop Observability (Zipkin)
    echo -e "\n${YELLOW}Stopping Zipkin...${NC}"
    docker compose -f "$OBSERVABILITY_COMPOSE" stop
    echo -e "${GREEN}Zipkin stopped.${NC}"

    # Stop Profile DB
    echo -e "\n${YELLOW}Stopping Profile DB...${NC}"
    docker compose -f "profile-service/docker-compose.yml" stop
    echo -e "${GREEN}Profile DB stopped.${NC}"

    # Stop Order DB
    echo -e "\n${YELLOW}Stopping Order DB...${NC}"
    docker compose -f "order-service/docker-compose.yml" stop
    echo -e "${GREEN}Order DB stopped.${NC}"
    
    echo -e "\n${GREEN}All services stopped.${NC}"
}

test_services() {
    echo -e "${YELLOW}Running tests for all services...${NC}"

    run_test() {
        SERVICE_DIR=$1
        NAME=$2
        echo -e "\n${YELLOW}[Testing $NAME]${NC}"
        (cd "$SERVICE_DIR" && mvn test)
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}$NAME tests passed.${NC}"
        else
            echo -e "${RED}$NAME tests failed.${NC}"
            exit 1
        fi
    }

    run_test "bff" "BFF Service"
    run_test "gateway" "Gateway Service"
    run_test "profile-service" "Profile Service"
    run_test "order-service" "Order Service"
    run_test "keycloak-admin-service" "Keycloak Admin Service"

    echo -e "\n${GREEN}All tests passed successfully!${NC}"
}

# Main Execution Logic
case "$1" in
    start|"")
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        ;;
    test)
        test_services
        ;;
    *)
        echo -e "${RED}Usage: $0 {start|stop|restart|test}${NC}"
        exit 1
        ;;
esac
