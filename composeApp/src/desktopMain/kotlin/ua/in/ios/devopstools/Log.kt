package ua.`in`.ios.devopstools

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Рівні логування
 */
enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3)
}

/**
 * Запис у лозі
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

/**
 * Інтерфейс для обробників логів
 */
interface LogHandler {
    fun handleLog(entry: LogEntry)
}

/**
 * Глобальний клас для логування з підтримкою перенаправлення в різні джерела
 */
class Logger private constructor() {
    // Список логів, доступний для UI
    private val _logs = mutableStateListOf<LogEntry>()

    // Безпечний для багатопоточності список обробників
    private val handlers = CopyOnWriteArrayList<LogHandler>()

    // Поточний мінімальний рівень логування
    private var minimumLogLevel = LogLevel.DEBUG

    // Додаємо стандартний обробник, який виводить логи в консоль
    init {
        addHandler(object : LogHandler {
            override fun handleLog(entry: LogEntry) {
                val throwableInfo = if (entry.throwable != null) {
                    "\n${entry.throwable.stackTraceToString()}"
                } else ""

                val formattedMessage = "[${entry.getFormattedTime()}] ${entry.level} ${entry.tag}: ${entry.message}$throwableInfo"

                when (entry.level) {
                    LogLevel.ERROR -> System.err.println(formattedMessage)
                    else -> println(formattedMessage)
                }
            }
        })
    }

    /**
     * Додає новий обробник логів
     */
    fun addHandler(handler: LogHandler) {
        handlers.add(handler)
    }

    /**
     * Видаляє обробник логів
     */
    fun removeHandler(handler: LogHandler) {
        handlers.remove(handler)
    }

    /**
     * Встановлює мінімальний рівень логування
     */
    fun setMinimumLogLevel(level: LogLevel) {
        minimumLogLevel = level
    }

    /**
     * Очищає всі логи
     */
    fun clearLogs() {
        _logs.clear()
    }

    /**
     * Повертає незмінний список всіх логів для відображення в UI
     */
    fun getLogs(): List<LogEntry> = _logs.toList()

    /**
     * Записує повідомлення в лог з рівнем DEBUG
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    /**
     * Записує повідомлення в лог з рівнем INFO
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    /**
     * Записує повідомлення в лог з рівнем WARNING
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARNING, tag, message, throwable)
    }

    /**
     * Записує повідомлення в лог з рівнем ERROR
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /**
     * Загальний метод для логування з будь-яким рівнем
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        // Перевіряємо рівень логування
        if (level.value < minimumLogLevel.value) return

        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )

        // Додаємо запис до списку для відображення в UI
        _logs.add(entry)

        // Обмежуємо розмір буфера логів для UI (наприклад, до 1000 записів)
        if (_logs.size > MAX_LOG_ENTRIES) {
            _logs.removeRange(0, _logs.size - MAX_LOG_ENTRIES)
        }

        // Передаємо запис всім обробникам
        handlers.forEach { it.handleLog(entry) }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 1000

        @Volatile
        private var instance: Logger? = null

        fun getInstance(): Logger {
            return instance ?: synchronized(this) {
                instance ?: Logger().also { instance = it }
            }
        }
    }
}