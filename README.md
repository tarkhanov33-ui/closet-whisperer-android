# Closet Whisperer — Android Application

Android client application for the **Closet Whisperer** wardrobe suggestions and management system.

## Features
- **Authentication Flow:** User registration and login screens integrated with the Spring Boot backend REST API.
- **Firebase Auth Google Sign-in Skeleton:** Boilerplate login support for Firebase Authentication and Google Play Services.
- **Dashboard:** Welcoming screen displaying current weather, outfit ratings (Outfit Hero), clothing metrics, and lately worn items.
- **Closet:** Full grid view of items in the closet with category filters and search options.
- **Add Item:** Convenient form to add items to the closet including auto-tagging options.
- **Outfits:** Daily clothing outfits suggestions compiled based on live weather temperature.
- **Profile:** Styling tabs to edit user credentials and visual persona characteristics.

## Architecture
This project is built using Hilt dependency injection, Coroutines, Flow, Jetpack components, and Material Design elements based on the MVVM architecture patterns.

- `:app` - UI pages (LoginActivity, MainActivity, Fragments, Adapters, Views).
- `:core-model` - Shared domain objects.
- `:core-network` - Retrofit REST service implementations (Spring Boot client and Weather client).
- `:core-database` - Room database setup.
- `:core-data` - Repository caching and data loading operations.
