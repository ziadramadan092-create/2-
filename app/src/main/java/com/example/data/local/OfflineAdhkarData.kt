package com.example.data.local

import com.example.domain.model.DhikrCategory
import com.example.domain.model.DhikrItem
import com.example.domain.model.TasbihPhrase

object OfflineAdhkarData {

  val tasbihPhrases: List<TasbihPhrase> = listOf(
    TasbihPhrase("subhan_allah", "سُبْحَانَ اللَّهِ", "تُكتب لك ألف حسنة أو تُحط عنك ألف خطيئة"),
    TasbihPhrase("alhamdulillah", "الْحَمْدُ لِلَّهِ", "تملأ الميزان"),
    TasbihPhrase("allahu_akbar", "اللَّهُ أَكْبَرُ", "خيرٌ مما طلعت عليه الشمس"),
    TasbihPhrase("la_ilaha_illallah", "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "أفضل ما قلت أنا والنبيون من قبلي"),
    TasbihPhrase("subhan_bihamdihi", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "غُفرت خطاياه وإن كانت مثل زبد البحر"),
    TasbihPhrase("astaghfirullah", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ", "طوبى لمن وجد في صحيفته استغفاراً كثيراً"),
    TasbihPhrase("salawat", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "من صلى عليّ صلاةً صلى الله عليه بها عشراً"),
  )

  val adhkar: List<DhikrItem> = listOf(
    // أذكار الصباح
    DhikrItem(
      id = "m_ayat_kursi",
      category = DhikrCategory.MORNING,
      text = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ.",
      reference = "آية الكرسي - من قرأها حين يصبح أُجير من الجن حتى يمسي",
      targetCount = 1,
    ),
    DhikrItem(
      id = "m_muawwidhat",
      category = DhikrCategory.MORNING,
      text = "قراءة المعوذات (سورة الإخلاص، وسورة الفلق، وسورة الناس).",
      reference = "تكفيه من كل شيء (ثلاث مرات)",
      targetCount = 3,
    ),
    DhikrItem(
      id = "m_asbahna",
      category = DhikrCategory.MORNING,
      text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ.",
      reference = "رواه مسلم",
      targetCount = 1,
    ),
    DhikrItem(
      id = "m_sayyid_istighfar",
      category = DhikrCategory.MORNING,
      text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
      reference = "سيد الاستغفار - رواه البخاري",
      targetCount = 1,
    ),
    DhikrItem(
      id = "m_bismillah",
      category = DhikrCategory.MORNING,
      text = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
      reference = "لم يضره شيء (ثلاث مرات)",
      targetCount = 3,
    ),
    DhikrItem(
      id = "m_raditu",
      category = DhikrCategory.MORNING,
      text = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا.",
      reference = "حقاً على الله أن يرضيه يوم القيامة",
      targetCount = 3,
    ),
    DhikrItem(
      id = "m_hasbi",
      category = DhikrCategory.MORNING,
      text = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
      reference = "كفاه الله ما أهمه (سبع مرات)",
      targetCount = 7,
    ),
    DhikrItem(
      id = "m_subhan_bihamdihi",
      category = DhikrCategory.MORNING,
      text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
      reference = "حُطّت خطاياه ولو كانت مثل زبد البحر",
      targetCount = 100,
    ),

    // أذكار المساء
    DhikrItem(
      id = "e_ayat_kursi",
      category = DhikrCategory.EVENING,
      text = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ.",
      reference = "آية الكرسي - من قرأها حين يمسي أُجير من الجن حتى يصبح",
      targetCount = 1,
    ),
    DhikrItem(
      id = "e_muawwidhat",
      category = DhikrCategory.EVENING,
      text = "قراءة المعوذات (سورة الإخلاص، وسورة الفلق، وسورة الناس).",
      reference = "تكفيه من كل شيء (ثلاث مرات)",
      targetCount = 3,
    ),
    DhikrItem(
      id = "e_amsayna",
      category = DhikrCategory.EVENING,
      text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا.",
      reference = "رواه مسلم",
      targetCount = 1,
    ),
    DhikrItem(
      id = "e_kalimat",
      category = DhikrCategory.EVENING,
      text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
      reference = "حرز من كل ضرر في الليل (ثلاث مرات)",
      targetCount = 3,
    ),
    DhikrItem(
      id = "e_bismillah",
      category = DhikrCategory.EVENING,
      text = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
      reference = "لم يضره شيء (ثلاث مرات)",
      targetCount = 3,
    ),
    DhikrItem(
      id = "e_ya_hayyu",
      category = DhikrCategory.EVENING,
      text = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
      reference = "حديث صحيح",
      targetCount = 1,
    ),
    DhikrItem(
      id = "e_subhan_bihamdihi",
      category = DhikrCategory.EVENING,
      text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
      reference = "مائة مرة مساءً",
      targetCount = 100,
    ),

    // أذكار بعد الصلاة
    DhikrItem(
      id = "p_istighfar",
      category = DhikrCategory.AFTER_PRAYER,
      text = "أَسْتَغْفِرُ اللَّهَ (ثلاثاً)، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ.",
      reference = "رواه مسلم - مباشرة بعد التسليم",
      targetCount = 1,
    ),
    DhikrItem(
      id = "p_la_ilaha",
      category = DhikrCategory.AFTER_PRAYER,
      text = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ.",
      reference = "متفق عليه",
      targetCount = 1,
    ),
    DhikrItem(
      id = "p_subhan",
      category = DhikrCategory.AFTER_PRAYER,
      text = "سُبْحَانَ اللَّهِ.",
      reference = "ثلاثاً وثلاثين مرة",
      targetCount = 33,
    ),
    DhikrItem(
      id = "p_alhamdulillah",
      category = DhikrCategory.AFTER_PRAYER,
      text = "الْحَمْدُ لِلَّهِ.",
      reference = "ثلاثاً وثلاثين مرة",
      targetCount = 33,
    ),
    DhikrItem(
      id = "p_allahu_akbar",
      category = DhikrCategory.AFTER_PRAYER,
      text = "اللَّهُ أَكْبَرُ.",
      reference = "ثلاثاً وثلاثين مرة",
      targetCount = 33,
    ),
    DhikrItem(
      id = "p_khatm_100",
      category = DhikrCategory.AFTER_PRAYER,
      text = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
      reference = "تمام المائة - غُفرت خطاياه وإن كانت مثل زبد البحر",
      targetCount = 1,
    ),
    DhikrItem(
      id = "p_ayat_kursi",
      category = DhikrCategory.AFTER_PRAYER,
      text = "قراءة آية الكرسي بعد كل صلاة مكتوبة.",
      reference = "لم يمنعه من دخول الجنة إلا أن يموت",
      targetCount = 1,
    ),
  )

  fun forCategory(category: DhikrCategory): List<DhikrItem> =
    adhkar.filter { it.category == category }
}
