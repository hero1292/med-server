package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 64, message = "Пароль должен содержать от 6 до 64 символов")
    private String password;

    @NotBlank(message = "ФИО обязательно")
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank(message = "Роль обязательна (PATIENT или DOCTOR)")
    @Pattern(regexp = "PATIENT|DOCTOR|ADMIN", message = "Недопустимая роль")
    private String role;
}
