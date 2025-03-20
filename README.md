# Cloud Storage
## 📑 Table of Contents

- [📝 About the Project](#-about-the-project)
- [⚙️ Features](#-features)
- [🛠️ Technologies](#-technologies)
- [🏃How to run](#-how-to-run)
- [🤝Contributing](#-contributing)
- [🙏 Acknowledgments](#-acknowledgments)
- [👨‍💻 Author](#-author)
---
## 📝 About the Project

Cloud Storage is a web application created to practice
backend development skills. You can view the technical specifications [here](https://zhukovsd.github.io/java-backend-learning-course/projects/cloud-file-storage/).
---
## ⚙️ Features

### 👤 User Management:
- User registration
- User authentication
- Session management

### 📤 File and Folder Operations:
- File and folder uploads
- Creating new directories
- Renaming files and folders
- Downloading files and folders

### 🔍 Search and Navigation:
- Searching files and folders by name
- Folder navigation
- Viewing folder contents

### 💾 Storage Organization:
- Isolated user data storage
- Access to storage content via active sessions
- Preservation of folder hierarchy

---

## 🛠️ Technologies

[![My Skills](https://skillicons.dev/icons?i=java,spring,postgres,redis,maven,react,docker,git,js,&perline=10)](https://skillicons.dev)

---

## 🏃 How to Run

### Prerequisites
- **Java 21+**
- **Maven**
- **Docker & Docker Compose**

### Steps to Run the Project

1. **Clone the Repository**  
   Open a terminal and run:
   ```bash
   git clone https://github.com/iqubb/cloud-storage.git
   cd cloud-storage
   ```
   
2. **Configure Environment Variables**
    ```
    POSTGRES_USER=your_db_user
    POSTGRES_PASSWORD=your_db_password
    POSTGRES_DB=your_db_name
    
    MINIO_ACCESS_KEY=your_minio_access_key
    MINIO_SECRET_KEY=your_minio_secret_key
    MINIO_ROOT_USER=your_minio_root_user
    MINIO_ROOT_PASSWORD=your_minio_root_password
    
    REDIS_PASSWORD=your_redis_password
    ```
   
3. **Run the Spring Boot Application**
   ```bash
    docker-compose up --build -d
   ```
   
4. **Access the Application**

       http://localhost:8080

5. **Stopping the Application and Services**

    To stop the Docker containers, run:
   ```bash
    docker-compose down
   ```
---
## 🤝 Contributing

1. **Fork the project**
2. **Create a new branch**
   ```bash
   git checkout -b feature/new-feature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add some feature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/new-feature
   ```
5. **Open a Pull Request**

---
## 🙏 Acknowledgments

- **[Sergey Zhukov](https://t.me/zhukovsd)
  for technical requirements.**
- **[MrShoffen](https://t.me/MrShoffen)
    for frontend.**

---
## 👨‍💻 Author
Reach out to me on [telegram](https://t.me/qubby)