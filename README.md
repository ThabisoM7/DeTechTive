# DeTechtive

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.8.10-blue.svg)](https://kotlinlang.org)

**DeTechtive is a native Android application designed to be your first line of defense against SMS phishing (smishing) attacks and malicious URLs.**

---

<!-- Suggestion: Add a hero image or GIF of the app in action here -->

## Key Features

*   **Real-Time Link Analysis:** Manually submit any suspicious URL and get an instant, detailed threat analysis. The app leverages a powerful, aggregated API backend to check URLs against dozens of industry-leading security engines.
*   **Live Security News Feed:** Stay up-to-date with the latest cybersecurity news and threats. The app features a built-in RSS reader that pulls from multiple reputable sources, including *Dark Reading*, *Krebs on Security*, and *The Hacker News*.
*   **Comprehensive Reports:** Keep a history of all your analyzed links. Review past results to identify patterns or recurring threats.
*   **Customizable Settings:** Tailor the app's behavior to your needs, including notification preferences for different threat levels.

## Technical Deep Dive

DeTechtive is built using modern Android development practices with Kotlin.

*   **Architecture:** The app is built on a Single-Activity, multiple-Fragment architecture, providing a fluid and responsive user experience.
*   **UI:** The user interface is built with Material Components and ConstraintLayout for a clean, modern, and adaptable design. `RecyclerView` is used extensively for efficient display of lists in the Reports and News Feed sections.
*   **Link Analysis:** Link checking is performed by making network requests to a powerful third-party API. The results are parsed and presented to the user in a clear, color-coded dialog.
*   **RSS Feed Parser:** The news feed is powered by a custom-built XML parser using Android's native `XmlPullParser`. This approach is lightweight, efficient, and has no external library dependencies, ensuring robust and error-free performance. Articles from multiple feeds are fetched asynchronously using Kotlin Coroutines, sorted by publication date, and displayed to the user.
*   **Localization:** In-app language switching is implemented using `AppCompatDelegate.setApplicationLocales()` to dynamically update the application's locale at runtime, providing a seamless transition between supported languages.

## Future Roadmap

DeTechtive is an actively developed project. Future enhancements include:

*   **Proactive SMS Scanning:** Granting permission for the app to automatically scan incoming SMS messages for threats in the background.
*   **Dark Mode Support:** A sleek, eye-friendly dark theme that respects the system-wide user setting.
*   **Security Education Center:** A dedicated section with curated tips and guides on staying safe online.

## Getting Started

To build the project yourself:
1.  Clone the repository: `git clone https://github.com/ThabisoM7/DeTechTive.git`
2.  You will need an API key for the link analysis feature. Create a `local.properties` file in the root of the project.
3.  Add your API key to the `local.properties` file: `apiKey="YOUR_API_KEY_HERE"`
4.  Open the project in Android Studio and run the `app` module.

## Contributing

Contributions are welcome! If you'd like to help improve DeTechtive, please feel free to fork the repository, make your changes, and submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
