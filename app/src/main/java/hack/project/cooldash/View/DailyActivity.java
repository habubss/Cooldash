package hack.project.cooldash.View;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import hack.project.cooldash.Controller.ApiClient;
import hack.project.cooldash.R;
import hack.project.cooldash.Utils.DailyManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class DailyActivity extends AppCompatActivity {

    private static final String TAG = "DailyActivity";
    private static final int MAX_LISTEN_ATTEMPTS = 3;

    private DailyManager dailyManager;
    private TextView listenCountTextView;
    private EditText inputEditText;
    private Button checkButton;
    private ImageView playButton;
    private ProgressBar loadingProgress;
    private CardView resultCard;
    private TextView resultTextView;
    private TextView correctTextView, back;

    private RequestQueue requestQueue;
    private MediaPlayer mediaPlayer;
    private String generatedText = "";
    private int listenAttempts = MAX_LISTEN_ATTEMPTS;
    private boolean isChecked = false;
    private boolean isPlayingAudio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);
        back = findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DailyActivity.this, MainActivity.class));
            }
        });

        dailyManager = new DailyManager(this);

        // Проверяем, может ли пользователь играть сегодня
        if (!dailyManager.canPlayDaily()) {
            showError("Вы уже прошли ежедневный диктант сегодня. Попробуйте завтра!");
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        initializeRequestQueue();
        generateTatarText();
    }

    private void initializeViews() {
        listenCountTextView = findViewById(R.id.listen_count);
        inputEditText = findViewById(R.id.input_text);
        checkButton = findViewById(R.id.check_button);
        playButton = findViewById(R.id.play_button);
        loadingProgress = findViewById(R.id.loading_progress);
        resultCard = findViewById(R.id.result_card);
        resultTextView = findViewById(R.id.result_text);
        correctTextView = findViewById(R.id.correct_text);

        inputEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkButton.setEnabled(!s.toString().trim().isEmpty() && listenAttempts < MAX_LISTEN_ATTEMPTS);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupClickListeners() {
        playButton.setOnClickListener(v -> playTatarText());
        checkButton.setOnClickListener(v -> checkAnswer());
    }

    private void initializeRequestQueue() {
        requestQueue = Volley.newRequestQueue(this);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void generateTatarText() {
        Toast.makeText(this, "Генерируется предложение...", Toast.LENGTH_SHORT).show();
        loadingProgress.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);

        if (!isNetworkAvailable()) {
            showError("Нет интернет-соединения");
            generatedText = getFallbackTatarText();
            loadingProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Сгенерировано", Toast.LENGTH_SHORT).show();
            playButton.setEnabled(true);
            updateListenCount();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "deepseek-chat");
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", "Сгенерируй предложение на совершенно другую тему на татарском языке из 5-8 слов. Только текст, без пояснений. Не используй кавычки.");
            requestBody.put("messages", new org.json.JSONArray().put(message));
            requestBody.put("max_tokens", 50);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage());
            showError("Ошибка создания запроса");
            generatedText = getFallbackTatarText();
            loadingProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Сгенерировано", Toast.LENGTH_SHORT).show();
            playButton.setEnabled(true);
            updateListenCount();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                getString(R.string.deepseek_api_url),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "DeepSeek Response: " + response.toString());
                            String content = response.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")
                                    .trim();
                            content = content.replaceAll("^[\"']|[\"']$", "").trim();
                            String[] sentences = content.split("[.!?]");
                            if (sentences.length > 0) {
                                generatedText = sentences[0].trim();
                            } else {
                                generatedText = content;
                            }
                            if (generatedText.isEmpty()) {
                                generatedText = getFallbackTatarText();
                            }
                            Log.d(TAG, "Generated text: " + generatedText);
                            updateListenCount();
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing DeepSeek response: " + e.getMessage());
                            showError("Ошибка обработки ответа");
                            generatedText = getFallbackTatarText();
                        }
                        loadingProgress.setVisibility(View.GONE);
                        playButton.setEnabled(true);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "DeepSeek API Error: " + error.toString());
                        if (error instanceof com.android.volley.NoConnectionError) {
                            showError("Нет соединения с интернетом");
                        } else if (error instanceof com.android.volley.TimeoutError) {
                            showError("Таймаут соединения");
                        } else if (error.networkResponse != null) {
                            showError("Ошибка сервера: " + error.networkResponse.statusCode);
                        } else {
                            showError("Ошибка сети: " + error.getMessage());
                        }
                        generatedText = getFallbackTatarText();
                        loadingProgress.setVisibility(View.GONE);
                        playButton.setEnabled(true);
                        updateListenCount();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getString(R.string.deepseek_api_key));
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public RetryPolicy getRetryPolicy() {
                return new DefaultRetryPolicy(
                        10000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            }
        };

        requestQueue.add(request);
    }

    private String getFallbackTatarText() {
        String[] fallbackTexts = {
                "Мин яхшы укыйм, язам.",
                "Мин яхшы укыйм һәм язам.",
                "Татар теле бик матур тел.",
                "Бездә күп кенә китаплар бар.",
                "Аның өчен яңа сүзләр өйрәнәм.",
                "Бүген һава яхшы һәм кояшлы.",
                "Минем гаиләдә дүрт кеше бар.",
                "Ул яңа китап укый һәм яза.",
                "Без паркта йөрергә яратабыз.",
                "Әни пешерелгән аш әзерли.",
                "Кошлар күктә очалар иртә белән."
        };
        return fallbackTexts[new Random().nextInt(fallbackTexts.length)];
    }

    private void playTatarText() {
        if (generatedText.isEmpty()) {
            showError("Текст еще не загружен");
            return;
        }
        if (listenAttempts <= 0 && !isChecked) {
            showError("Попытки прослушивания закончились");
            return;
        }
        if (isPlayingAudio) {
            Toast.makeText(this, "Уже воспроизводится аудио", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isChecked) {
            listenAttempts--;
            updateListenCount();
        }
        playButton.setEnabled(false);
        isPlayingAudio = true;
        synthesizeText(generatedText);
    }

    private void playAudio(InputStream inputStream) throws IOException {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        File tempFile = File.createTempFile("audio", ".wav", getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(tempFile.getAbsolutePath());
        mediaPlayer.prepare();
        mediaPlayer.setOnCompletionListener(mp -> {
            playButton.setEnabled(true);
            isPlayingAudio = false;
            mp.release();
            mediaPlayer = null;
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Ошибка воспроизведения: " + what + ", " + extra);
            playButton.setEnabled(true);
            isPlayingAudio = false;
            mp.release();
            mediaPlayer = null;
            return true;
        });
        mediaPlayer.start();
    }

    private void synthesizeText(String text) {
        if (!isNetworkAvailable()) {
            showError("Нет интернета для синтеза речи");
            playButton.setEnabled(true);
            isPlayingAudio = false;
            return;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);
        Toast.makeText(this, "Синтезируется речь...", Toast.LENGTH_SHORT).show();
        ApiClient.getApi().synthesizeText(jsonObject).enqueue(new Callback<ResponseBody>() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            playAudio(response.body().byteStream());
                            Toast.makeText(DailyActivity.this, "Озвучка успешна", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка чтения аудиопотока: ", e);
                            Toast.makeText(DailyActivity.this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                            playButton.setEnabled(true);
                            isPlayingAudio = false;
                        }
                    } else {
                        Toast.makeText(DailyActivity.this, "Сервер не вернул аудио", Toast.LENGTH_SHORT).show();
                        playButton.setEnabled(true);
                        isPlayingAudio = false;
                    }
                } else {
                    Toast.makeText(DailyActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                    playButton.setEnabled(true);
                    isPlayingAudio = false;
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DailyActivity.this, "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_LONG).show();
                playButton.setEnabled(true);
                isPlayingAudio = false;
            }
        });
    }

    private void updateListenCount() {
        String text = "Попыток: " + listenAttempts + "/" + MAX_LISTEN_ATTEMPTS;
        if (isChecked) {
            text += " (проверено)";
        }
        listenCountTextView.setText(text);
        checkButton.setEnabled(!inputEditText.getText().toString().trim().isEmpty() &&
                (listenAttempts < MAX_LISTEN_ATTEMPTS || isChecked));
    }

    private void checkAnswer() {
        String userInput = inputEditText.getText().toString().trim();
        if (userInput.isEmpty()) {
            showError("Введите текст для проверки");
            return;
        }
        isChecked = true;
        updateListenCount();
        String normalizedUser = normalizeText(userInput);
        String normalizedOriginal = normalizeText(generatedText);
        boolean isCorrect = normalizedUser.equals(normalizedOriginal);

        // Сохраняем результат в Firebase
        dailyManager.completeDaily(isCorrect, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                runOnUiThread(() -> {
                    if (task.isSuccessful()) {
                        showResult(isCorrect, userInput, generatedText);
                    } else {
                        showError("Ошибка сохранения результата: " + task.getException().getMessage());
                        showResult(isCorrect, userInput, generatedText);
                    }
                    // Блокируем возможность повторной проверки
                    checkButton.setEnabled(false);
                    inputEditText.setEnabled(false);
                    playButton.setEnabled(false);
                });
            }
        });
    }

    private String normalizeText(String text) {
        return text.replaceAll("[.!?,;:]+$", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void showResult(boolean isCorrect, String userInput, String originalText) {
        resultCard.setVisibility(View.VISIBLE);
        if (isCorrect) {
            resultTextView.setText("✅ Верно! Вы правильно написали текст.");
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            resultTextView.setText("❌ Есть ошибки. Сравните с оригиналом:");
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        SpannableString highlightedText = highlightDifferences(userInput, originalText);
        correctTextView.setText(highlightedText);
        listenCountTextView.setText("Попыток: неограниченно (после проверки)");
    }

    private SpannableString highlightDifferences(String userText, String originalText) {
        SpannableString spannable = new SpannableString(originalText);
        String normUser = normalizeText(userText);
        String normOriginal = normalizeText(originalText);
        String[] userWords = normUser.split("\\s+");
        String[] originalWords = normOriginal.split("\\s+");
        String[] origWordsWithPunct = originalText.split("\\s+");
        int pos = 0;
        for (int i = 0; i < origWordsWithPunct.length; i++) {
            String wordWithPunct = origWordsWithPunct[i];
            int start = pos;
            int end = pos + wordWithPunct.length();
            boolean matches = i < userWords.length &&
                    i < originalWords.length &&
                    userWords[i].equals(originalWords[i]);
            if (!matches) {
                spannable.setSpan(
                        new ForegroundColorSpan(getResources().getColor(android.R.color.holo_red_dark)),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            pos = end + 1;
        }
        return spannable;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlayingAudio = false;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}