package hack.project.cooldash.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import hack.project.cooldash.R;

public class GuessTranslationActivity extends AppCompatActivity {

    private static final String TAG = "GuessTranslationActivity";
    private static final int TOTAL_QUESTIONS = 10;

    private List<WordPair> words = new ArrayList<>();
    private int currentQuestion = 0;
    private int score = 0;

    private ProgressBar progressBar;
    private TextView progressText;
    private TextView wordTextView;
    private GridLayout optionsGrid;
    private MaterialButton nextButton;
    private CardView resultCard;
    private TextView scoreText;
    private TextView errorText, back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_translation);
        back = findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GuessTranslationActivity.this, MainActivity.class));
            }
        });
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        initializeUI();
        new LoadWordsTask().execute();
    }

    private void initializeUI() {
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);
        progressText = findViewById(R.id.progressText);
        wordTextView = findViewById(R.id.wordTextView);
        optionsGrid = findViewById(R.id.optionsGrid);
        nextButton = findViewById(R.id.nextButton);
        resultCard = findViewById(R.id.resultCard);
        scoreText = findViewById(R.id.scoreText);

        nextButton.setOnClickListener(v -> showNextQuestionWithAnimation());
        findViewById(R.id.restartButton).setOnClickListener(v -> restartQuiz());
    }

    private class LoadWordsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            showLoading(true);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                AssetManager assetManager = getAssets();
                InputStream inputStream = assetManager.open("tatar_words.json");
                Type listType = new TypeToken<ArrayList<WordPair>>() {
                }.getType();
                words = new Gson().fromJson(new InputStreamReader(inputStream), listType);
                inputStream.close();
                return !words.isEmpty();
            } catch (Exception e) {
                Log.e(TAG, "Error loading words: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            showLoading(false);

            if (success) {
                if (words.size() >= 5) {
                    startNewGame();
                } else {
                    showError("Недостаточно слов для начала квиза");
                }
            } else {
                showError("Ошибка загрузки данных");
            }
        }
    }

    private void startNewGame() {
        currentQuestion = 0;
        score = 0;
        Collections.shuffle(words);
        resultCard.setVisibility(View.GONE);
        showQuestion();
    }

    private void showQuestion() {
        if (currentQuestion >= TOTAL_QUESTIONS || currentQuestion >= words.size()) {
            handleEndOfQuiz();
            return;
        }

        WordPair currentWord = words.get(currentQuestion);
        wordTextView.setText(currentWord.getWord());
        progressText.setText(String.format("Вопрос %d/%d", currentQuestion + 1, TOTAL_QUESTIONS));

        List<String> options = generateOptions(currentWord);
        setupOptions(options, currentWord.getTranslation());
    }

    private List<String> generateOptions(WordPair correct) {
        List<String> options = new ArrayList<>();
        options.add(correct.getTranslation());

        List<WordPair> tempList = new ArrayList<>(words);
        tempList.remove(correct);

        Set<String> uniqueTranslations = new HashSet<>();
        for (WordPair pair : tempList) {
            if (!pair.getTranslation().equals(correct.getTranslation())) {
                uniqueTranslations.add(pair.getTranslation());
            }
        }

        List<String> uniqueOptions = new ArrayList<>(uniqueTranslations);
        Collections.shuffle(uniqueOptions);

        Random random = new Random();
        while (options.size() < 4) {
            if (!uniqueOptions.isEmpty()) {
                options.add(uniqueOptions.remove(0));
            } else {
                WordPair randomPair = tempList.get(random.nextInt(tempList.size()));
                if (!options.contains(randomPair.getTranslation())) {
                    options.add(randomPair.getTranslation());
                }
            }
        }

        Set<String> used = new HashSet<>();
        List<String> finalOptions = new ArrayList<>();
        for (String opt : options) {
            if (used.add(opt)) {
                finalOptions.add(opt);
            }
        }

        while (finalOptions.size() < 4) {
            finalOptions.add("Нет варианта");
        }

        Collections.shuffle(finalOptions);
        return finalOptions.subList(0, 4);
    }

    private void setupOptions(List<String> options, String correctAnswer) {
        optionsGrid.removeAllViews();
        optionsGrid.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        for (String option : options) {
            MaterialButton button = new MaterialButton(this);
            button.setText(option);
            button.setTag(option.equals(correctAnswer));
            button.setBackgroundColor(getColor(R.color.white));
            button.setTextColor(getColor(R.color.text_primary));
            button.setStrokeColorResource(R.color.primary);
            button.setStrokeWidth(2);
            button.setCornerRadius(16);
            button.setOnClickListener(this::onOptionClicked);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(16, 16, 16, 16);
            button.setLayoutParams(params);

            optionsGrid.addView(button);
        }
    }

    private void onOptionClicked(View view) {
        boolean isCorrect = (boolean) view.getTag();
        MaterialButton button = (MaterialButton) view;

        if (isCorrect) {
            button.setBackgroundColor(getColor(R.color.green));
            score++;
        } else {
            button.setBackgroundColor(getColor(R.color.red));
            highlightCorrectAnswer();
        }

        disableAllOptions();
        nextButton.setVisibility(View.VISIBLE);
        nextButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
    }

    private void highlightCorrectAnswer() {
        WordPair currentWord = words.get(currentQuestion);
        String correctAnswer = currentWord.getTranslation();

        for (int i = 0; i < optionsGrid.getChildCount(); i++) {
            View child = optionsGrid.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton optionButton = (MaterialButton) child;
                if (optionButton.getText().toString().equals(correctAnswer)) {
                    optionButton.setBackgroundColor(getColor(R.color.green));
                    break;
                }
            }
        }
    }

    private void showNextQuestionWithAnimation() {
        nextButton.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showNextQuestion();
                        nextButton.setAlpha(1f);
                    }
                });
    }

    private void showNextQuestion() {
        currentQuestion++;
        nextButton.setVisibility(View.GONE);
        showQuestion();
    }

    private void handleEndOfQuiz() {
        if (words.isEmpty()) return;

        showResults();
        if (words.size() < TOTAL_QUESTIONS) {
            Toast.makeText(this, "Доступно только " + words.size() + " вопросов", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResults() {
        resultCard.setVisibility(View.VISIBLE);
        scoreText.setText(String.format("Результат: %d/%d", score, Math.min(TOTAL_QUESTIONS, words.size())));
        resultCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
    }

    private void restartQuiz() {
        resultCard.setVisibility(View.GONE);
        startNewGame();
    }

    private void disableAllOptions() {
        for (int i = 0; i < optionsGrid.getChildCount(); i++) {
            optionsGrid.getChildAt(i).setEnabled(false);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        errorText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
    }

    static class WordPair {
        private final String word;
        private final String translation;

        public WordPair(String word, String translation) {
            this.word = word;
            this.translation = translation;
        }

        public String getWord() {
            return word;
        }

        public String getTranslation() {
            return translation;
        }
    }
}