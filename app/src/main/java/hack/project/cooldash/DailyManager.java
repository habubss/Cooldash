package hack.project.cooldash.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class DailyManager {
    private static final String TAG = "DailyManager";
    private static final String PREF_NAME = "daily_prefs";
    private static final String KEY_LAST_COMPLETED_DATE = "last_completed_date";
    private static final String KEY_STREAK_COUNT = "streak_count";
    private static final String KEY_TODAY_COMPLETED = "today_completed";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_LAST_FIREBASE_SYNC = "last_firebase_sync";

    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Context context;

    public DailyManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public boolean canPlayDaily() {
        if (isTodayCompleted()) {
            return false;
        }
        checkAndResetStreakIfNeeded();
        return true;
    }

    public void completeDaily(boolean isCorrect, OnCompleteListener<Void> completionListener) {
        Calendar today = Calendar.getInstance();
        String todayDate = getDateString(today);

        // Сохраняем локально
        prefs.edit()
                .putString(getPrefKey(KEY_LAST_COMPLETED_DATE), todayDate)
                .putBoolean(getPrefKey(KEY_TODAY_COMPLETED), true)
                .apply();

        if (isCorrect) {
            int currentStreak = getCurrentStreak();
            prefs.edit().putInt(getPrefKey(KEY_STREAK_COUNT), currentStreak + 1).apply();
            saveToFirebase(currentStreak + 1, todayDate, isCorrect, completionListener);
        } else {
            prefs.edit().putInt(getPrefKey(KEY_STREAK_COUNT), 0).apply();
            saveToFirebase(0, todayDate, isCorrect, completionListener);
        }
    }

    public int getCurrentStreak() {
        checkAndResetStreakIfNeeded();
        return prefs.getInt(getPrefKey(KEY_STREAK_COUNT), 0);
    }

    public boolean isTodayCompleted() {
        return prefs.getBoolean(getPrefKey(KEY_TODAY_COMPLETED), false);
    }

    public void syncFromFirebase(OnCompleteListener<Void> completionListener) {
        if (currentUser == null) {
            if (completionListener != null) {
                // Создаем успешную задачу для гостя
                completionListener.onComplete(createSuccessTask());
            }
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        Long streak = document.getLong("dailyStreak");
                        String lastDate = document.getString("lastDailyDate");

                        if (streak != null) {
                            prefs.edit().putInt(getPrefKey(KEY_STREAK_COUNT), streak.intValue()).apply();
                        }
                        if (lastDate != null) {
                            prefs.edit().putString(getPrefKey(KEY_LAST_COMPLETED_DATE), lastDate).apply();
                        }

                        prefs.edit().putString(getPrefKey(KEY_LAST_FIREBASE_SYNC), getDateString(Calendar.getInstance())).apply();
                    }

                    if (completionListener != null) {
                        // Вызываем completionListener с Void task
                        completionListener.onComplete(createSuccessTask());
                    }
                });
    }

    private void saveToFirebase(int streak, String date, boolean isCorrect, OnCompleteListener<Void> completionListener) {
        if (currentUser == null) {
            if (completionListener != null) {
                completionListener.onComplete(createSuccessTask());
            }
            return;
        }

        String userId = currentUser.getUid();

        // Обновляем основную информацию пользователя
        Map<String, Object> userData = new HashMap<>();
        userData.put("dailyStreak", streak);
        userData.put("lastDailyDate", date);
        userData.put("lastDailyCorrect", isCorrect);

        // Сохраняем историю
        Map<String, Object> dailyData = new HashMap<>();
        dailyData.put("date", date);
        dailyData.put("streak", streak);
        dailyData.put("isCorrect", isCorrect);
        dailyData.put("timestamp", Calendar.getInstance().getTime());

        // Сохраняем в Firestore
        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        db.collection("users").document(userId)
                                .collection("daily_history")
                                .document(date)
                                .set(dailyData)
                                .addOnCompleteListener(completionListener);
                    } else {
                        if (completionListener != null) {
                            completionListener.onComplete(task);
                        }
                    }
                });
    }

    private void checkAndResetStreakIfNeeded() {
        Calendar today = Calendar.getInstance();
        String todayDate = getDateString(today);
        String lastResetDate = prefs.getString(getPrefKey(KEY_LAST_RESET_DATE), "");

        if (!lastResetDate.equals(todayDate)) {
            String lastCompletedDate = prefs.getString(getPrefKey(KEY_LAST_COMPLETED_DATE), "");

            if (!lastCompletedDate.equals(todayDate) && !lastCompletedDate.isEmpty()) {
                // Проверяем, был ли вчерашний день завершен
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_MONTH, -1);
                String yesterdayDate = getDateString(yesterday);

                if (lastCompletedDate.equals(yesterdayDate)) {
                    // Вчера завершили - счетчик остается
                } else {
                    // Пропустили день - сбрасываем счетчик
                    prefs.edit().putInt(getPrefKey(KEY_STREAK_COUNT), 0).apply();
                }
            }

            prefs.edit()
                    .putBoolean(getPrefKey(KEY_TODAY_COMPLETED), false)
                    .putString(getPrefKey(KEY_LAST_RESET_DATE), todayDate)
                    .apply();
        }
    }

    private String getPrefKey(String key) {
        return currentUser != null ? currentUser.getUid() + "_" + key : "guest_" + key;
    }

    private String getDateString(Calendar calendar) {
        return calendar.get(Calendar.YEAR) + "-" +
                (calendar.get(Calendar.MONTH) + 1) + "-" +
                calendar.get(Calendar.DAY_OF_MONTH);
    }

    public String getLastCompletedDate() {
        return prefs.getString(getPrefKey(KEY_LAST_COMPLETED_DATE), "");
    }

    public boolean isUserLoggedIn() {
        return currentUser != null;
    }

    // Создаем успешную Task<Void> для гостевых пользователей
    private Task<Void> createSuccessTask() {
        return new Task<Void>() {
            @Override
            public boolean isComplete() { return true; }
            @Override
            public boolean isSuccessful() { return true; }
            @Override
            public boolean isCanceled() { return false; }
            @Override
            public Void getResult() { return null; }
            @Override
            public <X extends Throwable> Void getResult(Class<X> aClass) throws X { return null; }
            @Override
            public Exception getException() { return null; }
            @Override
            public Task<Void> addOnCompleteListener(OnCompleteListener<Void> onCompleteListener) {
                onCompleteListener.onComplete(this);
                return this;
            }

            @NonNull
            @Override
            public Task<Void> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
                return null;
            }

            @NonNull
            @Override
            public Task<Void> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
                return null;
            }

            @NonNull
            @Override
            public Task<Void> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
                return null;
            }

            @NonNull
            @Override
            public Task<Void> addOnSuccessListener(@NonNull OnSuccessListener<? super Void> onSuccessListener) {
                return null;
            }

            @NonNull
            @Override
            public Task<Void> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super Void> onSuccessListener) {
                return null;
            }

            @NonNull
            @Override
            public Task<Void> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super Void> onSuccessListener) {
                return null;
            }

            public Task<Void> addOnSuccessListener(OnCompleteListener<Void> onCompleteListener) {
                onCompleteListener.onComplete(this);
                return this;
            }

            public Task<Void> addOnFailureListener(OnCompleteListener<Void> onCompleteListener) {
                return this;
            }
        };
    }
}