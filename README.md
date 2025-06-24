# H2O Store

> **Development Status**: This application is currently in early development (approximately 70% complete). User functionalities are mostly implemented, but admin and delivery personnel features are still under development.

## Overview
H2O Store is a modern Android application designed for small to medium-scale businesses that addresses critical inventory management challenges. Built with the latest Google technologies like Jetpack Compose, this application integrates advanced AI solutions to optimize inventory levels, preventing both overstocking and stockouts.

## Key Features

### Inventory Optimization
- **AI-Powered Predictions**: Machine learning algorithms analyze historical data to predict optimal stock levels
- **Overstocking Prevention**: Reduces operational costs associated with excess inventory
- **Stockout Mitigation**: Ensures product availability to maintain customer satisfaction

### Multi-Role System
The application supports three distinct user roles:
1. **Customer**
   - Account creation and authentication
   - Location-based services with Google Maps integration
   - Product browsing and ordering
   - Order tracking

2. **Administrator**
   - Order management and approval
   - Delivery assignment
   - Analytics dashboard
   - Inventory prediction reports
   - Stock level recommendations

3. **Delivery Personnel**
   - Order assignment notifications
   - Delivery route optimization
   - Delivery status updates

### Technical Highlights
- **Modern UI**: Built with Jetpack Compose for a reactive and maintainable UI
- **Location Services**: Google Maps API integration for address selection and geocoding
- **Real-time Database**: Firebase NoSQL backend for seamless data synchronization
- **Authentication**: Secure user management with Firebase Authentication

## Installation Requirements

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Minimum SDK 23 (Android 6.0)
- Target SDK 35
- Kotlin 1.9.0 or newer
- Google Maps API key
- Firebase project configuration

### Configuration
1. **Google Maps API**:
   - Obtain a Google Maps API key from the [Google Cloud Console](https://console.cloud.google.com/)
   - Replace the placeholder in `LocationViewModel.kt` with your API key

2. **Firebase Setup**:
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add your application to the project
   - Download the `google-services.json` file
   - Place it in the app directory
   - Enable Authentication and Firestore in your Firebase project

### Build Instructions
1. Clone this repository
2. Add your Google Maps API key and Firebase configuration as described above
3. Open the project in Android Studio
4. Sync Gradle files
5. Build and run the application

## Architecture
The application follows MVVM (Model-View-ViewModel) architecture with clean architecture principles:
- **UI Layer**: Jetpack Compose UI components and screens
- **Domain Layer**: Business logic and use cases
- **Data Layer**: Repositories and data sources
- **Remote**: Firebase and API integrations

## Technology Stack
- **Jetpack Compose**: For modern, declarative UI
- **Kotlin Coroutines & Flow**: For asynchronous operations
- **Firebase Firestore**: NoSQL database
- **Firebase Authentication**: User management
- **Google Maps API**: Location services
- **Hilt**: Dependency injection
- **Machine Learning**: Cloud-based ML algorithms for inventory predictions

## Note for Developers
This is a private repository that requires specific API keys and configurations to run properly. Make sure to set up your own:
- Google Maps API key
- Firebase configuration (`google-services.json`)

Without these configurations, the application will not function correctly.

## Current Development Status
- **Completion**: Approximately 20% of planned functionality
- **Implemented Features**: 
  - User authentication (signup/login)
  - Google Maps integration for address management
  - Basic product browsing
  - Order placement
- **In Progress**:
  - Admin dashboard
  - Inventory prediction algorithms
  - Delivery management system
  - Analytics and reporting

## Roadmap
1. Implement admin interface and inventory management
2. Develop delivery personnel functionality
3. Deploy machine learning models for inventory prediction
4. Beta testing and performance optimization

## License
This project is proprietary software. Unauthorized reproduction or distribution is prohibited.



## Contact
For any inquiries, please contact -> abdelrahman9079@gmail.com .
