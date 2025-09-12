package hack.project.cooldash.View;

import static androidx.fragment.app.FragmentManager.TAG;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

        // Play button
        Button playButton = new Button(this);
        playButton.setText("Озвучить");
        playButton.setOnClickListener(v -> synthesizeText(wordData.get("word")));
        buttonsLayout.addView(playButton);

        // Delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("Удалить");
        deleteButton.setOnClickListener(v -> removeFavorite(wordData.get("word")));
        buttonsLayout.addView(deleteButton);

        layout.addView(buttonsLayout);
        card.addView(layout);
        favoritesContainer.addView(card);
    }

    private void synthesizeText(String text) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);

        Toast.makeText(this, "Синтезируется речь...", Toast.LENGTH_SHORT).show();

        ApiClient.getApi().synthesizeText(jsonObject).enqueue(new Callback<ResponseBody>() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            playAudio(response.body().byteStream());
                            Toast.makeText(FavoritesActivity.this,
                                    "Озвучка успешна", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка чтения аудиопотока: ", e);
                            Toast.makeText(FavoritesActivity.this,
                                    "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FavoritesActivity.this,
                                "Сервер не вернул аудио", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoritesActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(FavoritesActivity.this,
                        "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}