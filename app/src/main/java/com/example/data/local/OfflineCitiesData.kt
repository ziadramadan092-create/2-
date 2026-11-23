package com.example.data.local

import com.example.domain.model.CalculationMethod
import com.example.domain.model.CityInfo
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object OfflineCitiesData {

  val cities: List<CityInfo> = listOf(
    // Egypt
    CityInfo("cairo", "القاهرة", "Cairo", "مصر", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("giza", "الجيزة", "Giza", "مصر", 30.0131, 31.2089, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("alexandria", "الإسكندرية", "Alexandria", "مصر", 31.2001, 29.9187, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("mansoura", "المنصورة", "Mansoura", "مصر", 31.0409, 31.3785, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("tanta", "طنطا", "Tanta", "مصر", 30.7865, 31.0004, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("asyut", "أسيوط", "Asyut", "مصر", 27.1783, 31.1859, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("luxor", "الأقصر", "Luxor", "مصر", 25.6872, 32.6396, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("aswan", "أسوان", "Aswan", "مصر", 24.0889, 32.8998, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("ismailia", "الإسماعيلية", "Ismailia", "مصر", 30.5965, 32.2715, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("portsaid", "بورسعيد", "Port Said", "مصر", 31.2653, 32.3019, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("suez", "السويس", "Suez", "مصر", 29.9668, 32.5498, "Africa/Cairo", CalculationMethod.EGYPTIAN),
    CityInfo("hurghada", "الغردقة", "Hurghada", "مصر", 27.2579, 33.8116, "Africa/Cairo", CalculationMethod.EGYPTIAN),

    // Saudi Arabia
    CityInfo("makkah", "مكة المكرمة", "Makkah", "السعودية", 21.3891, 39.8579, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("madinah", "المدينة المنورة", "Madinah", "السعودية", 24.5247, 39.5692, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("riyadh", "الرياض", "Riyadh", "السعودية", 24.7136, 46.6753, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("jeddah", "جدة", "Jeddah", "السعودية", 21.4858, 39.1925, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("dammam", "الدمام", "Dammam", "السعودية", 26.4207, 50.0888, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("khobar", "الخبر", "Khobar", "السعودية", 26.2172, 50.1971, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("taif", "الطائف", "Taif", "السعودية", 21.2854, 40.4262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("abha", "أبها", "Abha", "السعودية", 18.2164, 42.5053, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("tabuk", "تبوك", "Tabuk", "السعودية", 28.3835, 36.5662, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),
    CityInfo("buraidah", "بريدة", "Buraidah", "السعودية", 26.3260, 43.9750, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA),

    // Gulf & Levant
    CityInfo("dubai", "دبي", "Dubai", "الإمارات", 25.2048, 55.2708, "Asia/Dubai", CalculationMethod.DUBAI),
    CityInfo("abudhabi", "أبوظبي", "Abu Dhabi", "الإمارات", 24.4539, 54.3773, "Asia/Dubai", CalculationMethod.DUBAI),
    CityInfo("sharjah", "الشارقة", "Sharjah", "الإمارات", 25.3463, 55.4209, "Asia/Dubai", CalculationMethod.DUBAI),
    CityInfo("doha", "الدوحة", "Doha", "قطر", 25.2854, 51.5310, "Asia/Qatar", CalculationMethod.QATAR),
    CityInfo("kuwait", "الكويت", "Kuwait City", "الكويت", 29.3759, 47.9774, "Asia/Kuwait", CalculationMethod.KUWAIT),
    CityInfo("manama", "المنامة", "Manama", "البحرين", 26.2285, 50.5860, "Asia/Bahrain", CalculationMethod.UMM_AL_QURA),
    CityInfo("muscat", "مسقط", "Muscat", "عُمان", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.UMM_AL_QURA),
    CityInfo("amman", "عمّان", "Amman", "الأردن", 31.9454, 35.9284, "Asia/Amman", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("jerusalem", "القدس", "Jerusalem", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("gaza", "غزة", "Gaza", "فلسطين", 31.5017, 34.4668, "Asia/Gaza", CalculationMethod.EGYPTIAN),
    CityInfo("ramallah", "رام الله", "Ramallah", "فلسطين", 31.9038, 35.2034, "Asia/Hebron", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("beirut", "بيروت", "Beirut", "لبنان", 33.8938, 35.5018, "Asia/Beirut", CalculationMethod.EGYPTIAN),
    CityInfo("damascus", "دمشق", "Damascus", "سوريا", 33.5138, 36.2765, "Asia/Damascus", CalculationMethod.EGYPTIAN),
    CityInfo("aleppo", "حلب", "Aleppo", "سوريا", 36.2021, 37.1343, "Asia/Damascus", CalculationMethod.EGYPTIAN),
    CityInfo("baghdad", "بغداد", "Baghdad", "العراق", 33.3152, 44.3661, "Asia/Baghdad", CalculationMethod.EGYPTIAN),
    CityInfo("basra", "البصرة", "Basra", "العراق", 30.5085, 47.7804, "Asia/Baghdad", CalculationMethod.EGYPTIAN),
    CityInfo("erbil", "أربيل", "Erbil", "العراق", 36.1911, 44.0092, "Asia/Baghdad", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("sanaa", "صنعاء", "Sanaa", "اليمن", 15.3694, 44.1910, "Asia/Aden", CalculationMethod.UMM_AL_QURA),
    CityInfo("aden", "عدن", "Aden", "اليمن", 12.7855, 45.0187, "Asia/Aden", CalculationMethod.UMM_AL_QURA),

    // North & East Africa
    CityInfo("khartoum", "الخرطوم", "Khartoum", "السودان", 15.5007, 32.5599, "Africa/Khartoum", CalculationMethod.EGYPTIAN),
    CityInfo("tripoli", "طرابلس", "Tripoli", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli", CalculationMethod.EGYPTIAN),
    CityInfo("benghazi", "بنغازي", "Benghazi", "ليبيا", 32.1194, 20.0868, "Africa/Tripoli", CalculationMethod.EGYPTIAN),
    CityInfo("tunis", "تونس", "Tunis", "تونس", 36.8065, 10.1815, "Africa/Tunis", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("sfax", "صفاقس", "Sfax", "تونس", 34.7406, 10.7603, "Africa/Tunis", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("algiers", "الجزائر", "Algiers", "الجزائر", 36.7538, 3.0588, "Africa/Algiers", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("oran", "وهران", "Oran", "الجزائر", 35.6971, -0.6308, "Africa/Algiers", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("constantine", "قسنطينة", "Constantine", "الجزائر", 36.3650, 6.6147, "Africa/Algiers", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("casablanca", "الدار البيضاء", "Casablanca", "المغرب", 33.5731, -7.5898, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("rabat", "الرباط", "Rabat", "المغرب", 34.0209, -6.8416, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("marrakesh", "مراكش", "Marrakesh", "المغرب", 31.6295, -7.9811, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("fez", "فاس", "Fez", "المغرب", 34.0181, -5.0078, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("tangier", "طنجة", "Tangier", "المغرب", 35.7595, -5.8340, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("nouakchott", "نواكشوط", "Nouakchott", "موريتانيا", 18.0735, -15.9582, "Africa/Nouakchott", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("mogadishu", "مقديشو", "Mogadishu", "الصومال", 2.0469, 45.3182, "Africa/Mogadishu", CalculationMethod.MUSLIM_WORLD_LEAGUE),

    // Islamic & World Cities
    CityInfo("istanbul", "إسطنبول", "Istanbul", "تركيا", 41.0082, 28.9784, "Europe/Istanbul", CalculationMethod.TURKEY),
    CityInfo("ankara", "أنقرة", "Ankara", "تركيا", 39.9334, 32.8597, "Europe/Istanbul", CalculationMethod.TURKEY),
    CityInfo("kuala_lumpur", "كوالالمبور", "Kuala Lumpur", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalculationMethod.SINGAPORE),
    CityInfo("jakarta", "جاكرتا", "Jakarta", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.SINGAPORE),
    CityInfo("karachi", "كراتشي", "Karachi", "باكستان", 24.8607, 67.0011, "Asia/Karachi", CalculationMethod.KARACHI),
    CityInfo("lahore", "لاهور", "Lahore", "باكستان", 31.5204, 74.3587, "Asia/Karachi", CalculationMethod.KARACHI),
    CityInfo("islamabad", "إسلام أباد", "Islamabad", "باكستان", 33.6844, 73.0479, "Asia/Karachi", CalculationMethod.KARACHI),
    CityInfo("dhaka", "دكا", "Dhaka", "بنغلاديش", 23.8103, 90.4125, "Asia/Dhaka", CalculationMethod.KARACHI),
    CityInfo("london", "لندن", "London", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("paris", "باريس", "Paris", "فرنسا", 48.8566, 2.3522, "Europe/Paris", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("berlin", "برلين", "Berlin", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("madrid", "مدريد", "Madrid", "إسبانيا", 40.4168, -3.7038, "Europe/Madrid", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("rome", "روما", "Rome", "إيطاليا", 41.9028, 12.4964, "Europe/Rome", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("new_york", "نيويورك", "New York", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA),
    CityInfo("chicago", "شيكاغو", "Chicago", "الولايات المتحدة", 41.8781, -87.6298, "America/Chicago", CalculationMethod.ISNA),
    CityInfo("los_angeles", "لوس أنجلوس", "Los Angeles", "الولايات المتحدة", 34.0522, -118.2437, "America/Los_Angeles", CalculationMethod.ISNA),
    CityInfo("toronto", "تورونتو", "Toronto", "كندا", 43.6532, -79.3832, "America/Toronto", CalculationMethod.ISNA),
    CityInfo("sydney", "سيدني", "Sydney", "أستراليا", -33.8688, 151.2093, "Australia/Sydney", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    CityInfo("tokyo", "طوكيو", "Tokyo", "اليابان", 35.6762, 139.6503, "Asia/Tokyo", CalculationMethod.MUSLIM_WORLD_LEAGUE),
  )

  fun search(query: String): List<CityInfo> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return cities
    return cities.filter {
      it.nameAr.contains(q) ||
        it.countryAr.contains(q) ||
        it.nameEn.lowercase().contains(q)
    }
  }

  fun findNearestCity(latitude: Double, longitude: Double): Pair<CityInfo, Double> {
    var bestCity = cities.first()
    var bestDist = Double.MAX_VALUE
    for (city in cities) {
      val d = haversineKm(latitude, longitude, city.latitude, city.longitude)
      if (d < bestDist) {
        bestDist = d
        bestCity = city
      }
    }
    return bestCity to bestDist
  }

  private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
      sin(dLon / 2) * sin(dLon / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
  }
}
