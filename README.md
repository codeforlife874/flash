## ⚡ flash: automated troubleshooter  

> A self-evolving decision engine designed to automate troubleshooting workflows and reduce customer support load.  

Flash is an intelligent troubleshooting system powered by an ever-growing **Binary Decision Tree**. Users are guided through dynamic Yes/No questions until they reach a final solution. 

But unlike traditional troubleshooters, Flash introduces a breakthrough:  
#### 🔥 Admin-driven Dynamic Knowledge Expansion  
Any solution node can be **converted into a question** with two new outcomes — instantly expanding the tree. No coding. No server restarts. No downtime.  Flash becomes smarter every time an admin interacts with it.  

---  

## Why Flash Exists  
Companies pay heavily for support agents to repeatedly solve common issues. Flash replaces repetitive tickets with an intelligent, automated system that:  

- ❓ asks structured diagnostic questions  
- 🌳 builds real-time decision paths  
- 🧩 adapts when solutions evolve  
- 👩‍💻 empowers non-technical admins  

The result: faster resolutions, lower support cost, and continuously improving logic.  


---  

## Core Features  

### 1. Interactive Troubleshooting Flow  
Users answer Yes/No inputs through a clean, responsive UI. Flash tracks their progress until a solution is reached. Session-based state means multiple users can troubleshoot independently.  

### 2. Dynamic Tree Expansion (The Innovation)  
Admins can modify a leaf node in real-time:  

1. Select a solution node  
2. Replace it with a new question  
3. Add Yes/No outcomes  
4. Flash instantly rebuilds and serves the updated tree  

This is what makes Flash superior to static troubleshooters.  

###  3. O(1) Node Access via Lookup Table  
Every decision node is indexed by ID → reference mapping. Expansion and traversal never require tree reconstruction.  

### 4. Multi-user Safe by Design  
Each session stores user progress independently — allowing unlimited parallel usage.  

---  

## Tech Stack  

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3 |
| **Frontend** | Thymeleaf + Bootstrap 5 |
| **Data Parsing** | Jackson |
| **Build Tool** | Maven |
| **Other** | Lombok, DevTools |  

---  

## ⚙️ Installation & Setup  

### ✔ Requirements  
- Java 17+  
- Maven (optional — wrapper included)  

### 📥 Clone the repository  
```bash
git clone https://github.com/flurry101/flash.git
cd flash
````

### 🧱 Build the project

Windows:

```bash
.\mvnw clean package
```

Mac/Linux:

```bash
./mvnw clean package
```

### ▶ Run the application

```bash
.\mvnw spring-boot:run
```

### 🌍 Open in Browser

```
http://localhost:8080
```

---

## 📂 Project Architecture

```text
src/main/java/com/example/demo/
│
├── controller
│   └── TroubleshooterController.java     # Manages navigation, session flow, routing
│
├── model
│   ├── Node.java                         # Abstract parent class (encapsulation/ISM)
│   ├── QuestionNode.java                 # Internal decision node
│   └── SolutionNode.java                 # Leaf result node
│
└── service
    └── TreeService.java                  # Core engine: load, traverse, expand, index
```

Tree data stored in:

```
src/main/resources/data/*.json
```

---

## 🧪 Feature Walkthrough: Dynamic Expansion

Follow this test flow to validate Flash’s unique feature set:

1️⃣ Run the app
2️⃣ Navigate until you hit a solution
3️⃣ Scroll below to the Admin Zone
4️⃣ Enter:

* A new question
* Yes-outcome solution
* No-outcome solution
  5️⃣ Submit the form
  6️⃣ Restart the decision path
  7️⃣ Confirm Flash replaced your old solution with your new question 🎉

This demonstrates successful dynamic mutation of the Binary Decision Tree.

---

## 📈 Performance & Scalability

* O(1) lookup operations
* Session-isolated navigation
* No performance drop as data expands
* Unlimited branching depth
* Zero-downtime knowledge growth

Flash remains fast.

---

## 🤝 Contributing

We welcome enhancements and ideas.

```bash
git checkout -b feature/upgrade
git commit -m "Added improvement"
git push origin feature/upgrade
```

Then open a PR.

---

## Final Notes

Flash isn’t just a Yes/No tree, it’s a **learning system**.
Every admin update makes Flash:

* smarter
* deeper
* richer
* more accurate

>What starts small becomes an automated support engine.
>User by user. Question by question.