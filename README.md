🧾 E-Commerce Web Application (Spring Boot)

A full-stack e-commerce web application built using Spring Boot, Thymeleaf, and MySQL.
The project implements core online shopping features such as authentication, product management, cart handling, and order processing, with an admin panel for managing the system.

This project was developed to practice backend architecture, database design, and secure authentication in a real-world application.

🚀 Features
👤 User Features

User registration and login with encrypted passwords (Spring Security + BCrypt)

Browse products by category

View product details

Add, update, and remove items from the shopping cart

Place orders and view order history

🛠️ Admin Features

Create, update, and delete products

Manage categories

View and manage orders

Control product stock

🧰 Tech Stack
Layer	Technology
Backend	Java, Spring Boot, Spring Security, JPA (Hibernate)
Frontend	Thymeleaf, HTML, CSS, JavaScript
Database	MySQL
Build Tool	Maven
🏗️ Project Architecture

The application follows a layered architecture:

Controller → Service → Repository → Database

Main Layers

Controller Layer: Handles HTTP requests and returns Thymeleaf views.

Service Layer: Contains business logic (cart handling, order processing, validation).

Repository Layer: Manages database operations using Spring Data JPA.

Entity Layer: Represents database tables as Java classes.

Key Entities

User, Role

Product, Category

Cart, CartItem

Order, OrderItem

⚙️ Setup & Installation
✅ Prerequisites

Java 17+

Maven

MySQL

Git

1️⃣ Clone the repository
git clone https://github.com/3mur-dev/E-Commerce.git
cd E-Commerce

2️⃣ Create the database
CREATE DATABASE ecommerce;

3️⃣ Configure application properties

Edit src/main/resources/application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8080

4️⃣ Run the application
mvn spring-boot:run


Then open:

http://localhost:8080

🔐 Security

Passwords are encrypted using BCrypt

Authentication and authorization are handled by Spring Security

Role-based access control (USER / ADMIN)

🧠 Design Decisions

Used Spring MVC + Thymeleaf instead of a REST API to simplify integration between backend and frontend.

Applied JPA/Hibernate for ORM to reduce boilerplate SQL.

Implemented a layered architecture to keep business logic separate from controllers.

Used relational database modeling to handle orders, carts, and products efficiently.

📈 Future Improvements

REST API for frontend frameworks (React / Angular)

JWT-based authentication

Pagination and advanced search

Payment gateway integration (Stripe / PayPal)

Docker support

Unit and integration tests

DTOs and MapStruct for cleaner data transfer

Better exception handling and logging

👀 For Reviewers

If you are reviewing this project, focus on:

Order processing logic and transaction handling

Cart and stock synchronization

Security configuration

Entity relationships and database design

Service layer separation and code structure

📬 Contact

GitHub: https://github.com/3mur-dev

Email: omar.dev@example.com
