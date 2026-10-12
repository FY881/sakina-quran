package app.sakina.quran.ui.tabs

import app.sakina.quran.ui.icons.Ico

/** Explore sections, in display order. */
enum class Section(val ar: String, val en: String) {
    QURAN("القرآن الكريم", "The Quran"),
    PRAYER("الصلاة والعبادة", "Prayer & worship"),
    DHIKR("الأذكار والأدعية", "Dhikr & duas"),
    KNOWLEDGE("العلم والسيرة", "Knowledge & Seerah"),
    CALENDAR("رمضان والتقويم", "Ramadan & calendar"),
    TOOLS("أدوات وحاسبات", "Tools & calculators"),
    HAJJ("الحج والعمرة", "Hajj & Umrah"),
    FAMILY("الأسرة والمجتمع", "Family & community"),
    SMART("ذكي وخاص", "Smart & personal"),
    APP("التطبيق", "App"),
}

/**
 * A feature in the catalog. [route] null = not built yet: tapping opens the "coming in the next update"
 * sheet with [descAr]/[descEn]. [kw] = extra search keywords (both languages, lower case).
 */
data class Feature(
    val id: String, val ico: Ico, val ar: String, val en: String, val route: String? = null,
    val section: Section = Section.APP, val descAr: String = "", val descEn: String = "", val kw: String = "",
)

private fun f(id: String, ico: Ico, ar: String, en: String, route: String?, s: Section, dAr: String, dEn: String, kw: String = "") =
    Feature(id, ico, ar, en, route, s, dAr, dEn, kw)

val FEATURES: List<Feature> = listOf(
    // ── Quran
    f("quran", Ico.QURAN, "القرآن", "Quran", "quran", Section.QURAN, "المصحف كاملًا بالرسم العثماني من مشروع تنزيل، مع فهرس السور والبحث ومتابعة القراءة.", "The full Quran in Uthmani script (Tanzil), with surah index, search and continue reading.", "مصحف قران سورة آية quran surah ayah"),
    f("quran_search", Ico.QURAN, "البحث في القرآن", "Quran search", "quran_search", Section.QURAN, "ابحث في نص القرآن كله دون اعتبار للتشكيل.", "Search the whole Quran text, ignoring diacritics.", "بحث search find"),
    f("mushaf", Ico.BOOKMARK, "المصحف المصوّر", "Page mushaf", null, Section.QURAN, "عرض صفحات المصحف الستمائة وأربع كما في مصحف المدينة، مع التنقل بالجزء والحزب والصفحة.", "All 604 pages laid out like the Madinah mushaf, with juz, hizb and page navigation.", "صفحات madinah pages juz"),
    f("tafsir", Ico.HADITH, "التفسير", "Tafsir", null, Section.QURAN, "تفاسير موثوقة (الميسر، ابن كثير، السعدي) تحت كل آية، تعمل دون إنترنت بعد التنزيل.", "Trusted tafsir (Muyassar, Ibn Kathir, Sa'di) under every ayah, offline after download.", "تفسير معنى explanation"),
    f("translations", Ico.LANGUAGE, "الترجمات", "Translations", null, Section.QURAN, "ترجمات معاني القرآن بعدة لغات مع النقل الحرفي اللاتيني.", "Quran meaning translations in many languages plus transliteration.", "ترجمة translation transliteration"),
    f("reciters", Ico.RECITERS, "القرّاء والاستماع", "Reciters", null, Section.QURAN, "استمع لعشرات القرّاء مع تظليل الآية الجارية وتكرار المقاطع للحفظ.", "Listen to dozens of reciters with live ayah highlighting and repeat for memorisation.", "قارئ تلاوة صوت audio recitation listen"),
    f("khatma", Ico.KHATMA, "الختمة", "Khatma", null, Section.QURAN, "خطة ختمة ذكية تحسب وردك اليومي وتذكّرك بعد الصلوات، وختمة جماعية مع الأصدقاء.", "A smart khatma plan that computes your daily portion, reminds you after prayers and supports group khatmas.", "ختم ورد plan"),
    f("hifz", Ico.BRAIN, "الحفظ والمراجعة", "Hifz", null, Section.QURAN, "مساعد حفظ بالتكرار المتباعد واختبار التسميع بالصوت.", "Memorisation coach with spaced repetition and voice recitation tests.", "حفظ تسميع memorize memorise"),
    f("tajweed", Ico.SPARKLES, "التجويد", "Tajweed", null, Section.QURAN, "مصحف ملوّن بأحكام التجويد مع شرح مبسّط لكل حكم.", "Colour-coded tajweed mushaf with a simple explanation of each rule.", "تجويد احكام rules"),
    // ── Prayer & worship
    f("prayer", Ico.MOSQUE, "مواقيت الصلاة", "Prayer times", "prayers", Section.PRAYER, "مواقيت دقيقة لكل مدينة مع الأذان والتنبيهات.", "Accurate prayer times for every city with adhan and alerts.", "صلاة اذان مواقيت salah adhan times"),
    f("tracker", Ico.TRACKER, "متتبع الصلاة", "Prayer tracker", "prayers", Section.PRAYER, "سجّل صلواتك وتابع تقدمك الأسبوعي والشهري وسلسلة أيامك.", "Log your prayers and follow weekly/monthly progress and streaks.", "تتبع سجل log streak"),
    f("qibla", Ico.QIBLA, "القبلة", "Qibla", "qibla", Section.PRAYER, "بوصلة القبلة الدقيقة باتجاه الكعبة المشرفة.", "Accurate qibla compass towards the Kaaba.", "قبله بوصلة kaaba compass direction"),
    f("ar_qibla", Ico.KAABA, "القبلة بالكاميرا", "AR Qibla", null, Section.PRAYER, "وجّه الكاميرا فيظهر لك اتجاه الكعبة فوق الواقع مباشرة.", "Point your camera and see the Kaaba direction overlaid on the real world.", "كاميرا واقع معزز camera ar"),
    f("mosques", Ico.MAP, "المساجد القريبة", "Mosques", null, Section.PRAYER, "خريطة المساجد من حولك مع المسافة والاتجاهات وأوقات الإقامة.", "Map of mosques around you with distance, directions and iqama times.", "مسجد جامع mosque masjid near"),
    f("sunnah", Ico.SUNRISE, "السنن والنوافل", "Sunnah prayers", null, Section.PRAYER, "تتبع الرواتب والضحى والوتر وقيام الليل مع فضل كل منها.", "Track rawatib, Duha, Witr and Qiyam with the virtue of each.", "رواتب ضحى وتر قيام nawafil duha witr"),
    f("qada", Ico.HOURGLASS, "قضاء الفوائت", "Qada tracker", null, Section.PRAYER, "احسب الصلوات الفائتة وقسّمها على خطة يومية مريحة.", "Calculate missed prayers and spread them over a comfortable daily plan.", "قضاء فوائت missed makeup"),
    f("how_pray", Ico.WUDU, "كيف أصلي وأتوضأ", "How to pray", null, Section.PRAYER, "شرح مصوّر للوضوء والغسل والتيمم والصلاة خطوة بخطوة.", "Illustrated step-by-step wudu, ghusl, tayammum and prayer.", "وضوء غسل تيمم wudu ghusl"),
    f("istikhara", Ico.IDEA, "صلاة الاستخارة", "Istikhara", null, Section.PRAYER, "كيفية صلاة الاستخارة ودعاؤها كاملًا مع آدابها.", "How to pray Istikhara with its full dua and etiquette.", "استخاره قرار decision"),
    f("janazah", Ico.NIGHT, "صلاة الجنازة", "Janazah", null, Section.PRAYER, "خطوات صلاة الجنازة وأدعيتها وأحكام العزاء.", "Steps and duas of the funeral prayer and condolence etiquette.", "جنازه ميت funeral"),
    f("silent", Ico.BELL, "الوضع الصامت في المسجد", "Mosque silent mode", null, Section.PRAYER, "يصمت هاتفك تلقائيًا عند دخول المسجد أو وقت الصلاة ثم يعود كما كان.", "Silences your phone automatically at the mosque or prayer time, then restores it.", "صامت silent mute"),
    f("travel", Ico.LOCATION, "وضع السفر", "Travel mode", null, Section.PRAYER, "يكتشف سفرك ويعرض مواقيت المدينة الجديدة وأحكام القصر والجمع.", "Detects travel, shows the new city's times and the rules of shortening/combining.", "سفر قصر جمع travel qasr"),
    // ── Dhikr & duas
    f("azkar", Ico.AZKAR, "الأذكار", "Azkar", null, Section.DHIKR, "أذكار الصباح والمساء والنوم والصلاة بعدّاد وتذكير في وقتها.", "Morning, evening, sleep and after-prayer azkar with counters and timely reminders.", "اذكار صباح مساء morning evening"),
    f("hisn", Ico.SHAHADA, "حصن المسلم", "Hisn al-Muslim", null, Section.DHIKR, "كتاب حصن المسلم كاملًا مقسّمًا بالمواقف مع الصوت والمصادر.", "The complete Hisn al-Muslim by situation, with audio and sources.", "حصن fortress"),
    f("duas", Ico.DUA, "الأدعية", "Duas", null, Section.DHIKR, "أدعية من القرآن والسنة لكل حال، مع المفضلة والمشاركة.", "Duas from the Quran and Sunnah for every occasion, with favourites and sharing.", "دعاء ادعية supplication"),
    f("ruqyah", Ico.PRIVACY, "الرقية الشرعية", "Ruqyah", null, Section.DHIKR, "آيات وأدعية الرقية الشرعية من الكتاب والسنة مع التلاوة.", "Ruqyah verses and duas from the Quran and Sunnah with recitation.", "رقيه ruqya"),
    f("tasbih", Ico.TASBIH, "السبحة", "Tasbih", "tasbih", Section.DHIKR, "سبحة إلكترونية باهتزاز وأهداف وأذكار جاهزة وسجل يومي.", "Digital tasbih with haptics, targets, presets and daily history.", "سبحه تسبيح عداد counter dhikr"),
    f("names", Ico.NAMES, "أسماء الله الحسنى", "99 Names of Allah", "names", Section.DHIKR, "الأسماء الحسنى التسعة والتسعون بمعانيها، مع المفضلة.", "The 99 Beautiful Names with their meanings and favourites.", "اسماء الحسنى asma husna names"),
    f("names_quiz", Ico.TROPHY, "اختبار الأسماء الحسنى", "Names quiz", null, Section.DHIKR, "اختبر حفظك للأسماء الحسنى ومعانيها بأسئلة ممتعة.", "Test your knowledge of the Names and their meanings with fun questions.", "اختبار مسابقه quiz"),
    // ── Knowledge
    f("hadith", Ico.HADITH, "الأحاديث", "Hadith", null, Section.KNOWLEDGE, "الأربعون النووية ورياض الصالحين وصحيح البخاري ومسلم مع البحث.", "Nawawi's Forty, Riyad as-Salihin, Bukhari and Muslim with search.", "حديث سنة bukhari muslim"),
    f("seerah", Ico.STAR, "السيرة النبوية", "Seerah", null, Section.KNOWLEDGE, "خط زمني تفاعلي لسيرة النبي ﷺ من المولد إلى الوفاة.", "Interactive timeline of the Prophet's ﷺ life from birth to passing.", "سيره نبي prophet muhammad"),
    f("prophets", Ico.SEEDLING, "قصص الأنبياء", "Prophets' stories", null, Section.KNOWLEDGE, "قصص الأنبياء كما وردت في القرآن الكريم بأسلوب سهل.", "Stories of the prophets as told in the Quran, simply written.", "قصص انبياء stories"),
    f("videos", Ico.VIDEO, "مكتبة المرئيات", "Videos", null, Section.KNOWLEDGE, "دروس ومحاضرات وتلاوات مرئية مختارة بعناية.", "Carefully curated lessons, talks and recitation videos.", "فيديو دروس video lectures"),
    f("arabic", Ico.LANGUAGE, "عربية القرآن", "Quranic Arabic", null, Section.KNOWLEDGE, "دروس قصيرة لفهم مفردات القرآن الأكثر تكرارًا.", "Short lessons on the Quran's most frequent words.", "عربي مفردات words"),
    f("new_muslim", Ico.SEEDLING, "المسلم الجديد", "New Muslim", null, Section.KNOWLEDGE, "مسار تعليمي هادئ للمسلم الجديد: العقيدة والطهارة والصلاة خطوة بخطوة.", "A gentle learning path for new Muslims: belief, purification and prayer.", "مسلم جديد revert convert"),
    f("shahada", Ico.SHAHADA, "الشهادة", "Shahada", null, Section.KNOWLEDGE, "نطق الشهادتين ومعناهما مع الصوت.", "The declaration of faith, its meaning and audio.", "شهاده faith"),
    // ── Ramadan & calendar
    f("ramadan", Ico.RAMADAN, "رمضان", "Ramadan", null, Section.CALENDAR, "إمساكية رمضان وعداد الإفطار والسحور ومخطط العشر الأواخر.", "Ramadan imsakiya, iftar/suhoor countdowns and last-ten-nights planner.", "رمضان امساكيه افطار سحور iftar suhoor"),
    f("fasting", Ico.DATES, "تقويم الصيام", "Fasting calendar", null, Section.CALENDAR, "تذكير بالاثنين والخميس والأيام البيض وعرفة وعاشوراء وست شوال.", "Reminders for Monday/Thursday, white days, Arafah, Ashura and six of Shawwal.", "صيام صوم fasting sawm"),
    f("calendar", Ico.CALENDAR, "التقويم الهجري", "Hijri calendar", null, Section.CALENDAR, "التقويم الهجري والميلادي معًا مع المناسبات ومحوّل التاريخ.", "Hijri and Gregorian calendar with events and a date converter.", "هجري تقويم مناسبات hijri events"),
    // ── Tools
    f("zakat", Ico.ZAKAT, "حاسبة الزكاة", "Zakat calculator", null, Section.TOOLS, "احسب زكاة المال والذهب والفضة والأسهم بنصاب محدّث.", "Calculate zakat on cash, gold, silver and shares with an up-to-date nisab.", "زكاه نصاب zakat nisab"),
    f("inheritance", Ico.SCALE, "حاسبة المواريث", "Inheritance", null, Section.TOOLS, "حساب أنصبة الورثة وفق الفرائض مع شرح كل نصيب.", "Compute heirs' shares according to the fara'id with explanations.", "مواريث ارث فرائض inheritance"),
    f("halal", Ico.HALAL, "دليل الحلال", "Halal guide", null, Section.TOOLS, "أماكن الطعام الحلال من حولك ودليل المكونات المشكوك فيها.", "Halal places near you and a guide to doubtful ingredients.", "حلال مطاعم halal food"),
    f("names_baby", Ico.LABEL, "أسماء المواليد", "Islamic names", null, Section.TOOLS, "آلاف الأسماء العربية والإسلامية بمعانيها وأصولها.", "Thousands of Arabic and Islamic names with meanings and origins.", "اسماء مواليد baby names"),
    f("cards", Ico.GREETING, "بطاقات التهنئة", "Greeting cards", null, Section.TOOLS, "صمّم بطاقات رمضان والعيد والجمعة وشاركها بلمسة.", "Design Ramadan, Eid and Jumu'ah cards and share them in a tap.", "بطاقات تهنئه عيد cards eid"),
    f("widgets", Ico.TOOLS, "الودجات", "Widgets", null, Section.TOOLS, "ودجات الشاشة الرئيسية: الصلاة القادمة، آية اليوم، التسبيح.", "Home-screen widgets: next prayer, ayah of the day, tasbih.", "ودجت widget"),
    // ── Hajj
    f("hajj", Ico.KAABA, "دليل الحج والعمرة", "Hajj & Umrah guide", null, Section.HAJJ, "دليل خطوة بخطوة لمناسك الحج والعمرة بأدعيتها ومواقعها.", "Step-by-step Hajj and Umrah rites with duas and locations.", "حج عمره مناسك hajj umrah"),
    f("makkah_live", Ico.LIVE, "مكة مباشر", "Makkah live", null, Section.HAJJ, "بث مباشر من المسجد الحرام والمسجد النبوي.", "Live stream from Masjid al-Haram and Masjid an-Nabawi.", "بث مباشر live stream"),
    f("hajj_journey", Ico.MAP, "رحلة الحج", "Hajj journey", null, Section.HAJJ, "خرائط المشاعر دون إنترنت وتتبّع مجموعتك أثناء الحج.", "Offline maps of the holy sites and group tracking during Hajj.", "رحله مشاعر منى عرفات journey"),
    // ── Family & community
    f("inspiration", Ico.INSPIRATION, "إلهام", "Inspiration", "community", Section.FAMILY, "بطاقات مختارة من الآيات والأحاديث والأدعية تشاركها مع من تحب.", "Curated ayat, hadith and duas to share with the people you love.", "الهام inspiration"),
    f("community", Ico.COMMUNITY, "مجتمع الأمة", "Ummah community", null, Section.FAMILY, "مجتمع آمن للمشاركة والدعاء لبعضنا مع إشراف ذكي.", "A safe community to share and pray for each other, with smart moderation.", "مجتمع community"),
    f("kids", Ico.KIDS, "ركن الأطفال", "Kids corner", null, Section.FAMILY, "قصار السور والأذكار والقصص للأطفال بأسلوب مرح.", "Short surahs, azkar and stories for children in a playful way.", "اطفال kids children"),
    f("family", Ico.FAMILY, "وضع العائلة", "Family mode", null, Section.FAMILY, "تابع صلوات أطفالك وشجّعهم بالنجوم والجوائز.", "Follow your children's prayers and encourage them with stars and rewards.", "عائله family"),
    f("journal", Ico.JOURNAL, "يومياتي", "Journal", null, Section.FAMILY, "دفتر خاص لتأملاتك ودعواتك ومشاعرك، محفوظ على جهازك فقط.", "A private journal for reflections, duas and feelings, kept on your device.", "يوميات مذكرات journal diary"),
    // ── Smart
    f("ask_ai", Ico.ROBOT, "اسأل سكينة", "Ask AI", null, Section.SMART, "إجابات بمصادرها من القرآن والسنة (ليست فتوى) بالذكاء الاصطناعي.", "AI answers with Quran and hadith sources (not a fatwa).", "ذكاء اصطناعي سؤال ai ask"),
    f("deen", Ico.DEEN, "وضع الدين", "Deen mode", null, Section.SMART, "خطة يومية مخصصة: وردك وأذكارك وصيامك في بطاقة واحدة.", "A personalised daily plan: your wird, azkar and fasting in one card.", "خطه يوميه plan"),
    f("notifications", Ico.BELL, "التنبيهات الذكية", "Smart notifications", null, Section.SMART, "تذكيرات تتعلم عاداتك: الكهف يوم الجمعة، الصيام، الختمة، دون إزعاج.", "Reminders that learn your habits: Kahf on Friday, fasting, khatma — never spammy.", "تنبيهات اشعارات notifications"),
    // ── App
    f("profile", Ico.PROFILE, "ملفي", "Profile", "profile", Section.APP, "اسمك وإحصاءاتك وسلسلة أيامك.", "Your name, stats and streaks.", "ملف حساب profile account"),
    f("themes", Ico.THEME, "الثيمات", "Themes", "themes", Section.APP, "أحد عشر ثيمًا مع الزجاج السائل والوضع الليلي.", "Eleven themes with liquid glass and dark mode.", "ثيم الوان مظهر theme colors"),
    f("iconpacks", Ico.ICONPACK, "حزم الأيقونات", "Icon packs", "iconpacks", Section.APP, "اختر شكل الأيقونات: ثلاثي الأبعاد أو ملوّن أو مسطّح.", "Choose icon style: 3D, colour or flat.", "ايقونات icons"),
    f("prayer_settings", Ico.ADHAN, "إعدادات الصلاة", "Prayer settings", "settings", Section.APP, "طريقة الحساب والمذهب وأصوات الأذان والتعديلات.", "Calculation method, madhab, adhan voices and adjustments.", "اعدادات طريقه حساب method"),
    f("settings", Ico.SETTINGS, "الإعدادات", "Settings", "more", Section.APP, "اللغة والمظهر وعن التطبيق والمصادر.", "Language, appearance, about and sources.", "اعدادات لغه settings language"),
    f("backup", Ico.BACKUP, "النسخ الاحتياطي", "Backup", null, Section.APP, "احفظ بياناتك في ملف أو زامنها مع حسابك واستعدها على أي جهاز.", "Save your data to a file or sync it with your account and restore anywhere.", "نسخ احتياطي مزامنه backup sync"),
    f("help", Ico.HELP, "المساعدة", "Help", null, Section.APP, "إجابات الأسئلة الشائعة وإصلاح مشكلات الأذان وتواصل معنا.", "FAQs, adhan troubleshooting and contact.", "مساعده help faq"),
)

fun feature(id: String): Feature? = FEATURES.firstOrNull { it.id == id }

/** Arabic/English-insensitive search key: strips tashkeel, unifies alef/ya/ta-marbuta forms, lower-cases. */
fun normalize(s: String): String = buildString(s.length) {
    s.lowercase().forEach { c ->
        when (c) {
            in '\u064B'..'\u065F', '\u0670', '\u0640', in '\u06D6'..'\u06ED' -> {}
            'أ', 'إ', 'آ', 'ٱ' -> append('ا')
            'ى' -> append('ي')
            'ة' -> append('ه')
            'ؤ' -> append('و')
            'ئ' -> append('ي')
            else -> append(c)
        }
    }
}

fun Feature.matches(q: String): Boolean {
    val n = normalize(q.trim())
    if (n.isEmpty()) return true
    val hay = normalize("$ar $en $kw $descAr $descEn")
    return n.split(' ').filter { it.isNotBlank() }.all { it in hay }
}
