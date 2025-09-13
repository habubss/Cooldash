package hack.project.cooldash.View;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import hack.project.cooldash.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private boolean isGuest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Проверяем, вошел ли пользователь как гость
        isGuest = getIntent().getBooleanExtra("isGuest", false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null && !isGuest) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        TextView logoutButton = findViewById(R.id.logout_btn);
        ImageView favoriteLayout = findViewById(R.id.layout_favorites);

        logoutButton.setOnClickListener(v -> {
            if (isGuest) {
                // Для гостя переход на экран логина
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            } else {
                // Для авторизованного пользователя - выход
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
            finish();
        });

        ImageView dailyLayout = findViewById(R.id.layout_daily);
        ImageView wordLearningLayout = findViewById(R.id.layout_word_learning);
        ImageView translatorLayout = findViewById(R.id.layout_translator);
        ImageView guessTranslationLayout = findViewById(R.id.layout_guess_translation);
        ImageView listeningLayout = findViewById(R.id.layout_listening);
        ImageView favorite = findViewById(R.id.layout_favorites);
        ImageView dictionarySearchLayout = findViewById(R.id.layout_dictionary_search);
        ImageView wordGameLayout = findViewById(R.id.layout_word_game);

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

        dailyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGuest) {
                    // Для гостя показываем сообщение о необходимости регистрации
                    Toast.makeText(MainActivity.this,
                            "Для доступа к ежедневному заданию необходимо войти в аккаунт",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Для авторизованного пользователя - стандартное поведение
                    try {
                        startActivity(new Intent(MainActivity.this, DailyActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                        showError("Не удалось открыть раздел");
                    }
                }
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

        // Для гостя скрываем функционал избранного в обработчиках
        favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGuest) {
                    // Для гостя показываем сообщение о необходимости регистрации
                    Toast.makeText(MainActivity.this,
                            "Для доступа к избранному необходимо войти в аккаунт",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Для авторизованного пользователя - стандартное поведение
                    Log.i(TAG, "Нажатие на 'Избранное' зафиксировано");
                    try {
                        startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                        showError("Не удалось открыть раздел");
                    }
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
                    Intent intent = new Intent(MainActivity.this, WordLearningActivity.class);
                    intent.putExtra("isGuest", isGuest);
                    startActivity(intent);
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