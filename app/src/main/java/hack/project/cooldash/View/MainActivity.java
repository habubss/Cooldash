package hack.project.cooldash.View;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import hack.project.cooldash.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        TextView logoutButton = findViewById(R.id.logout_btn);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        LinearLayout wordLearningLayout = findViewById(R.id.layout_word_learning);
        LinearLayout translatorLayout = findViewById(R.id.layout_translator);
        LinearLayout guessTranslationLayout = findViewById(R.id.layout_guess_translation);
        LinearLayout listeningLayout = findViewById(R.id.layout_listening);
        LinearLayout favorite = findViewById(R.id.layout_favorites);
        LinearLayout dictionarySearchLayout = findViewById(R.id.layout_dictionary_search);
        LinearLayout wordGameLayout = findViewById(R.id.layout_word_game);
        wordGameLayout.setOnClickListener(v -> {
            Log.i(TAG, "Нажатие на 'Сүз Боткасы' зафиксировано");
            try {
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra("url", "https://suzbotkasi.netlify.app/");
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                showError("Не удалось открыть игру");
            }
        });

        dictionarySearchLayout.setOnClickListener(v -> {
            Log.i(TAG, "Нажатие на 'Поиск слов' зафиксировано");
            try {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                showError("Не удалось открыть раздел");
            }
        });

        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Угадать перевод' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        listeningLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Угадать перевод' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, ListeningActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        guessTranslationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Угадать перевод' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, GuessTranslationActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        translatorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Переводчик' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, TranslaterActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        wordLearningLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Изучение слов' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, WordLearningActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}