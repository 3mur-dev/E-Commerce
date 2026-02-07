E-Commerce 🛒✨
[
[
[
[
[

Full-stack e-commerce platform with Spring Boot 4.0, Hibernate 7.1.8, Thymeleaf & MySQL. Complete cart, checkout, authentication, and order management.

<img src="https://github.com/3mur-dev/E-Commerce/blob/main/screenshot.png?raw=true" alt="E-Commerce Demo" width="800"/>
✨ Features
✅ User Authentication - Secure login/register with Spring Security

🛒 Shopping Cart - Add/remove items, update quantities

💳 Checkout Flow - Complete order processing with validation

📦 Order Management - View order history and details

🔍 Product Catalog - Browse products with rich details

⭐ Favorites - Save favorite products

📱 Responsive UI - Mobile-first Thymeleaf templates

🛡️ Production Ready - Transaction management, lazy loading fixes

🚀 Quick Start
bash
# Clone the repo
git clone https://github.com/3mur-dev/E-Commerce.git
cd E-Commerce

# Start MySQL (docker)
docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ecommerce mysql:8.0

# Run the app
mvn spring-boot:run
Open http://localhost:8080 🎉

🛠️ Tech Stack
Frontend	Backend	Database	Tools
Thymeleaf 3.1.3	Spring Boot 4.0.0	MySQL 8.0.43	Maven
Bootstrap 5	Spring Security 7.0	Hibernate 7.1.8	HikariCP
HTML5/CSS3	Spring Data JPA 4.0		Lombok
📋 Prerequisites
Java 21 (JDK 21.0.9)

MySQL 8.0+ (or Docker)

Maven 3.9+

Node.js (optional, for dev tools)

🗄️ Database Setup
Option 1: Docker (Recommended)
bash
docker run -d \
  --name ecommerce-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=ecommerce \
  mysql:8.0.43
Option 2: Local MySQL
sql
CREATE DATABASE ecommerce;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
Update application.properties:

text
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
📁 Project Structure
text
E-Commerce/
├── controllers/     # REST + MVC controllers
├── entities/        # JPA entities (User, Product, Order, OrderItem)
├── repositories/    # Spring Data JPA repositories
├── services/        # Business logic
├── templates/       # Thymeleaf HTML templates
├── static/          # CSS/JS/images
└── resources/
    └── application.properties
🎮 Usage
1. Register/Login
text
POST /register - Create new user
POST /login - Spring Security login
2. Shopping Flow
text
GET /products - Browse catalog
POST /cart/add/{productId} - Add to cart
GET /cart - View cart
POST /checkout - Complete purchase
GET /thank - Order confirmation
3. Admin Features
text
GET /admin/products - Manage products
POST /admin/product - CRUD operations
💾 Database Schema
sql
Users → Orders → OrderItems → Products
    ↕         ↕          ↕
favorites    items     product
Key Relations:

User 1 ↔ * Orders

Order 1 ↔ * OrderItems

OrderItem 1 ↔ 1 Product

🔧 Configuration
Property	Default	Description
server.port	8080	Web server port
spring.jpa.hibernate.ddl-auto	validate	Schema validation
spring.jpa.show-sql	false	Show SQL queries
spring.datasource.hikari.maximum-pool-size	10	DB connection pool
🧪 Testing
bash
# Unit tests
mvn test

# Integration tests
mvn test -Dtest=IntegrationTest

# Coverage
mvn jacoco:report
🚀 Production Deployment
bash
# Build JAR
mvn clean package -DskipTests

# Docker
docker build -t ecommerce .
docker run -p 8080:8080 ecommerce
Docker Compose:

text
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
  mysql:
    image: mysql:8.0.43
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ecommerce
📱 API Endpoints
Method	Endpoint	Description	Auth
GET	/products	List products	No
POST	/cart/add/{id}	Add to cart	Yes
GET	/cart	View cart	Yes
POST	/checkout	Process order	Yes
GET	/orders	Order history	Yes
🐛 Troubleshooting
Issue	Solution
LazyInitializationException	Add @Transactional(readOnly = true) to controllers
Access denied	Check spring.security.user.name/password
Table doesn't exist	spring.jpa.hibernate.ddl-auto=update
Connection refused	Start MySQL first
🔮 Roadmap
 Shopping cart + checkout

 Order management + thank you page

 Fix Hibernate lazy loading

 Payment integration (Stripe/PayPal)

 REST API + Swagger docs

 Product search + categories

 Email notifications

 Admin dashboard

🤝 Contributing
Fork the project

Create feature branch (git checkout -b feature/AmazingFeature)

Commit changes (git commit -m 'Add some AmazingFeature')

Push to branch (git push origin feature/AmazingFeature)

Open Pull Request

Code Style:

4 spaces indentation

@AllArgsConstructor Lombok

@Transactional for services/controllers

Thymeleaf: th:if, th:each, th:text

📄 License
This project is MIT licensed - see LICENSE file for details.

🙌 Acknowledgments
Built with ❤️ using:

Spring Boot

Thymeleaf

Bootstrap

MySQL


My Email: 3mur1111@gmail.com
