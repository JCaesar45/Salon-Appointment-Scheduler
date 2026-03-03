#!/usr/bin/env python3
"""
Luxe Salon Appointment Scheduler
Python Implementation with PostgreSQL Backend
Enterprise-grade CLI application with rich UI
"""

import psycopg2
import psycopg2.extras
import re
import sys
from datetime import datetime
from typing import Optional, List, Dict, Tuple


class DatabaseManager:
   """Handles all database operations with connection pooling"""
   
   def __init__(self, dbname: str = "salon", user: str = "freecodecamp", 
                password: str = "", host: str = "localhost", port: str = "5432"):
       self.connection_params = {
           "dbname": dbname,
           "user": user,
           "password": password,
           "host": host,
           "port": port
       }
       self.conn = None
       self.cursor = None
   
   def connect(self) -> bool:
       """Establish database connection"""
       try:
           self.conn = psycopg2.connect(**self.connection_params)
           self.cursor = self.conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
           return True
       except psycopg2.Error as e:
           print(f"Database connection failed: {e}")
           return False
   
   def disconnect(self):
       """Close database connection"""
       if self.cursor:
           self.cursor.close()
       if self.conn:
           self.conn.close()
   
   def initialize_database(self):
       """Create tables and seed initial data"""
       schema = """
       DROP TABLE IF EXISTS appointments CASCADE;
       DROP TABLE IF EXISTS customers CASCADE;
       DROP TABLE IF EXISTS services CASCADE;
       
       CREATE TABLE customers (
           customer_id SERIAL PRIMARY KEY,
           name VARCHAR(255) NOT NULL,
           phone VARCHAR(255) UNIQUE NOT NULL
       );
       
       CREATE TABLE services (
           service_id SERIAL PRIMARY KEY,
           name VARCHAR(255) NOT NULL
       );
       
       CREATE TABLE appointments (
           appointment_id SERIAL PRIMARY KEY,
           customer_id INTEGER REFERENCES customers(customer_id),
           service_id INTEGER REFERENCES services(service_id),
           time VARCHAR(255) NOT NULL
       );
       
       INSERT INTO services (service_id, name) VALUES
           (1, 'cut'),
           (2, 'color'),
           (3, 'perm'),
           (4, 'style'),
           (5, 'trim');
       """
       
       try:
           self.cursor.execute(schema)
           self.conn.commit()
           print("Database initialized successfully")
       except psycopg2.Error as e:
           print(f"Schema creation failed: {e}")
           self.conn.rollback()
   
   def get_services(self) -> List[Dict]:
       """Retrieve all available services"""
       self.cursor.execute("SELECT service_id, name FROM services ORDER BY service_id")
       return self.cursor.fetchall()
   
   def get_service_by_id(self, service_id: int) -> Optional[Dict]:
       """Get specific service details"""
       self.cursor.execute(
           "SELECT * FROM services WHERE service_id = %s", 
           (service_id,)
       )
       return self.cursor.fetchone()
   
   def find_customer_by_phone(self, phone: str) -> Optional[Dict]:
       """Lookup customer by phone number"""
       self.cursor.execute(
           "SELECT * FROM customers WHERE phone = %s",
           (phone,)
       )
       return self.cursor.fetchone()
   
   def create_customer(self, name: str, phone: str) -> int:
       """Insert new customer and return ID"""
       self.cursor.execute(
           "INSERT INTO customers (name, phone) VALUES (%s, %s) RETURNING customer_id",
           (name, phone)
       )
       customer_id = self.cursor.fetchone()['customer_id']
       self.conn.commit()
       return customer_id
   
   def create_appointment(self, customer_id: int, service_id: int, time: str) -> int:
       """Schedule new appointment"""
       self.cursor.execute(
           """INSERT INTO appointments (customer_id, service_id, time) 
              VALUES (%s, %s, %s) RETURNING appointment_id""",
           (customer_id, service_id, time)
       )
       appointment_id = self.cursor.fetchone()['appointment_id']
       self.conn.commit()
       return appointment_id
   
   def get_customer_appointments(self, customer_id: int) -> List[Dict]:
       """Retrieve all appointments for a customer"""
       self.cursor.execute("""
           SELECT a.*, s.name as service_name 
           FROM appointments a
           JOIN services s ON a.service_id = s.service_id
           WHERE a.customer_id = %s
           ORDER BY a.appointment_id DESC
       """, (customer_id,))
       return self.cursor.fetchall()


class SalonScheduler:
   """Main application controller with rich CLI interface"""
   
   # ANSI color codes for terminal styling
   GOLD = '\033[38;5;220m'
   WHITE = '\033[97m'
   GRAY = '\033[90m'
   GREEN = '\033[92m'
   RED = '\033[91m'
   RESET = '\033[0m'
   BOLD = '\033[1m'
   
   def __init__(self):
       self.db = DatabaseManager()
       self.current_customer = None
       self.selected_service = None
   
   def print_header(self, text: str):
       """Print styled header"""
       width = 50
       print(f"\n{self.GOLD}{'='*width}{self.RESET}")
       print(f"{self.BOLD}{self.WHITE}{text.center(width)}{self.RESET}")
       print(f"{self.GOLD}{'='*width}{self.RESET}\n")
   
   def print_success(self, message: str):
       """Print success message"""
       print(f"{self.GREEN}✓ {message}{self.RESET}")
   
   def print_error(self, message: str):
       """Print error message"""
       print(f"{self.RED}✗ {message}{self.RESET}")
   
   def print_info(self, message: str):
       """Print info message"""
       print(f"{self.GRAY}ℹ {message}{self.RESET}")
   
   def format_phone(self, phone: str) -> str:
       """Standardize phone number format"""
       digits = re.sub(r'\D', '', phone)
       if len(digits) == 10:
           return f"({digits[:3]}) {digits[3:6]}-{digits[6:]}"
       return phone
   
   def validate_phone(self, phone: str) -> bool:
       """Validate phone number has 10 digits"""
       digits = re.sub(r'\D', '', phone)
       return len(digits) == 10
   
   def display_services(self) -> List[Dict]:
       """Show available services with styling"""
       services = self.db.get_services()
       
       print(f"\n{self.BOLD}{self.GOLD}Available Services:{self.RESET}\n")
       
       for svc in services:
           print(f"  {self.GOLD}{svc['service_id']}){self.RESET} {self.WHITE}{svc['name'].title()}{self.RESET}")
       
       return services
   
   def select_service(self) -> bool:
       """Handle service selection with validation"""
       while True:
           services = self.display_services()
           
           choice = input(f"\n{self.GRAY}Enter service number: {self.RESET}").strip()
           
           if not choice.isdigit():
               self.print_error("Please enter a valid number")
               continue
           
           service_id = int(choice)
           service = self.db.get_service_by_id(service_id)
           
           if service:
               self.selected_service = service
               self.print_success(f"Selected: {service['name'].title()}")
               return True
           else:
               self.print_error("Invalid service selection")
   
   def get_customer_info(self) -> bool:
       """Handle customer lookup or creation"""
       phone = input(f"\n{self.GRAY}What's your phone number? {self.RESET}").strip()
       
       if not self.validate_phone(phone):
           self.print_error("Please enter a valid 10-digit phone number")
           return self.get_customer_info()
       
       formatted_phone = self.format_phone(phone)
       
       # Check existing customer
       customer = self.db.find_customer_by_phone(formatted_phone)
       
       if customer:
           self.current_customer = customer
           self.print_success(f"Welcome back, {customer['name']}!")
           return True
       
       # New customer flow
       self.print_info("I don't have a record for that phone number.")
       name = input(f"{self.GRAY}What's your name? {self.RESET}").strip()
       
       if not name:
           self.print_error("Name cannot be empty")
           return self.get_customer_info()
       
       # Create new customer
       customer_id = self.db.create_customer(name, formatted_phone)
       self.current_customer = {
           'customer_id': customer_id,
           'name': name,
           'phone': formatted_phone
       }
       self.print_success(f"Profile created for {name}")
       return True
   
   def schedule_time(self) -> bool:
       """Get appointment time"""
       time = input(f"\n{self.GRAY}What time would you like your {self.selected_service['name']}, {self.current_customer['name']}? {self.RESET}").strip()
       
       if not time:
           self.print_error("Please enter a time")
           return self.schedule_time()
       
       # Create appointment
       appointment_id = self.db.create_appointment(
           self.current_customer['customer_id'],
           self.selected_service['service_id'],
           time
       )
       
       # Confirmation
       print(f"\n{self.GOLD}{'='*50}{self.RESET}")
       print(f"{self.GREEN}{self.BOLD}")
       print(f"  I have put you down for a {self.selected_service['name']} at {time},")
       print(f"  {self.current_customer['name']}.")
       print(f"{self.RESET}")
       print(f"{self.GOLD}{'='*50}{self.RESET}\n")
       
       return True
   
   def view_appointments(self):
       """Display customer's appointment history"""
       if not self.current_customer:
           return
       
       appointments = self.db.get_customer_appointments(
           self.current_customer['customer_id']
       )
       
       if not appointments:
           self.print_info("No appointments scheduled")
           return
       
       print(f"\n{self.BOLD}{self.GOLD}Your Appointments:{self.RESET}\n")
       for apt in appointments:
           print(f"  {self.WHITE}• {apt['service_name'].title()} at {apt['time']}{self.RESET}")
   
   def run(self):
       """Main application loop"""
       if not self.db.connect():
           sys.exit(1)
       
       # Initialize if needed (uncomment for first run)
       # self.db.initialize_database()
       
       try:
           self.print_header("LUXE SALON APPOINTMENT SCHEDULER")
           print(f"{self.GRAY}Welcome to our premium scheduling system{self.RESET}")
           
           # Service selection
           if not self.select_service():
               return
           
           # Customer identification
           if not self.get_customer_info():
               return
           
           # Time scheduling
           if not self.schedule_time():
               return
           
           # Show summary
           self.view_appointments()
           
       except KeyboardInterrupt:
           print(f"\n\n{self.GRAY}Scheduling cancelled.{self.RESET}")
       except Exception as e:
           self.print_error(f"An error occurred: {e}")
       finally:
           self.db.disconnect()
           print(f"\n{self.GOLD}Thank you for choosing Luxe Salon{self.RESET}\n")


def main():
   """Entry point"""
   scheduler = SalonScheduler()
   scheduler.run()


if __name__ == "__main__":
   main()
