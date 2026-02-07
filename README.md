🛒 E-Commerce Web Application (Spring Boot + MySQL + Docker)

A full-stack e-commerce web application built with Spring Boot, Thymeleaf, MySQL, and Docker.
The project implements real-world e-commerce features including authentication, cart management, order processing, and an admin dashboard.

This project focuses on clean backend architecture, database design, and production-ready deployment practices.

📌 Overview

This application provides:

A complete shopping experience for users

Secure authentication and role-based authorization

Admin tools for managing products, categories, and orders

Dockerized environment for consistent development and deployment

It is designed using a layered architecture to separate concerns and improve maintainability.

🚀 Features
👤 User Features

User registration and login (Spring Security + BCrypt)

Browse products and categories

Product details page

Shopping cart (add, update, remove items)

Order placement and order history

🛠️ Admin Features

Product CRUD (create, update, delete)

Category management

Order management

Stock control

🧰 Tech Stack
Layer	Technology
Backend	Java, Spring Boot, Spring Security, Spring Data JPA (Hibernate)
Frontend	Thymeleaf, HTML, CSS, JavaScript
Database	MySQL
Build Tool	Maven
DevOps	Docker, Docker Compose
🏗️ Architecture & Design

The project follows a layered architecture:

Controller → Service → Repository → Database

Layers Explained

Controller Layer: Handles HTTP requests and renders Thymeleaf views.

Service Layer: Contains business logic such as cart handling and order processing.

Repository Layer: Uses Spring Data JPA to interact with the database.

Entity Layer: Represents database tables as JPA entities.

Core Domain Models

User, Role

Product, Category

Cart, CartItem

Order, OrderItem

🐳 Docker Setup (Recommended)

The application can be run using Docker and Docker Compose.

📦 Services

Spring Boot application

MySQL database

▶️ Run the project with Docker
docker-compose up --build


Application URL:

http://localhost:8080

🧾 Example docker-compose.yml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: ecommerce-mysql
    restart: always
    environment:
      MYSQL_DATABASE: ecommerce
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  app:
    build: .
    container_name: ecommerce-app
    depends_on:
      - mysql
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/ecommerce
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root

volumes:
  mysql_data:

🧾 Example Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

⚙️ Local Setup (Without Docker)
✅ Prerequisites

Java 17+

Maven

MySQL

1️⃣ Clone the repository
git clone https://github.com/3mur-dev/E-Commerce.git
cd E-Commerce

2️⃣ Create database
CREATE DATABASE ecommerce;

3️⃣ Configure application properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8080

4️⃣ Run the application
mvn spring-boot:run

🔐 Security

Passwords are encrypted using BCrypt

Spring Security handles authentication and authorization

Role-based access control (USER / ADMIN)

🧠 Key Design Decisions

Server-side rendering with Thymeleaf for simplicity and tight backend integration.

Layered architecture to keep business logic separate from controllers.

Relational database modeling for carts, orders, and products.

Docker for environment consistency and easier deployment.

📈 Future Improvements

REST API + React/Vue frontend

JWT-based authentication

Payment gateway integration (Stripe/PayPal)

DTOs and MapStruct for cleaner architecture

Unit & integration tests

Caching (Redis)

Advanced search and filtering

CI/CD pipeline (GitHub Actions)

👀 For Reviewers

If you are reviewing this project, focus on:

Order processing and transaction handling

Cart logic and stock synchronization

Security configuration

Entity relationships and database design

Service layer responsibilities and code structure

📬 Contact

GitHub: https://github.com/3mur-dev

Email: omar.dev@example.com
