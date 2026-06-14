# Prize Bond Tracker

## Overview

Prize Bond Tracker is an intelligent Android-based application developed to simplify prize bond management and provide data-driven insights for prize bond investors. The system enables users to securely manage their bonds, access historical draw records, receive real-time notifications, and view AI-generated winning probability predictions.

The platform maintains a centralized database of prize bond draw results collected from official National Savings Pakistan records. Historical draw data spanning the last six years is acquired through automated web scraping and can also be managed manually through an administrative dashboard.

To support informed decision-making, the system employs a Random Forest machine learning model to estimate the winning probability of individual bonds based on historical draw patterns. Additionally, a budget-based recommendation module suggests suitable bond denominations according to user-defined investment limits.

---

## Key Features

### User Module

* Secure Registration and Authentication
* Prize Bond Registration and Management
* Bulk Bond Registration (Range-Based Entry)
* Historical Draw Results (Last 6 Years)
* Latest Draw Result Tracking
* Winning Bond Detection
* Push Notifications for Draws and Results
* AI-Based Winning Probability Prediction
* Budget-Based Bond Recommendations
* User Profile Management

### Administrator Module

* Secure Admin Authentication
* User Management
* Draw Result Management
* Historical Data Management
* Manual Result Upload and Verification
* System Monitoring and Maintenance

---

## Historical Draw Data Management

The application maintains a comprehensive repository of prize bond draw records covering the previous six years.

### Data Sources

**Automated Collection**

* Official National Savings Pakistan draw records
* Python-based web scraping module
* Automated data extraction and processing

**Manual Administration**

* Manual draw result uploads
* Record correction and validation
* Historical data maintenance

This hybrid approach ensures data accuracy, consistency, and reliability.

---

## Artificial Intelligence Module

### Random Forest Prediction Model

The system utilizes a Random Forest machine learning model to estimate the probability of winning for individual prize bonds.

### Input Parameters

* Historical draw records
* Bond denomination
* Draw frequency information
* Previous winning trends

### Output

* Winning probability score for each bond
* Probability ranking among registered bonds
* Historical trend insights

The AI module is designed to assist users in evaluating prize bond performance using historical data analysis.

---

## Recommendation Engine

The recommendation engine combines AI-generated probability scores with predefined mathematical formulas to generate budget-aware suggestions.

### Recommendation Workflow

1. Historical data analysis
2. Probability prediction using Random Forest
3. User budget evaluation
4. Bond filtering and ranking
5. Personalized recommendations

The recommendation module does not use additional machine learning models beyond Random Forest.

---

## System Architecture

### Android Application

* User Interface
* Bond Management
* Notification Services
* Probability Visualization
* Recommendation Dashboard

### Firebase Backend

* Firebase Authentication
* Cloud Firestore Database
* Firebase Cloud Messaging (FCM)

### Data Collection Layer

* National Savings Data Scraper
* Data Processing and Validation
* Historical Record Management

### AI Processing Layer

* Data Preprocessing
* Probability Prediction
* Recommendation Support

### Desktop Admin Panel

* User Administration
* Draw Result Management
* System Monitoring

---

## Technology Stack

### Mobile Development

* Java
* XML
* Android Studio

### Backend Services

* Firebase Authentication
* Firebase Firestore
* Firebase Cloud Messaging

### Artificial Intelligence

* Python
* Scikit-Learn
* Pandas
* NumPy
* Random Forest Classifier

### Data Collection

* Python
* BeautifulSoup
* Requests

### Desktop Application

* C#
* .NET
* Visual Studio

### Supporting Libraries

* Firebase SDKs
* iText PDF

---

## Functional Requirements

### User Functions

* Account Registration and Login
* Prize Bond Registration
* Bulk Bond Entry
* Historical Draw Search
* Draw Result Tracking
* Probability Analysis
* Recommendation Generation
* Notification Management
* Profile Management

### Administrative Functions

* User Administration
* Draw Result Management
* Historical Data Maintenance
* System Monitoring

---

## Non-Functional Requirements

### Security

* Secure Authentication
* Protected User Data
* Controlled Administrative Access

### Reliability

* Accurate Draw Result Processing
* Consistent Notification Delivery
* Reliable Historical Data Storage

### Performance

* Fast Data Retrieval
* Efficient AI Prediction Processing
* Responsive User Experience

### Availability

* Continuous System Accessibility
* Minimal Downtime

---

## Software Requirements

### Development Environment

* Windows 10/11
* Android Studio
* Visual Studio
* Python 3.x
* Firebase Console

### Python Dependencies

```bash id="gh9v2a"
pip install pandas numpy scikit-learn beautifulsoup4 requests
```

---

## Hardware Requirements

### Minimum Specifications

* 8 GB RAM
* 100 GB Storage
* Stable Internet Connection
* Android Device
* Desktop/Laptop Computer

---

## Future Enhancements

* Automated Scheduled Draw Synchronization
* Advanced Statistical Analytics
* PDF Report Generation
* Multi-Language Support
* Interactive Data Visualization
* Enhanced Prediction Models

---

## Project Team

### Supervisor

**Mr. Bilal Arshad**
Lecturer, Gujrat Institute of Management Sciences (GIMS)

### Team Members

* Muhammad Usman
* Sawaira Tanveer
* Zain Ul Hassan

---

## Institution

**Gujrat Institute of Management Sciences (GIMS)**
PMAS-Arid Agriculture University, Rawalpindi

---

## License

This project is developed as a Final Year Project (FYP) for academic and research purposes.
