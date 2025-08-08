package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.google.gson.*
import org.jdesktop.swingx.JXTable
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

// Модель даних для завдань
data class TaskData(
    val name: String,
    val status: String,
    val description: String,
    val version: String? = null,
    val category: String? = null
)

// Рендерер для звичайних ячейок з відступами
class PaddedTableCellRenderer : DefaultTableCellRenderer() {
    init {
        // Встановлюємо відступи (top, left, bottom, right)
        border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
        // Додаємо горизонтальне вирівнювання
        horizontalAlignment = SwingConstants.LEFT
    }

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        // Викликаємо батьківський метод
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        // Переконуємося, що border встановлена
        border = BorderFactory.createEmptyBorder(8, 12, 8, 12)

        // Додаткові налаштування за потреби
        if (isSelected) {
            background = table?.selectionBackground ?: Color.BLUE
            foreground = table?.selectionForeground ?: Color.WHITE
        } else {
            background = table?.background ?: Color.WHITE
            foreground = table?.foreground ?: Color.BLACK
        }

        return this
    }
}



// TableModel для JSON даних
class JsonTaskTableModel(private var tasks: List<TaskData> = emptyList()) : AbstractTableModel() {
    private val columnNames = arrayOf("Name", "Status", "Description", "Version", "Actions")

    override fun getRowCount() = tasks.size
    override fun getColumnCount() = columnNames.size
    override fun getColumnName(column: Int) = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val task = tasks[rowIndex]
        return when (columnIndex) {
            0 -> task.name
            1 -> task.status
            2 -> task.description
            3 -> task.version ?: "N/A"
            4 -> "Actions"
            else -> ""
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnIndex == 4
    }

    fun updateTasks(newTasks: List<TaskData>) {
        this.tasks = newTasks
        fireTableDataChanged()
    }

    fun getTaskAt(row: Int): TaskData? {
        return if (row in 0 until tasks.size) tasks[row] else null
    }
}

// Рендерер для кнопок
class ActionButtonRenderer : DefaultTableCellRenderer() {
    private val panel = JPanel(FlowLayout(FlowLayout.CENTER, 2, 2))
    private val installButton = JButton("Install").apply {
        preferredSize = Dimension(70, 25)
        font = font.deriveFont(10f)
        background = Color(0x4CAF50)
        foreground = Color.WHITE
        isFocusPainted = false
    }
    private val updateButton = JButton("Update").apply {
        preferredSize = Dimension(70, 25)
        font = font.deriveFont(10f)
        background = Color(0x2196F3)
        foreground = Color.WHITE
        isFocusPainted = false
    }
    private val removeButton = JButton("Remove").apply {
        preferredSize = Dimension(70, 25)
        font = font.deriveFont(10f)
        background = Color(0xF44336)
        foreground = Color.WHITE
        isFocusPainted = false
    }

    init {
        panel.add(installButton)
        panel.add(updateButton)
        panel.add(removeButton)
        panel.isOpaque = true
        panel.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
    }

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        if (isSelected) {
            panel.background = table?.selectionBackground ?: Color.LIGHT_GRAY
        } else {
            panel.background = table?.background ?: Color.BLACK
        }

        // Умовне відображення кнопок залежно від статусу
        if (table != null && row >= 0) {
            val status = table.getValueAt(row, 2).toString()
            when (status.lowercase()) {
                "not installed" -> {
                    installButton.isEnabled = true
                    updateButton.isEnabled = false
                    removeButton.isEnabled = false
                }
                "installed" -> {
                    installButton.isEnabled = false
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
                "outdated" -> {
                    installButton.isEnabled = false
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
                else -> {
                    installButton.isEnabled = true
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
            }
        }

        return panel
    }
}

// Редактор для кнопок
class ActionButtonEditor(
    private val onInstall: (TaskData) -> Unit,
    private val onUpdate: (TaskData) -> Unit,
    private val onRemove: (TaskData) -> Unit,
    private val tableModel: JsonTaskTableModel
) : DefaultCellEditor(JCheckBox()) {

    private val panel = JPanel(FlowLayout(FlowLayout.CENTER, 2, 2))
    private val installButton = JButton("Install")
    private val updateButton = JButton("Update")
    private val removeButton = JButton("Remove")
    private var currentRow = -1

    init {
        setupButtons()
        panel.add(installButton)
        panel.add(updateButton)
        panel.add(removeButton)
    }

    private fun setupButtons() {
        listOf(installButton, updateButton, removeButton).forEach { button ->
            button.preferredSize = Dimension(70, 25)
            button.font = button.font.deriveFont(10f)
            button.isFocusPainted = false
        }

        installButton.apply {
            background = Color(0x4CAF50)
            foreground = Color.WHITE
            addActionListener {
                handleAction { task -> onInstall(task) }
            }
        }

        updateButton.apply {
            background = Color(0x2196F3)
            foreground = Color.WHITE
            addActionListener {
                handleAction { task -> onUpdate(task) }
            }
        }

        removeButton.apply {
            background = Color(0xF44336)
            foreground = Color.WHITE
            addActionListener {
                handleAction { task -> onRemove(task) }
            }
        }
    }

    private fun handleAction(action: (TaskData) -> Unit) {
        fireEditingStopped()
        tableModel.getTaskAt(currentRow)?.let { task ->
            action(task)
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentRow = row

        // Оновлення стану кнопок
        if (table != null) {
            val status = table.getValueAt(row, 2).toString()
            when (status.lowercase()) {
                "not installed" -> {
                    installButton.isEnabled = true
                    updateButton.isEnabled = false
                    removeButton.isEnabled = false
                }
                "installed" -> {
                    installButton.isEnabled = false
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
                "outdated" -> {
                    installButton.isEnabled = false
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
                else -> {
                    installButton.isEnabled = true
                    updateButton.isEnabled = true
                    removeButton.isEnabled = true
                }
            }
        }

        return panel
    }

    override fun getCellEditorValue(): Any = ""
}

// Головний клас таблиці
class JsonTaskTableComponent {
    private val tableModel = JsonTaskTableModel()
    private val table = JXTable(tableModel)
    private val scrollPane = JScrollPane(table)

    init {
        setupTable()
    }

    private fun setupTable() {
        // Основні налаштування таблиці
        table.apply {
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            rowHeight = 35
            gridColor = Color.gray
            background = Color.lightGray
            foreground = Color.BLACK
            setShowGrid(true,false)

            // Налаштування колонок
            columnModel.apply {
                getColumn(0).preferredWidth = 150 // Name
                getColumn(1).preferredWidth = 100 // Status
                getColumn(2).preferredWidth = 200 // Description
                getColumn(3).preferredWidth = 80  // Version
                getColumn(4).preferredWidth = 220 // Actions
            }

            // Створюємо рендерер з відступами ПІСЛЯ створення таблиці
            val paddedRenderer = PaddedTableCellRenderer()

            // Застосування рендерера з відступами до всіх колонок крім Actions
            for (i in 0 until 4) {
                table.columnModel.getColumn(i).cellRenderer = paddedRenderer
            }


            // Встановлення рендерера і редактора для колонки Actions
            val actionsColumn = columnModel.getColumn(4)
            actionsColumn.cellRenderer = ActionButtonRenderer()
            actionsColumn.cellEditor = ActionButtonEditor(
                onInstall = { task -> handleInstall(task) },
                onUpdate = { task -> handleUpdate(task) },
                onRemove = { task -> handleRemove(task) },
                tableModel = tableModel
            )
        }

        // Налаштування скрол панелі
        scrollPane.apply {
            preferredSize = Dimension(900, 600)
            background = Color.WHITE
        }

        table.revalidate()
        table.repaint()

    }

    fun loadJsonData(jsonFile: File) {
        try {
            val jsonContent = jsonFile.readText()
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

            val tasks = mutableListOf<TaskData>()

            if (jsonObject.has("tasks") && jsonObject.get("tasks").isJsonArray) {
                val tasksArray = jsonObject.getAsJsonArray("tasks")

                for (taskElement in tasksArray) {
                    if (taskElement.isJsonObject) {
                        val taskObj = taskElement.asJsonObject
                        val task = TaskData(
                            name = taskObj.get("name")?.asString ?: "",
                            status = taskObj.get("status")?.asString ?: "Unknown",
                            description = taskObj.get("description")?.asString ?: "",
                            version = taskObj.get("version")?.asString,
                            category = taskObj.get("category")?.asString
                        )
                        tasks.add(task)
                    }
                }
            }

            tableModel.updateTasks(tasks)

        } catch (e: Exception) {
            println("Помилка завантаження JSON: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleInstall(task: TaskData) {
        println("Installing: ${task.name}")
        // Тут буде ваша логіка встановлення
        JOptionPane.showMessageDialog(
            table,
            "Installing ${task.name}...",
            "Install Task",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun handleUpdate(task: TaskData) {
        println("Updating: ${task.name}")
        // Тут буде ваша логіка оновлення
        JOptionPane.showMessageDialog(
            table,
            "Updating ${task.name}...",
            "Update Task",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun handleRemove(task: TaskData) {
        println("Removing: ${task.name}")
        // Тут буде ваша логіка видалення
        val result = JOptionPane.showConfirmDialog(
            table,
            "Are you sure you want to remove ${task.name}?",
            "Remove Task",
            JOptionPane.YES_NO_OPTION
        )

        if (result == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(
                table,
                "Removing ${task.name}...",
                "Remove Task",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    fun getComponent(): JComponent = scrollPane
}

// Compose компонент
@Composable
fun JsonTaskTable() {
    val taskTableComponent = remember { JsonTaskTableComponent() }
    val tasksManager = TasksManager.getInstance()

    LaunchedEffect(Unit) {
        taskTableComponent.loadJsonData(File(tasksManager.tasksFile))
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = { taskTableComponent.getComponent() }
    )
}