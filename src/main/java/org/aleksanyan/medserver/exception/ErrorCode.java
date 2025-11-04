package org.aleksanyan.medserver.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Пользователь с таким email уже существует"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "Недопустимая роль пользователя"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Недействительный refresh токен"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Срок действия refresh токена истёк"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Пользователь не найден"),
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Пациент не найден"),
    DOCTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Врач не найден"),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Профиль пользователя не найден"),
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Рекомендация врача не найдена"),
    SCHEDULE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Запись расписания не найдена"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Доступ запрещён"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
