package hack.project.cooldash.View;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hack.project.cooldash.R;
import hack.project.cooldash.WordDefinition;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private LinearLayout resultsContainer;
    private List<WordDefinition> dictionary = new ArrayList<>();
    private List<WordDefinition> allWords = new ArrayList<>();

    // Оптимизированные структуры данных для быстрого поиска
    private Map<String, List<WordDefinition>> wordIndex = new HashMap<>();
    private Map<String, List<WordDefinition>> translationIndex = new HashMap<>();
    private ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        searchInput = findViewById(R.id.search_input);
        ImageButton searchButton = findViewById(R.id.search_button);
        resultsContainer = findViewById(R.id.results_container);

        loadDictionaryFromAssets();

        showInitialWords();

        searchButton.setOnClickListener(v -> performSearch());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Используем debounce для избежания множественных поисков при быстром вводе
                resultsContainer.removeCallbacks(searchRunnable);
                resultsContainer.postDelayed(searchRunnable, 300); // Задержка 300ms
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // Runnable для отложенного поиска
    private Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            performSearch();
        }
    };

    private void loadDictionaryFromAssets() {
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("tatar_words.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<WordDefinition>>(){}.getType();
            List<WordDefinition> loadedWords = gson.fromJson(json, listType);

            if (loadedWords != null) {
                dictionary = loadedWords;
                allWords = new ArrayList<>(loadedWords);

                // Строим индексы для быстрого поиска
                buildSearchIndexes();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки словаря", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildSearchIndexes() {
        for (WordDefinition word : dictionary) {
            if (word != null) {
                // Индекс по слову
                String wordKey = word.getWord() != null ? word.getWord().toLowerCase() : "";
                if (!wordKey.isEmpty()) {
                    if (!wordIndex.containsKey(wordKey)) {
                        wordIndex.put(wordKey, new ArrayList<>());
                    }
                    wordIndex.get(wordKey).add(word);
                }

                // Индекс по переводу
                String translationKey = word.getTranslation() != null ? word.getTranslation().toLowerCase() : "";
                if (!translationKey.isEmpty()) {
                    if (!translationIndex.containsKey(translationKey)) {
                        translationIndex.put(translationKey, new ArrayList<>());
                    }
                    translationIndex.get(translationKey).add(word);
                }
            }
        }
    }

    private void showInitialWords() {
        resultsContainer.removeAllViews();
        if (allWords.isEmpty()) return;

        int count = Math.min(100, allWords.size());
        for (int i = 0; i < count; i++) {
            WordDefinition word = allWords.get(i);
            if (word != null) {
                addWordResultView(word);
            }
        }

        if (allWords.size() > 100) {
            addHintText("Показаны первые 100 слов. Введите поисковой запрос для полного поиска.");
        }
    }

    private void performSearch() {
        final String query = searchInput.getText() != null ?
                searchInput.getText().toString().trim().toLowerCase() : "";

        // Очищаем контейнер в UI потоке
        runOnUiThread(() -> resultsContainer.removeAllViews());

        if (query.isEmpty()) {
            runOnUiThread(this::showInitialWords);
            return;
        }

        // Выполняем поиск в фоновом потоке
        searchExecutor.execute(() -> {
            List<WordDefinition> results = new ArrayList<>();

            // Быстрый поиск по индексам
            if (wordIndex.containsKey(query)) {
                results.addAll(wordIndex.get(query));
            }

            if (translationIndex.containsKey(query)) {
                results.addAll(translationIndex.get(query));
            }

            // Если точного совпадения нет, ищем частичные совпадения
            if (results.isEmpty()) {
                for (WordDefinition word : dictionary) {
                    if (word != null &&
                            (containsIgnoreNull(word.getWord(), query) ||
                                    containsIgnoreNull(word.getTranslation(), query))) {
                        results.add(word);
                    }
                }
            }

            // Отображаем результаты в UI потоке
            runOnUiThread(() -> {
                resultsContainer.removeAllViews();

                if (results.isEmpty()) {
                    addHintText("Слово не найдено");
                } else {
                    for (WordDefinition word : results) {
                        addWordResultView(word);
                    }
                }
            });
        });
    }

    private boolean containsIgnoreNull(String str, String query) {
        return str != null && str.toLowerCase().contains(query);
    }

    private void addHintText(String text) {
        TextView hint = new TextView(this);
        hint.setText(text);
        hint.setTextSize(14);
        hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        hint.setPadding(0, 16, 0, 0);
        hint.setGravity(Gravity.CENTER);
        resultsContainer.addView(hint);
    }

    private void addWordResultView(WordDefinition word) {
        if (word == null) return;

        View resultView = getLayoutInflater().inflate(R.layout.item_word_result, resultsContainer, false);

        TextView wordText = resultView.findViewById(R.id.word_text);
        TextView partOfSpeech = resultView.findViewById(R.id.part_of_speech);
        TextView definition = resultView.findViewById(R.id.definition);

        wordText.setText(word.getWord() != null ? word.getWord() : "");

        partOfSpeech.setText(getTypeName(word.getType()));
        partOfSpeech.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        partOfSpeech.setTextSize(14);

        definition.setText(word.getTranslation() != null ? word.getTranslation() : "");
        definition.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        definition.setTextSize(16);

        resultsContainer.addView(resultView);
    }

    private String getTypeName(String type) {
        if (type == null) return "";
        switch (type) {
            case "N": return "Существительное";
            case "V": return "Глагол";
            case "Adj": return "Прилагательное";
            case "Adv": return "Наречие";
            case "Pron": return "Местоимение";
            default: return type;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchExecutor.shutdown();
    }
}