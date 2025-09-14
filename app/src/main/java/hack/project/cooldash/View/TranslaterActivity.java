package hack.project.cooldash.View;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import hack.project.cooldash.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TranslaterActivity extends AppCompatActivity {
    private static final String TAG = "TranslaterActivity";

    private EditText inputText;
    private TextView outputText, back;
    private Spinner sourceLangSpinner;
    private Spinner targetLangSpinner;
    private ImageView swapButton;
    private Button translateButton;
    private OkHttpClient client;
    private String currentDirection = "rus2tat";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translater);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        back = findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TranslaterActivity.this, MainActivity.class));
            }
        });
        client = createUnsafeOkHttpClient();
        initializeViews();
        setupSpinners();
        setDefaultLanguages();
        setupButton();
        setupSwapButton();
        setupCopyFeature();
    }

    private void initializeViews() {
        inputText = findViewById(R.id.input_text);
        outputText = findViewById(R.id.output_text);
        sourceLangSpinner = findViewById(R.id.source_lang);
        targetLangSpinner = findViewById(R.id.target_lang);
        swapButton = findViewById(R.id.swap_button);
        translateButton = findViewById(R.id.translate_button);
    }

    private void setDefaultLanguages() {
        sourceLangSpinner.setSelection(1); // Russian
        targetLangSpinner.setSelection(0); // Tatar
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sourceLangSpinner.setAdapter(adapter);
        targetLangSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTranslationDirection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        sourceLangSpinner.setOnItemSelectedListener(listener);
        targetLangSpinner.setOnItemSelectedListener(listener);
    }

    private void setupSwapButton() {
        swapButton.setOnClickListener(v -> {
            int sourcePosition = sourceLangSpinner.getSelectedItemPosition();
            int targetPosition = targetLangSpinner.getSelectedItemPosition();

            sourceLangSpinner.setSelection(targetPosition);
            targetLangSpinner.setSelection(sourcePosition);
        });
    }

    private void setupButton() {
        translateButton.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, R.string.enter_text, Toast.LENGTH_SHORT).show();
                return;
            }
            translateText(text);
        });
    }

    private void updateTranslationDirection() {
        String source = sourceLangSpinner.getSelectedItem().toString();
        String target = targetLangSpinner.getSelectedItem().toString();
        currentDirection = (source.equals("Татарский") && target.equals("Русский"))
                ? "tat2rus"
                : "rus2tat";
    }

    private void translateText(String text) {
        executor.execute(() -> {
            try {
                String url = buildUrl(text);
                Log.d(TAG, "Request URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            outputText.setText(parseResponse(responseBody));
                        } else {
                            outputText.setText(getString(R.string.translation_error, response.code()));
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error: ", e);
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.network_error, e.getMessage()),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private String parseResponse(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Element translation = doc.select("translation").first();

            if (translation != null) {
                return translation.text()
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
            }

            Element error = doc.select("error").first();
            if (error != null) {
                return getString(R.string.server_error, error.text());
            }

            return doc.body().text().replace("\\n", "\n");

        } catch (Exception e) {
            Log.e(TAG, "Parsing error: ", e);
            return getString(R.string.parse_error);
        }
    }

    private String buildUrl(String text) throws IOException {
        String baseUrl = currentDirection.equals("tat2rus")
                ? "https://translate.tatar/translate?lang=1&text="
                : "https://translate.tatar/translate?lang=0&text=";

        return baseUrl + URLEncoder.encode(text, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", "%20")
                .replaceAll("%(?![0-9a-fA-F]{2})", "%25");
    }

    private void setupCopyFeature() {
        outputText.setOnLongClickListener(v -> {
            String text = outputText.getText().toString();
            if (!text.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Translation", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private OkHttpClient createUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(
                                java.security.cert.X509Certificate[] chain,
                                String authType) {}
                        @Override public void checkServerTrusted(
                                java.security.cert.X509Certificate[] chain,
                                String authType) {}
                        @Override public java.security.cert.X509Certificate[]
                        getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}