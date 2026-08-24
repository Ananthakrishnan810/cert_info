# 🛡️ Certificate Manager

A modern, full-stack Spring Boot web application designed for monitoring SSL/TLS and service certificates across infrastructure clusters, featuring automated daily email expiration alerts, local file-based data persistence, and Spring Security authentication.

---

## ✨ Features

- **🔐 Spring Security Authentication**: Secure login overlay protecting dashboard access (`ethan_hunt` / `ethanhunt@10`). User credentials are persisted and loaded from `user_credentials.txt`.
- **📁 Cluster & Certificate Management**: Group SSL/TLS certificates by infrastructure clusters. Add, edit, view, and delete certificate details with real-time status calculations (Active, Expiring, Expired).
- **⏰ Automated Daily Expiration Scheduler**: Background Spring `@Scheduled` task that runs daily at 8:00 AM to check certificate expiration dates and notify configured recipient email addresses 7 days prior to expiry.
- **📧 Gmail SMTP Integration**: Direct email alert delivery via Spring Mail (STARTTLS Port 587) with customizable HTML email alert templates.
- **💾 Zero-Database File Storage**: Cluster and certificate data are saved to `cluster_certificate_data.txt` in human-readable JSON format, enabling quick deployment without database setup.
- **🎨 Glassmorphism Dark UI**: A responsive, dark-mode user dashboard with smooth CSS glassmorphism effects and modern typography.

---

## 🛠️ Technology Stack

- **Backend Framework**: Java 17, Spring Boot 3.2.5
- **Security**: Spring Security 6 (BCrypt Password Encoder, Session Management)
- **Email Dispatch**: Spring Boot Mail (`JavaMailSender`), Jakarta Mail
- **Data Format**: Jackson JSON Serialization / Deserialization
- **Frontend**: HTML5, Vanilla JavaScript (ES6+), Modern Vanilla CSS3 (Glassmorphism), Font Awesome 6
- **Build Tool**: Apache Maven

---

## 🚀 Quick Start & Installation

### Prerequisites
- **Java Development Kit (JDK)** 17 or higher
- **Apache Maven** 3.8+

### 1. Clone the Repository
```bash
git clone https://github.com/ananthakpm/cert_info.git
cd cert_info
```

### 2. Configure Credentials & Properties

Ensure your `src/main/resources/application.properties` includes your Gmail SMTP details (or environment variables):

```properties
server.port=8080

# Gmail SMTP Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${GMAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${GMAIL_APP_PASSWORD:your-app-password}
```

### 3. Build & Run Application

Execute the following command in the project root directory:

```bash
mvn clean spring-boot:run
```

### 4. Access the Dashboard

Open your browser and navigate to:
```
http://localhost:8080
```

- **Default Username**: `ethan_hunt`
- **Default Password**: `ethanhunt@10`

---

## 📊 File Storage Format

- `user_credentials.txt`: Stores system administrator credentials (`username:password`).
- `cluster_certificate_data.txt`: Stores JSON records of all monitored clusters, recipient emails, and certificate expiration dates.

---

## 📡 API Endpoints Summary

| HTTP Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate user against stored credentials |
| `GET` | `/api/auth/status` | Check active session authentication status |
| `POST` | `/api/auth/logout` | Invalidate active session |
| `GET` | `/api/clusters` | Retrieve all clusters & certificates |
| `POST` | `/api/clusters` | Create a new cluster |
| `POST` | `/api/clusters/certificates` | Add a new certificate to a cluster |
| `PUT` | `/api/clusters/{clusterId}/certificates/{certId}` | Update an existing certificate |
| `DELETE` | `/api/clusters/{clusterId}/certificates/{certId}` | Delete a certificate |
| `DELETE` | `/api/clusters/{clusterId}` | Delete a cluster |

---

## 🤝 Contributors

- **Ananthakrishnan** ([@ananthakpm](https://github.com/ananthakpm))
- **Antigravity AI Assistant** (Pair Programming Contributor)

---

## 📄 License

This project is licensed under the MIT License.
