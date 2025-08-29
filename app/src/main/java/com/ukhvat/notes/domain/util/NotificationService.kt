package com.ukhvat.notes.domain.util

import com.ukhvat.notes.domain.model.Note

/**
 * Интерфейс сервиса для работы с уведомлениями и quick actions
 */
interface NotificationService {

    /**
     * Показывает уведомление в шторке с quick actions для создания заметки
     */
    suspend fun showQuickNoteNotification()

    /**
     * Скрывает уведомление в шторке
     */
    suspend fun hideQuickNoteNotification()

    /**
     * Обновляет уведомление в шторке (например, счетчик заметок)
     */
    suspend fun updateQuickNoteNotification()

    /**
     * Обрабатывает действие из quick action уведомления
     * @param actionId идентификатор действия
     * @param inputText текст, введенный пользователем (если поддерживается)
     */
    suspend fun handleQuickAction(actionId: String, inputText: String? = null): Note?

    /**
     * Проверяет, включена ли функция quick note
     */
    fun isQuickNoteEnabled(): Boolean

    /**
     * Включает/отключает функцию quick note
     */
    suspend fun setQuickNoteEnabled(enabled: Boolean)
}
