package app.sakina.quran.quran

/**
 * Surah info cards (QR22). Themes are Sakina's own short summaries (S-ORIG, to be reviewed in the WA content queue);
 * virtues are limited to well-known authentic narrations with their reference.
 */
data class Virtue(val ar: String, val en: String, val refAr: String, val refEn: String)

object SuraInfo {
    /** Main themes, index = surah - 1: Arabic | English. */
    private val THEMES = listOf(
        "أم الكتاب: الحمد والتوحيد وطلب الهداية" to "The Mother of the Book: praise, tawhid and the prayer for guidance",
        "الإيمان والتقوى، قصة آدم وبني إسرائيل، وأحكام العبادات والمعاملات" to "Faith and taqwa, Adam and the Children of Israel, rulings on worship and dealings",
        "الثبات على الحق، حوار أهل الكتاب، ودروس غزوة أحد" to "Steadfastness on the truth, dialogue with the People of the Book, lessons of Uhud",
        "حقوق النساء واليتامى والمواريث وبناء الأسرة والمجتمع" to "Rights of women and orphans, inheritance, family and society",
        "الوفاء بالعقود، الحلال والحرام، والمائدة" to "Fulfilling covenants, the lawful and the unlawful, the Table Spread",
        "دلائل التوحيد وإبطال الشرك ومحاجة إبراهيم" to "Signs of tawhid, refuting shirk, Ibrahim's argument",
        "صراع الحق والباطل عبر قصص الأنبياء، وأصحاب الأعراف" to "Truth against falsehood through the prophets' stories, the people of the Heights",
        "غزوة بدر، النصر من عند الله، وأحكام الغنائم" to "The battle of Badr, victory is from Allah, rulings on spoils",
        "البراءة من المشركين، غزوة تبوك، وفضح المنافقين" to "Disavowal of the polytheists, Tabuk, exposing the hypocrites",
        "صدق الوحي، قصة يونس ونوح وموسى" to "Truth of the revelation, the stories of Yunus, Nuh and Musa",
        "الاستقامة وقصص الأنبياء مع أقوامهم" to "Uprightness and the prophets with their peoples",
        "أحسن القصص: يوسف عليه السلام والصبر والفرج" to "The best of stories: Yusuf, patience and relief",
        "آيات الله في الكون، والحق والباطل كالماء والزبد" to "Allah's signs in creation; truth and falsehood like water and foam",
        "إبراهيم يدعو لمكة، وشكر النعم، والكلمة الطيبة" to "Ibrahim's prayer for Makkah, gratitude, the good word",
        "حفظ الذكر، قصة إبليس، وضيف إبراهيم وقوم لوط" to "Preservation of the Reminder, Iblis, Ibrahim's guests and Lut's people",
        "النعم: سورة النعم وتعداد آلاء الله" to "Blessings: Allah's favours enumerated",
        "الإسراء، أخلاق المؤمن ووصايا القرآن" to "The Night Journey, the believer's ethics, the Quran's commandments",
        "الفتن الأربع: أصحاب الكهف وصاحب الجنتين وموسى والخضر وذو القرنين" to "Four trials: the Cave, the two gardens, Musa and al-Khidr, Dhul-Qarnayn",
        "رحمة الله بزكريا ومريم وعيسى وإبراهيم" to "Allah's mercy to Zakariyya, Maryam, 'Isa and Ibrahim",
        "قصة موسى مفصلة، والقرآن رحمة لا شقاء" to "Musa's story in detail; the Quran is mercy, not hardship",
        "اقتراب الحساب ودعوات الأنبياء المستجابة" to "Reckoning draws near; the answered prayers of the prophets",
        "مناسك الحج والتوحيد وأهوال الساعة" to "Rites of Hajj, tawhid and the terrors of the Hour",
        "صفات المؤمنين المفلحين ومراحل خلق الإنسان" to "Traits of the successful believers, stages of human creation",
        "العفاف والستر وآداب البيوت، وآية النور" to "Chastity, privacy and household manners; the Verse of Light",
        "الفرقان بين الحق والباطل وصفات عباد الرحمن" to "The Criterion; the traits of the servants of the Most Merciful",
        "قصص موسى وإبراهيم ونوح وهود وصالح ولوط وشعيب" to "Musa, Ibrahim, Nuh, Hud, Salih, Lut and Shu'ayb",
        "سليمان والنملة والهدهد وملكة سبأ" to "Sulayman, the ant, the hoopoe and the Queen of Sheba",
        "قصة موسى من مولده إلى مدين، وقارون" to "Musa from his birth to Madyan; Qarun",
        "الابتلاء سنة، وبيت العنكبوت" to "Trials are a law of life; the spider's house",
        "وعد الله بنصر الروم وآياته في الأنفس والآفاق" to "The promised Roman victory; signs in the self and the horizons",
        "وصايا لقمان لابنه والحكمة" to "Luqman's counsel to his son; wisdom",
        "الخلق والبعث وصفات المؤمنين القائمين بالليل" to "Creation, resurrection, those who rise at night",
        "غزوة الأحزاب، وبيت النبوة وآداب المؤمنين" to "The Confederates; the Prophet's household; manners",
        "شكر النعم: داود وسليمان وقوم سبأ" to "Gratitude: Dawud, Sulayman and the people of Saba",
        "فاطر السماوات، وفضل العلماء والتلاوة" to "The Originator; the merit of scholars and recitation",
        "قلب القرآن في التوحيد والبعث وأصحاب القرية" to "Tawhid and resurrection; the people of the town",
        "الملائكة الصافّات وقصص الأنبياء وفداء إسماعيل" to "The angels in ranks; the prophets; Isma'il's ransom",
        "داود وسليمان وأيوب والصبر والإنابة" to "Dawud, Sulayman, Ayyub; patience and turning back to Allah",
        "الإخلاص في العبادة وسعة رحمة الله" to "Sincere worship and the vastness of Allah's mercy",
        "مؤمن آل فرعون والدعاء المستجاب" to "The believer of Pharaoh's family; prayer is answered",
        "القرآن المفصّل وشهادة الجوارح والدفع بالتي هي أحسن" to "The detailed Quran, testimony of the limbs, repelling evil with good",
        "الشورى والوحي والرزق بقدر" to "Consultation, revelation, provision in due measure",
        "زينة الدنيا الزائلة وقصة إبراهيم وموسى" to "Passing glitter of this world; Ibrahim and Musa",
        "ليلة القدر المباركة وعاقبة فرعون" to "The blessed night; Pharaoh's end",
        "آيات الله والجاثية يوم القيامة" to "Allah's signs; every nation kneeling on the Day",
        "بر الوالدين وقصة عاد والجن المستمعين" to "Kindness to parents, 'Ad, the jinn who listened",
        "القتال في سبيل الله وصفات المنافقين" to "Striving in Allah's way; traits of the hypocrites",
        "صلح الحديبية الفتح المبين وبيعة الرضوان" to "Hudaybiyyah, the clear victory, the Pledge of Ridwan",
        "آداب المجتمع المسلم: الأدب مع النبي والتثبت والأخوة" to "Social manners: respect for the Prophet, verifying news, brotherhood",
        "البعث والحساب وقرب الله من عبده" to "Resurrection, reckoning, Allah's nearness to His servant",
        "الرزق في السماء، وضيف إبراهيم، والخلق للعبادة" to "Provision is in the heaven; Ibrahim's guests; created to worship",
        "عذاب الكافرين ونعيم المتقين" to "Punishment of the deniers, bliss of the righteous",
        "المعراج ورؤية جبريل وعدل الجزاء" to "The Ascension, seeing Jibril, just recompense",
        "انشقاق القمر، ولقد يسرنا القرآن للذكر" to "The splitting of the moon; the Quran made easy to remember",
        "عروس القرآن: فبأي آلاء ربكما تكذبان" to "Which of your Lord's favours will you deny?",
        "الواقعة وأصناف الناس الثلاثة يوم القيامة" to "The Event and the three groups on the Day",
        "الإنفاق والإيمان وحقيقة الدنيا" to "Spending, faith and the reality of this world",
        "المجادلة وآداب المجالس والتناجي" to "The pleading woman; manners of gatherings and private talk",
        "إجلاء بني النضير وأسماء الله الحسنى" to "Banu an-Nadir's exile; the Most Beautiful Names",
        "الولاء والبراء وأسوة إبراهيم" to "Loyalty and disavowal; Ibrahim the example",
        "الصف المتراص في سبيل الله، والتجارة الرابحة" to "Ranks in Allah's way; the profitable trade",
        "صلاة الجمعة وفضلها" to "The Friday prayer and its merit",
        "صفات المنافقين والتحذير من الانشغال بالمال" to "Traits of hypocrites; wealth must not distract",
        "التغابن يوم الحساب والصبر على المصائب" to "Mutual loss on the Day; patience in calamity",
        "أحكام الطلاق والعدة والتقوى مخرج" to "Divorce and waiting periods; taqwa is a way out",
        "بيت النبوة والتوبة النصوح" to "The Prophet's household; sincere repentance",
        "الملك بيد الله، والمانعة من عذاب القبر" to "Dominion is Allah's; protection in the grave",
        "خلق النبي العظيم وأصحاب الجنة" to "The Prophet's sublime character; the garden owners",
        "الحاقة وأهوال يوم القيامة" to "The Reality and the terrors of the Day",
        "الهلع والجزع وصفات المصلين" to "Human anxiety; the traits of those who pray",
        "دعوة نوح ثمانمئة وخمسين عامًا والاستغفار" to "Nuh's long call and seeking forgiveness",
        "الجن يستمعون القرآن ويؤمنون" to "The jinn listen to the Quran and believe",
        "قيام الليل والترتيل" to "Night prayer and measured recitation",
        "القيام بالدعوة والإنذار" to "Rise and warn",
        "يوم القيامة والنفس اللوامة" to "The Day of Resurrection; the self-reproaching soul",
        "خلق الإنسان ونعيم الأبرار" to "Creation of man and the bliss of the righteous",
        "المرسلات وويل يومئذ للمكذبين" to "Those sent forth; woe to the deniers",
        "النبأ العظيم ومشاهد البعث" to "The great news and scenes of resurrection",
        "النازعات وقصة موسى وفرعون" to "Those who pull out; Musa and Pharaoh",
        "عتاب النبي في ابن أم مكتوم ويوم يفر المرء" to "The Prophet and Ibn Umm Maktum; the day one flees",
        "تكوير الشمس وأهوال القيامة" to "The sun wrapped up; the Day's terrors",
        "انفطار السماء والملائكة الكاتبون" to "The sky cleft; the recording angels",
        "ويل للمطففين في الكيل والميزان" to "Woe to those who give short measure",
        "انشقاق السماء وتسليم الكتاب" to "The sky split; records handed over",
        "البروج وأصحاب الأخدود والثبات على الإيمان" to "The constellations; the people of the ditch",
        "الطارق وحفظ الله لكل نفس" to "The night star; every soul has a guardian",
        "سبح اسم ربك الأعلى والتزكية" to "Glorify your Lord; self-purification",
        "الغاشية ومشاهد الآخرة وآيات الخلق" to "The Overwhelming; the hereafter and signs in creation",
        "الفجر والليالي العشر والنفس المطمئنة" to "The dawn, the ten nights, the soul at peace",
        "البلد واقتحام العقبة" to "The city; climbing the steep path",
        "الشمس وتزكية النفس" to "The sun; purifying the soul",
        "الليل وأصناف الساعين" to "The night; the different strivings",
        "الضحى ورعاية الله لنبيه" to "The morning light; Allah's care for His Prophet",
        "الشرح: إن مع العسر يسرًا" to "The relief: with hardship comes ease",
        "التين وخلق الإنسان في أحسن تقويم" to "The fig; man created in the best form",
        "اقرأ: أول ما نزل من القرآن" to "Read: the first revelation",
        "ليلة القدر خير من ألف شهر" to "The Night of Decree is better than a thousand months",
        "البيّنة ودين القيمة" to "The clear proof; the upright religion",
        "الزلزلة ومثقال الذرة" to "The earthquake; an atom's weight",
        "العاديات وجحود الإنسان" to "The charging horses; man's ingratitude",
        "القارعة والموازين" to "The Striking Hour; the scales",
        "التكاثر والسؤال عن النعيم" to "Competition in increase; questioned about blessings",
        "العصر: الإيمان والعمل والتواصي بالحق والصبر" to "Time: faith, good deeds, truth and patience",
        "الهمزة والتحذير من الغيبة وجمع المال" to "The slanderer; hoarding wealth",
        "الفيل وحماية الله لبيته" to "The Elephant; Allah protects His House",
        "قريش ونعمة الأمن والرزق" to "Quraysh; the blessing of safety and provision",
        "الماعون والتحذير من الرياء ومنع الخير" to "Small kindnesses; showing off and withholding help",
        "الكوثر: نهر في الجنة للنبي" to "Al-Kawthar: a river in Paradise for the Prophet",
        "الكافرون والبراءة من الشرك" to "The disbelievers; disavowal of shirk",
        "النصر والفتح والاستغفار" to "Victory, the opening and seeking forgiveness",
        "المسد ومصير أبي لهب" to "The palm fibre; the end of Abu Lahab",
        "الإخلاص: توحيد الله الخالص" to "Sincerity: pure oneness of Allah",
        "الفلق: الاستعاذة من الشرور الظاهرة" to "Daybreak: refuge from outward evils",
        "الناس: الاستعاذة من الوسواس" to "Mankind: refuge from the whisperer",
    )

    fun themeAr(s: Int) = THEMES.getOrNull(s - 1)?.first ?: ""
    fun themeEn(s: Int) = THEMES.getOrNull(s - 1)?.second ?: ""
    val count: Int get() = THEMES.size

    val VIRTUES: Map<Int, List<Virtue>> = mapOf(
        1 to listOf(Virtue("أعظم سورة في القرآن، وهي السبع المثاني.", "The greatest surah in the Quran, the seven oft-repeated verses.", "صحيح البخاري ٤٤٧٤", "Sahih al-Bukhari 4474")),
        2 to listOf(
            Virtue("إن الشيطان ينفر من البيت الذي تُقرأ فيه سورة البقرة.", "Satan flees from the house in which al-Baqarah is recited.", "صحيح مسلم ٧٨٠", "Sahih Muslim 780"),
            Virtue("آية الكرسي أعظم آية في كتاب الله.", "Ayat al-Kursi is the greatest verse in the Book of Allah.", "صحيح مسلم ٨١٠", "Sahih Muslim 810"),
            Virtue("من قرأ الآيتين من آخر سورة البقرة في ليلة كفتاه.", "Whoever recites the last two verses of al-Baqarah at night, they suffice him.", "صحيح البخاري ٥٠٠٩", "Sahih al-Bukhari 5009"),
        ),
        3 to listOf(Virtue("اقرؤوا الزهراوين: البقرة وآل عمران، فإنهما تأتيان يوم القيامة تحاجّان عن أصحابهما.", "Recite the two bright ones, al-Baqarah and Al 'Imran: they will come on the Day of Resurrection pleading for their companions.", "صحيح مسلم ٨٠٤", "Sahih Muslim 804")),
        18 to listOf(Virtue("من حفظ عشر آيات من أول سورة الكهف عُصم من الدجال.", "Whoever memorises ten verses from the beginning of al-Kahf is protected from the Dajjal.", "صحيح مسلم ٨٠٩", "Sahih Muslim 809")),
        32 to listOf(Virtue("كان النبي ﷺ لا ينام حتى يقرأ «الم تنزيل» السجدة و«تبارك الذي بيده الملك».", "The Prophet ﷺ would not sleep until he recited as-Sajdah and al-Mulk.", "سنن الترمذي ٢٨٩٢", "Jami' at-Tirmidhi 2892")),
        48 to listOf(Virtue("«لقد أُنزلت عليّ الليلة سورة لهي أحب إليّ مما طلعت عليه الشمس».", "\"A surah was revealed to me tonight that is dearer to me than all on which the sun rises.\"", "صحيح البخاري ٤١٧٧", "Sahih al-Bukhari 4177")),
        67 to listOf(
            Virtue("سورة ثلاثون آية شفعت لرجل حتى غُفر له.", "A surah of thirty verses interceded for a man until he was forgiven.", "سنن الترمذي ٢٨٩١، سنن أبي داود ١٤٠٠", "Jami' at-Tirmidhi 2891, Sunan Abi Dawud 1400"),
            Virtue("كان النبي ﷺ لا ينام حتى يقرأ «تبارك الذي بيده الملك».", "The Prophet ﷺ would not sleep until he recited al-Mulk.", "سنن الترمذي ٢٨٩٢", "Jami' at-Tirmidhi 2892"),
        ),
        112 to listOf(Virtue("تعدل ثلث القرآن.", "It equals a third of the Quran.", "صحيح البخاري ٥٠١٣", "Sahih al-Bukhari 5013")),
        113 to listOf(Virtue("«ألم تر آيات أُنزلت الليلة لم يُرَ مثلهن قط: قل أعوذ برب الفلق، وقل أعوذ برب الناس».", "\"Verses were revealed tonight the like of which has never been seen: al-Falaq and an-Nas.\"", "صحيح مسلم ٨١٤", "Sahih Muslim 814")),
        114 to listOf(Virtue("«ألم تر آيات أُنزلت الليلة لم يُرَ مثلهن قط: قل أعوذ برب الفلق، وقل أعوذ برب الناس».", "\"Verses were revealed tonight the like of which has never been seen: al-Falaq and an-Nas.\"", "صحيح مسلم ٨١٤", "Sahih Muslim 814")),
    )
}

/** A curated topic (QR21) with ayah refs "s:a" (assets/quran/topics.json). */
data class Topic(val id: String, val ar: String, val en: String, val refs: List<Pair<Int, Int>>)

object Topics {
    fun parse(json: String): List<Topic> {
        val a = org.json.JSONArray(json)
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i); val r = o.getJSONArray("refs")
            Topic(o.getString("id"), o.getString("ar"), o.getString("en"), (0 until r.length()).map { j -> r.getString(j).split(':').let { it[0].toInt() to it[1].toInt() } })
        }
    }
    @Volatile private var cache: List<Topic>? = null
    fun load(ctx: android.content.Context): List<Topic> = cache ?: parse(ctx.assets.open("quran/topics.json").bufferedReader().readText()).also { cache = it }
}

/** mp3quran.net reciter catalogue (bundled snapshot; streamed audio). */
data class Moshaf(val id: Int, val ar: String, val en: String, val server: String, val count: Int, val surahs: Set<Int>) {
    fun has(s: Int) = surahs.isEmpty() || s in surahs
    fun url(s: Int) = server.trimEnd('/') + "/" + s.toString().padStart(3, '0') + ".mp3"
}
data class Reciter(val id: Int, val ar: String, val en: String, val moshafs: List<Moshaf>)

object Reciters {
    fun parse(json: String): List<Reciter> {
        val a = org.json.JSONObject(json).getJSONArray("reciters")
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i); val ms = o.getJSONArray("m")
            Reciter(o.getInt("id"), o.getString("ar"), o.optString("en"), (0 until ms.length()).map { j ->
                val m = ms.getJSONObject(j)
                val list = m.optString("l").split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                Moshaf(m.getInt("id"), m.getString("ar"), m.optString("en"), m.getString("s"), m.optInt("n"), list)
            })
        }
    }
    @Volatile private var cache: List<Reciter>? = null
    fun load(ctx: android.content.Context): List<Reciter> = cache ?: parse(ctx.assets.open("quran/reciters.json").bufferedReader().readText()).also { cache = it }

    /** Islamic Network per-ayah editions used for the ayah-by-ayah voice (stream only, S-IN). */
    val AYAH_VOICES = listOf(
        Triple("ar.alafasy", "مشاري العفاسي", "Mishary Alafasy"),
        Triple("ar.husary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
        Triple("ar.minshawi", "محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi"),
        Triple("ar.abdulbasitmurattal", "عبد الباسط عبد الصمد (مرتل)", "Abdul Basit (Murattal)"),
        Triple("ar.mahermuaiqly", "ماهر المعيقلي", "Maher Al-Muaiqly"),
        Triple("ar.shaatree", "أبو بكر الشاطري", "Abu Bakr Ash-Shaatree"),
        Triple("ar.hudhaify", "علي الحذيفي", "Ali Al-Hudhaify"),
        Triple("ar.ahmedajamy", "أحمد العجمي", "Ahmed Al-Ajamy"),
        Triple("ar.muhammadayyoub", "محمد أيوب", "Muhammad Ayyoub"),
    )
    /** Per-ayah MP3 (global ayah number 1..6236) on the Islamic Network CDN. */
    fun ayahUrl(edition: String, global0: Int) = "https://cdn.islamic.network/quran/audio/${if (edition == "ar.abdulbasitmurattal") 192 else 128}/$edition/${global0 + 1}.mp3"
}
