package com.example.util

/** Supported country + dialing/phone metadata, shared across signup/profile forms. */
data class CountryInfo(val name: String, val dialCode: String, val phoneLen: Int)

val COUNTRIES: List<CountryInfo> = listOf(
    CountryInfo("India", "+91", 10),
    CountryInfo("United States", "+1", 10),
    CountryInfo("United Kingdom", "+44", 10),
    CountryInfo("Canada", "+1", 10),
    CountryInfo("Australia", "+61", 9),
    CountryInfo("Germany", "+49", 11),
    CountryInfo("France", "+33", 9),
    CountryInfo("Japan", "+81", 10),
    CountryInfo("Brazil", "+55", 11),
    CountryInfo("South Korea", "+82", 10),
    CountryInfo("Nigeria", "+234", 10),
    CountryInfo("South Africa", "+27", 9),
    CountryInfo("Mexico", "+52", 10),
    CountryInfo("Indonesia", "+62", 11),
    CountryInfo("Pakistan", "+92", 10),
    CountryInfo("Bangladesh", "+880", 10),
)

val COUNTRY_BY_NAME: Map<String, CountryInfo> = COUNTRIES.associateBy { it.name }

/**
 * Major cities → country. Typing/selecting a city auto-fills its country (mirrors the
 * website's city-first profile flow). Covers the supported countries; unknown cities fall
 * back to manual country selection.
 */
val CITY_TO_COUNTRY: List<Pair<String, String>> = listOf(
    // India
    "Mumbai" to "India", "Delhi" to "India", "New Delhi" to "India", "Bengaluru" to "India",
    "Bangalore" to "India", "Hyderabad" to "India", "Chennai" to "India", "Kolkata" to "India",
    "Pune" to "India", "Ahmedabad" to "India", "Jaipur" to "India", "Lucknow" to "India",
    "Chandigarh" to "India", "Kochi" to "India", "Goa" to "India", "Surat" to "India",
    "Indore" to "India", "Nagpur" to "India", "Bhopal" to "India", "Patna" to "India",
    "Ludhiana" to "India", "Guwahati" to "India",
    // United States
    "New York" to "United States", "Los Angeles" to "United States", "Chicago" to "United States",
    "Houston" to "United States", "Phoenix" to "United States", "San Francisco" to "United States",
    "Seattle" to "United States", "Boston" to "United States", "Austin" to "United States",
    "Miami" to "United States", "Atlanta" to "United States", "Dallas" to "United States",
    "Nashville" to "United States", "Las Vegas" to "United States", "Denver" to "United States",
    // United Kingdom
    "London" to "United Kingdom", "Manchester" to "United Kingdom", "Birmingham" to "United Kingdom",
    "Liverpool" to "United Kingdom", "Glasgow" to "United Kingdom", "Edinburgh" to "United Kingdom",
    "Leeds" to "United Kingdom", "Bristol" to "United Kingdom",
    // Canada
    "Toronto" to "Canada", "Vancouver" to "Canada", "Montreal" to "Canada", "Calgary" to "Canada",
    "Ottawa" to "Canada", "Edmonton" to "Canada",
    // Australia
    "Sydney" to "Australia", "Melbourne" to "Australia", "Brisbane" to "Australia",
    "Perth" to "Australia", "Adelaide" to "Australia",
    // Germany
    "Berlin" to "Germany", "Munich" to "Germany", "Hamburg" to "Germany", "Frankfurt" to "Germany",
    "Cologne" to "Germany",
    // France
    "Paris" to "France", "Marseille" to "France", "Lyon" to "France", "Nice" to "France",
    // Japan
    "Tokyo" to "Japan", "Osaka" to "Japan", "Kyoto" to "Japan", "Yokohama" to "Japan",
    // Brazil
    "São Paulo" to "Brazil", "Sao Paulo" to "Brazil", "Rio de Janeiro" to "Brazil",
    "Brasília" to "Brazil", "Brasilia" to "Brazil",
    // South Korea
    "Seoul" to "South Korea", "Busan" to "South Korea", "Incheon" to "South Korea",
    // Nigeria
    "Lagos" to "Nigeria", "Abuja" to "Nigeria", "Kano" to "Nigeria",
    // South Africa
    "Johannesburg" to "South Africa", "Cape Town" to "South Africa", "Durban" to "South Africa",
    "Pretoria" to "South Africa",
    // Mexico
    "Mexico City" to "Mexico", "Guadalajara" to "Mexico", "Monterrey" to "Mexico",
    // Indonesia
    "Jakarta" to "Indonesia", "Surabaya" to "Indonesia", "Bandung" to "Indonesia", "Bali" to "Indonesia",
    // Pakistan
    "Karachi" to "Pakistan", "Lahore" to "Pakistan", "Islamabad" to "Pakistan", "Rawalpindi" to "Pakistan",
    // Bangladesh
    "Dhaka" to "Bangladesh", "Chittagong" to "Bangladesh",
)

/** City suggestions (city → country) matching the query substring, capped. */
fun citySuggestions(query: String, limit: Int = 8): List<Pair<String, String>> {
    val q = query.trim()
    if (q.isBlank()) return emptyList()
    return CITY_TO_COUNTRY.filter { it.first.contains(q, ignoreCase = true) }.take(limit)
}

/** Resolve a city name to its country (exact, case-insensitive), or null if unknown. */
fun countryForCity(city: String): CountryInfo? {
    val match = CITY_TO_COUNTRY.firstOrNull { it.first.equals(city.trim(), ignoreCase = true) } ?: return null
    return COUNTRY_BY_NAME[match.second]
}
