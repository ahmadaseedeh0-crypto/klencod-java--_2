package com.example.quran; // تأكد من مطابقة اسم الـ package لمشروعك

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    // قائمة بأسماء سور القرآن الكريم الـ 114 كاملة
    private final String[] surahNames = {
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    };

    private int currentSurahNumber = 1; // السورة الحالية (تبدأ بالفاتحة)
    private float currentFontSize = 24.0f; // حجم الخط الافتراضي للقراءة

    // عناصر الواجهة
    private TextView tvSurahTitle, tvBasmala, tvSurahContent;
    private ProgressBar loadingBar;
    private ScrollView scrollView;
    private Button btnPrevSurah, btnNextSurah;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بناء الواجهة برمجياً
        setContentView(buildUserInterface());

        // تحميل سورة الفاتحة عند فتح التطبيق
        loadSurah(currentSurahNumber);
    }

    private View buildUserInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // خلفية داكنة مريحة للقراءة
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // شريط الأدوات العلوي (Header)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(dp(16), dp(14), dp(16), dp(14));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#1E293B"));

        Button btnIndex = createStyledButton("فهرس السور", "#10B981");
        btnIndex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSurahIndexDialog();
            }
        });
        topBar.addView(btnIndex);

        // مسافة فارغة
        View spacer = new View(this);
        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(spParams);
        topBar.addView(spacer);

        // زر تكبير الخط
        Button btnZoomIn = createStyledButton("A+", "#334155");
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFontSize < 40) {
                    currentFontSize += 2;
                    tvSurahContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                }
            }
        });
        topBar.addView(btnZoomIn);

        // زر تصغير الخط
        Button btnZoomOut = createStyledButton("A-", "#334155");
        LinearLayout.LayoutParams zoomOutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        zoomOutParams.setMargins(dp(8), 0, 0, 0);
        btnZoomOut.setLayoutParams(zoomOutParams);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFontSize > 16) {
                    currentFontSize -= 2;
                    tvSurahContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
                }
            }
        });
        topBar.addView(btnZoomOut);

        root.addView(topBar);

        // شريط التحميل (أثناء جلب السورة)
        loadingBar = new ProgressBar(this);
        loadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpBar = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBar.gravity = Gravity.CENTER;
        lpBar.setMargins(0, dp(20), 0, dp(10));
        loadingBar.setLayoutParams(lpBar);
        root.addView(loadingBar);

        // مساحة قراءة القرآن
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);
        scrollView.setPadding(dp(16), dp(12), dp(16), dp(16));

        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);

        // إطار بطاقة المصحف
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#172236"));
        cardBg.setCornerRadius(dp(16));
        contentContainer.setBackground(cardBg);
        contentContainer.setPadding(dp(20), dp(22), dp(20), dp(22));

        // عنوان السورة
        tvSurahTitle = new TextView(this);
        tvSurahTitle.setTextColor(Color.parseColor("#F59E0B")); // لون ذهبي
        tvSurahTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvSurahTitle.setTypeface(null, Typeface.BOLD);
        tvSurahTitle.setGravity(Gravity.CENTER);
        contentContainer.addView(tvSurahTitle);

        // البسملة
        tvBasmala = new TextView(this);
        tvBasmala.setText("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ");
        tvBasmala.setTextColor(Color.parseColor("#10B981")); // لون أخضر
        tvBasmala.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvBasmala.setGravity(Gravity.CENTER);
        tvBasmala.setPadding(0, dp(10), 0, dp(18));
        contentContainer.addView(tvBasmala);

        // نص آيات السورة
        tvSurahContent = new TextView(this);
        tvSurahContent.setTextColor(Color.WHITE);
        tvSurahContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSize);
        tvSurahContent.setLineSpacing(dp(12), 1.0f);
        tvSurahContent.setGravity(Gravity.CENTER_HORIZONTAL);
        contentContainer.addView(tvSurahContent);

        scrollView.addView(contentContainer);
        root.addView(scrollView);

        // شريط التنقل السفلي بين السور
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(dp(16), dp(10), dp(16), dp(14));
        bottomBar.setBackgroundColor(Color.parseColor("#1E293B"));

        btnPrevSurah = createStyledButton("السورة السابقة", "#334155");
        btnPrevSurah.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnPrevSurah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSurahNumber > 1) {
                    loadSurah(currentSurahNumber - 1);
                }
            }
        });
        bottomBar.addView(btnPrevSurah);

        View bSpacer = new View(this);
        bSpacer.setLayoutParams(new LinearLayout.LayoutParams(dp(14), 1));
        bottomBar.addView(bSpacer);

        btnNextSurah = createStyledButton("السورة التالية", "#334155");
        btnNextSurah.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnNextSurah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentSurahNumber < 114) {
                    loadSurah(currentSurahNumber + 1);
                }
            }
        });
        bottomBar.addView(btnNextSurah);

        root.addView(bottomBar);

        return root;
    }

    // نافذة اختيار السورة من الفهرس
    private void showSurahIndexDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر السورة");

        // تحضير الأسماء مع أرقامها
        String[] displayList = new String[114];
        for (int i = 0; i < 114; i++) {
            displayList[i] = (i + 1) + ". سورة " + surahNames[i];
        }

        builder.setItems(displayList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadSurah(which + 1);
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    // تحميل السورة بالرسم العثماني عبر الإنترنت
    private void loadSurah(final int surahNumber) {
        currentSurahNumber = surahNumber;
        tvSurahTitle.setText("سُورَةُ " + surahNames[surahNumber - 1]);

        // إخفاء البسملة في سورة التوبة (رقم 9) وسورة الفاتحة (لأنها آية فيها)
        if (surahNumber == 9 || surahNumber == 1) {
            tvBasmala.setVisibility(View.GONE);
        } else {
            tvBasmala.setVisibility(View.VISIBLE);
        }

        loadingBar.setVisibility(View.VISIBLE);
        tvSurahContent.setText("جاري تحميل السورة المباركة...");
        scrollView.scrollTo(0, 0);

        btnPrevSurah.setEnabled(surahNumber > 1);
        btnNextSurah.setEnabled(surahNumber < 114);

        // جلب نص السورة في خلفية غير متزامنة (Background Thread)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // الاتصال بمصدر المصحف بالرسم العثماني الموثوق
                    URL url = new URL("https://api.alquran.cloud/v1/surah/" + surahNumber + "/quran-uthmani");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonString = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                    reader.close();

                    // تحليل الآيات
                    JSONObject jsonRoot = new JSONObject(jsonString.toString());
                    JSONObject data = jsonRoot.getJSONObject("data");
                    JSONArray ayahs = data.getJSONArray("ayahs");

                    final StringBuilder fullSurahText = new StringBuilder();
                    String basmalaPattern = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";

                    for (int i = 0; i < ayahs.length(); i++) {
                        JSONObject ayahObj = ayahs.getJSONObject(i);
                        String ayahText = ayahObj.getString("text");
                        int ayahNum = ayahObj.getInt("numberInSurah");

                        // إزالة البسملة من أول آية إذا كانت مدمجة (عدا الفاتحة) لتبقى في الترويسة
                        if (surahNumber != 1 && surahNumber != 9 && i == 0 && ayahText.startsWith(basmalaPattern)) {
                            ayahText = ayahText.substring(basmalaPattern.length()).trim();
                        }

                        fullSurahText.append(ayahText);
                        fullSurahText.append(" ﴿").append(toArabicNumber(ayahNum)).append("﴾ ");
                    }

                    // تحديث الواجهة بنص السورة
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingBar.setVisibility(View.GONE);
                            tvSurahContent.setText(fullSurahText.toString());
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingBar.setVisibility(View.GONE);
                            tvSurahContent.setText("تعذر تحميل السورة، يرجى التأكد من اتصال الهاتف بالإنترنت ثم إعادة المحاولة.");
                            Toast.makeText(MainActivity.this, "تأكد من اتصالك بالإنترنت", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // تحويل الأرقام إلى أرقام عربية (١، ٢، ٣...)
    private String toArabicNumber(int number) {
        char[] arabicDigits = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
        String numStr = String.valueOf(number);
        StringBuilder builder = new StringBuilder();
        for (char c : numStr.toCharArray()) {
            builder.append(arabicDigits[c - '0']);
        }
        return builder.toString();
    }

    // دالة مساعدة لتصميم الأزرار
    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(dp(14), dp(8), dp(14), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorHex));
        bg.setCornerRadius(dp(10));
        btn.setBackground(bg);
        return btn;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}