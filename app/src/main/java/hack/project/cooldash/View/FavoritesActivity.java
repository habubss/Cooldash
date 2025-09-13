package hack.project.cooldash.View;

import static androidx.fragment.app.FragmentManager.TAG;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import hack.project.cooldash.Controller.ApiClient;
import hack.project.cooldash.R;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {

    private DatabaseReference favoritesRef;
    private LinearLayout favoritesContainer;
    private MediaPlayer mediaPlayer;
    // Добавляем переменную для отслеживания состояния воспроизведения
    private boolean isPlayingAudio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        favoritesContainer = findViewById(R.id.favoritesContainer);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(userId)
                .child("Favorites");

        loadFavorites();
    }

    private void loadFavorites() {
        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoritesContainer.removeAllViews();

                if (!snapshot.exists()) {
                    TextView emptyText = new TextView(FavoritesActivity.this);
                    emptyText.setText("Нет избранных слов");
                    emptyText.setTextSize(16);
                    emptyText.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    favoritesContainer.addView(emptyText);
                    return;
                }

                for (DataSnapshot wordSnapshot : snapshot.getChildren()) {
                    Map<String, String> wordData = (Map<String, String>) wordSnapshot.getValue();
                    addFavoriteCard(wordData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FavoritesActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("ResourceAsColor")
    private void addFavoriteCard(Map<String, String> wordData) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(12);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundResource(R.color.bezheviy);

        // Word
        TextView wordText = new TextView(this);
        wordText.setText(wordData.get("word"));
        wordText.setTextSize(20);
        wordText.setTextColor(getResources().getColor(R.color.text_primary));
        layout.addView(wordText);

        // Translation
        TextView translationText = new TextView(this);
        translationText.setText(wordData.get("translation"));
        translationText.setTextSize(16);
        translationText.setTextColor(getResources().getColor(R.color.text_secondary));
        translationText.setPadding(0, 8, 0, 16);
        layout.addView(translationText);

        // Buttons
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setWeightSum(2); // Устанавливаем сумму весов

        // Play button
        ImageView playButton = new ImageView(this);
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                0, // ширина будет определяться weight
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        playParams.weight = 1; // занимает половину пространства
        playParams.setMargins(0, 0, 5, 16); // отступ справа 5dp и снизу 16dp
        playButton.setLayoutParams(playParams);
        playButton.setImageResource(R.drawable.spell);
        playButton.setAdjustViewBounds(true);
        playButton.setClickable(true);
        playButton.setFocusable(true);
        playButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        playButton.setOnClickListener(v -> synthesizeText(wordData.get("word"), playButton));
        buttonsLayout.addView(playButton);

        // Delete button
        ImageView deleteButton = new ImageView(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0, // ширина будет определяться weight
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.weight = 1; // занимает половину пространства
        deleteParams.setMargins(5, 0, 0, 16); // отступ слева 5dp и снизу 16dp
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(R.drawable.delete);
        deleteButton.setAdjustViewBounds(true);
        deleteButton.setClickable(true);
        deleteButton.setFocusable(true);
        deleteButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        deleteButton.setOnClickListener(v -> removeFavorite(wordData.get("word")));
        buttonsLayout.addView(deleteButton);

        layout.addView(buttonsLayout);
        card.addView(layout);
        favoritesContainer.addView(card);
    }

    // Добавляем параметр ImageView для блокировки конкретной кнопки
    private void synthesizeText(String text, ImageView playButton) {
        // Проверяем, не воспроизводится ли уже аудио
        if (isPlayingAudio) {
            Toast.makeText(this, "Уже воспроизводится аудио", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);
        Toast.makeText(this, "Синтезируется речь...", Toast.LENGTH_SHORT).show();

        // Блокируем кнопку
        playButton.setEnabled(false);
        isPlayingAudio = true;

        ApiClient.getApi().synthesizeText(jsonObject).enqueue(new Callback<ResponseBody>() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            playAudio(response.body().byteStream(), playButton);
                            Toast.makeText(FavoritesActivity.this,
                                    "Озвучка успешна", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка чтения аудиопотока: ", e);
                            Toast.makeText(FavoritesActivity.this,
                                    "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                            // Разблокируем кнопку при ошибке
                            playButton.setEnabled(true);
                            isPlayingAudio = false;
                        }
                    } else {
                        Toast.makeText(FavoritesActivity.this,
                                "Сервер не вернул аудио", Toast.LENGTH_SHORT).show();
                        // Разблокируем кнопку при ошибке
                        playButton.setEnabled(true);
                        isPlayingAudio = false;
                    }
                } else {
                    Toast.makeText(FavoritesActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                    // Разблокируем кнопку при ошибке
                    playButton.setEnabled(true);
                    isPlayingAudio = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(FavoritesActivity.this,
                        "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_LONG).show();
                // Разблокируем кнопку при ошибке
                playButton.setEnabled(true);
                isPlayingAudio = false;
            }
        });
    }

    // Добавляем параметр ImageView для разблокировки конкретной кнопки
    @SuppressLint("RestrictedApi")
    private void playAudio(InputStream inputStream, ImageView playButton) throws IOException {
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

        // Добавляем обработчик завершения воспроизведения
        mediaPlayer.setOnCompletionListener(mp -> {
            // Разблокируем кнопку после завершения воспроизведения
            playButton.setEnabled(true);
            isPlayingAudio = false;
        });

        // Добавляем обработчик ошибок воспроизведения
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Ошибка воспроизведения: " + what + ", " + extra);
            // Разблокируем кнопку при ошибке
            playButton.setEnabled(true);
            isPlayingAudio = false;
            return true;
        });

        mediaPlayer.start();
    }

    private void removeFavorite(String word) {
        favoritesRef.child(word).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlayingAudio = false; // Сбрасываем состояние

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
