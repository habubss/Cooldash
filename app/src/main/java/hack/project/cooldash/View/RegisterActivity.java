package hack.project.cooldash.View;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import hack.project.cooldash.databinding.ActivityRegistrationBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegistrationBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mAuth = FirebaseAuth.getInstance();

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();
                String username = binding.etUsername.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Поля не должны быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Пароль должен содержать не менее 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    HashMap<String, String> userInfo = new HashMap<>();
                                    userInfo.put("email", email);
                                    userInfo.put("username", username);

                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Users")
                                            .child(mAuth.getCurrentUser().getUid())
                                            .setValue(userInfo)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(getApplicationContext(), "Ошибка при сохранении данных пользователя", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    String errorMessage = "Ошибка регистрации";
                                    if (task.getException() instanceof FirebaseAuthException) {
                                        errorMessage = getErrorMessage(((FirebaseAuthException) task.getException()).getErrorCode());
                                    }
                                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        binding.textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });
    }

    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                return "Неверный формат email";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "Этот email уже зарегистрирован";
            case "ERROR_WEAK_PASSWORD":
                return "Пароль слишком слабый";
            default:
                return "Ошибка регистрации";
        }
    }
}
