#!/bin/bash

# Salon Appointment Scheduler Script

PSQL="psql -X --username=freecodecamp --dbname=salon --tuples-only -c"

# Function to display services
DISPLAY_SERVICES() {
  # Get services from database
  SERVICES=$($PSQL "SELECT service_id, name FROM services ORDER BY service_id")
  
  # Display services
  echo "$SERVICES" | while read SERVICE_ID BAR NAME
  do
    echo "$SERVICE_ID) $NAME"
  done
}

# Function to handle service selection
SELECT_SERVICE() {
  # Display services
  DISPLAY_SERVICES
  
  # Prompt for service selection
  echo -e "\nPlease select a service by entering the service_id:"
  read SERVICE_ID_SELECTED
  
  # Validate service selection
  SERVICE_NAME=$($PSQL "SELECT name FROM services WHERE service_id = $SERVICE_ID_SELECTED")
  
  # If service doesn't exist, show list again
  if [[ -z $SERVICE_NAME ]]
  then
    SELECT_SERVICE
  else
    # Trim whitespace from service name
    SERVICE_NAME=$(echo $SERVICE_NAME | sed 's/ //g')
    SCHEDULE_APPOINTMENT
  fi
}

# Function to schedule appointment
SCHEDULE_APPOINTMENT() {
  # Get customer phone
  echo -e "\nWhat's your phone number?"
  read CUSTOMER_PHONE
  
  # Check if customer exists
  CUSTOMER_NAME=$($PSQL "SELECT name FROM customers WHERE phone = '$CUSTOMER_PHONE'")
  
  # If customer doesn't exist, get name and create customer
  if [[ -z $CUSTOMER_NAME ]]
  then
    echo -e "\nI don't have a record for that phone number, what's your name?"
    read CUSTOMER_NAME
    
    # Insert new customer
    INSERT_CUSTOMER_RESULT=$($PSQL "INSERT INTO customers(name, phone) VALUES('$CUSTOMER_NAME', '$CUSTOMER_PHONE')")
  fi
  
  # Trim whitespace from customer name
  CUSTOMER_NAME=$(echo $CUSTOMER_NAME | sed 's/ //g')
  
  # Get customer_id
  CUSTOMER_ID=$($PSQL "SELECT customer_id FROM customers WHERE phone = '$CUSTOMER_PHONE'")
  CUSTOMER_ID=$(echo $CUSTOMER_ID | sed 's/ //g')
  
  # Get appointment time
  echo -e "\nWhat time would you like your $SERVICE_NAME, $CUSTOMER_NAME?"
  read SERVICE_TIME
  
  # Insert appointment
  INSERT_APPOINTMENT_RESULT=$($PSQL "INSERT INTO appointments(customer_id, service_id, time) VALUES($CUSTOMER_ID, $SERVICE_ID_SELECTED, '$SERVICE_TIME')")
  
  # Confirm appointment
  echo -e "\nI have put you down for a $SERVICE_NAME at $SERVICE_TIME, $CUSTOMER_NAME."
}

# Main execution
echo -e "\n~~~~~ MY SALON ~~~~~\n"
echo -e "Welcome to My Salon, how can I help you?\n"

SELECT_SERVICE
