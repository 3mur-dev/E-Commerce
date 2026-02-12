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

add products to whishlists

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

whishlist, whishlistItem

Render Link:

https://e-commerce-shoppio.onrender.com

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

JWT-based authentication

Payment gateway integration (Stripe/PayPal)

Unit & integration tests

👀 For Reviewers

If you are reviewing this project, focus on:

Order processing and transaction handling

Cart logic and stock synchronization

Security configuration

Entity relationships and database design

Service layer responsibilities and code structure

📬 Contact

GitHub: https://github.com/3mur-dev

Email: 3mur@gmail.com

Project's Images:

Home Page:
<img width="1919" height="942" alt="Screenshot 2026-02-12 161942" src="https://github.com/user-attachments/assets/c449409a-ac01-419e-9e1e-2c71bee82a03" />


Products Page:
<img width="1918" height="944" alt="Screenshot 2026-02-12 162010" src="https://github.com/user-attachments/assets/8c0c35ed-d883-4b9e-a65c-a7a85072364b" />

<img width="1917" height="941" alt="Screenshot 2026-02-12 162028" src="https://github.com/user-attachments/assets/a5923677-336f-4cac-acf1-cdb7ea4be994" />


Login Page:
<img width="1919" height="944" alt="Screenshot 2026-02-12 162633" src="https://github.com/user-attachments/assets/c876d44f-3206-45f2-8f47-a88c812cd749" />


Cart Page:
<img width="1919" height="942" alt="Screenshot 2026-02-12 162723" src="https://github.com/user-attachments/assets/4e608900-43c9-4c52-96b7-d31aafd51372" />


CheckOut Page:
<img width="1919" height="944" alt="Screenshot 2026-02-12 162742" src="https://github.com/user-attachments/assets/f5a3e203-c18d-405b-afca-275a67461d16" />


Order Summary Page:
<img width="1919" height="942" alt="Screenshot 2026-02-12 162810" src="https://github.com/user-attachments/assets/ffa05c76-bd40-4f4c-8219-a5fd92f81ad9" />


Register Page:
<img width="1917" height="942" alt="Screenshot 2026-02-12 163151" src="https://github.com/user-attachments/assets/7a0a310e-9088-4cdd-9bc9-2627ccffd780" />


Wishlist Page:
<img width="1919" height="943" alt="Screenshot 2026-02-12 163644" src="https://github.com/user-attachments/assets/460be148-d381-4a77-bd11-a61f50bb7227" />


ShareLink Page:
<img width="1918" height="944" alt="Screenshot 2026-02-12 163659" src="https://github.com/user-attachments/assets/b2eca43c-c3b3-4d78-bed0-7bea429ca24d" />


Contact Page:
<img width="1918" height="944" alt="Screenshot 2026-02-12 163725" src="https://github.com/user-attachments/assets/d5ae89d2-963e-4ccf-82e7-82509382d1dc" />



<img width="1918" height="944" alt="Screenshot 2026-02-12 163725" src="https://github.com/user-attachments/assets/7a7f298f-0f5f-4863-8a25-65586cf39710" />


Admin Dashboard Page:
<img width="1919" height="944" alt="Screenshot 2026-02-12 163245" src="https://github.com/user-attachments/assets/5eb1f799-3d30-4a2a-a590-43ab7073fa7d" />


Manage Orders PageP:
<img width="1918" height="944" alt="Screenshot 2026-02-12 163439" src="https://github.com/user-attachments/assets/281b6411-cd9f-4cdb-8931-33c0d937caa3" />


Manage Products Page:
<img width="1918" height="944" alt="Screenshot 2026-02-12 163257" src="https://github.com/user-attachments/assets/02455b61-b94c-4f70-8b59-1dd3a61f35eb" />

<img width="1918" height="945" alt="Screenshot 2026-02-12 163315" src="https://github.com/user-attachments/assets/26285ef9-ab50-4b23-bd26-33d63264f6da" />


Manage Categories Page:
<img width="1919" height="941" alt="Screenshot 2026-02-12 163533" src="https://github.com/user-attachments/assets/c14d79bb-49f9-477b-8d56-6b1b29e2a2b5" />


Terms Page:
<img width="1914" height="942" alt="image" src="https://github.com/user-attachments/assets/79965091-5cfb-411c-a5b0-f4f7919c4dc6" />

<img width="1918" height="942" alt="Screenshot 2026-02-12 165017" src="https://github.com/user-attachments/assets/aee4d948-2225-4399-923d-d50d044b13c4" />

Privacy Page:
<img width="1919" height="940" alt="Screenshot 2026-02-12 164928" src="https://github.com/user-attachments/assets/3b765f4a-14e8-41a9-9e26-2ccc21f3d022" />

<img width="1917" height="942" alt="Screenshot 2026-02-12 164938" src="https://github.com/user-attachments/assets/4414a9e9-4082-43e8-9bfc-394e96f4b185" />
