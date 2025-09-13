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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import hack.project.cooldash.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.etEmail.getText().toString();
                String password = binding.etPassword.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Поля не должны быть пустыми", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthInvalidUserException e) {
                                            // Пользователь не найден или был удален
                                            Toast.makeText(LoginActivity.this, "Пользователь с таким email не найден", Toast.LENGTH_SHORT).show();
                                        } catch (FirebaseAuthInvalidCredentialsException e) {
                                            // Неверный пароль или неверный формат email
                                            if (e.getErrorCode().equals("ERROR_INVALID_EMAIL")) {
                                                Toast.makeText(LoginActivity.this, "Неверный формат email", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Неверный пароль", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(LoginActivity.this, "Ошибка входа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                }
            }
        });

        binding.btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем интент с флагом, указывающим на гостевой режим
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("isGuest", true);
                startActivity(intent);
                finish();
            }
        });

        binding.textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
}
