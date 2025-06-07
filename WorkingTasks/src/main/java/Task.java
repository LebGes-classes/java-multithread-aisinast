import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Task implements DataLoader {
    private int id;
    private String title;
    private int totalHours;
    private int completedHours;
    private TaskStatus status;

    // id работника, которому назначена задача; -1, если никому конкретно не назначена
    private int assignedTo;

    private static List<Task> tasks = new ArrayList<>();

    // перечисление возможных статусов задачи
    public enum TaskStatus {
        PENDING,        // Задача ожидает назначения
        IN_PROGRESS,    // Задача в работе
        COMPLETED,      // Задача завершена
    }

    // конструкторы
    public Task(int id, String title, int totalHours, int completedHours, TaskStatus status, int assignedTo) {
        this.id = id;
        this.title = title;
        this.totalHours = totalHours;
        this.completedHours = completedHours;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public Task(int id, String title, int totalHours) {
        this(id, title, totalHours, 0, TaskStatus.PENDING, -1);
    }

    public Task(int id, String title, int totalHours, int assignedTo) {
        this(id, title, totalHours, 0, TaskStatus.PENDING, assignedTo);
    }

    // геттеры
    public int getId() { return id;}
    public String getTitle() { return title; }
    public int getTotalHours() { return totalHours; }
    public int getCompletedHours() { return completedHours; }
    public TaskStatus getStatus() { return status; }
    public int getAssignedTo() { return assignedTo; }

    // метод для считывания объектов из таблицы в список
    public static void loadFromExcel() {
        try (FileInputStream fis = new FileInputStream(Excel.getFilepath())) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheet("tasks");

            Iterator<Row> rowIterator = sheet.iterator();

            // проходимся итератором по строкам
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // строку с заголовками столбцов пропускаем
                if (row.getRowNum() != 0) {
                    // считываем данные
                    int id = (int) row.getCell(0).getNumericCellValue();
                    String title = row.getCell(1).getStringCellValue();
                    int totalHours = (int) row.getCell(2).getNumericCellValue();
                    int completedHours = (int) row.getCell(3).getNumericCellValue();
                    String strStatus = row.getCell(4).getStringCellValue();
                    int assignedTo = (int) row.getCell(5).getNumericCellValue();

                    // определяем статус задачи
                    TaskStatus status;
                    switch (strStatus) {
                        case "PENDING":
                            status = TaskStatus.PENDING;
                            break;
                        case "IN_PROGRESS":
                            status = TaskStatus.IN_PROGRESS;
                            break;
                        case "COMPLETED":
                            status = TaskStatus.COMPLETED;
                            break;
                        default:
                            status = null;
                            break;
                    }

                    // создаем объект и добавляем в список
                    Task task = new Task(id, title, totalHours, completedHours, status, assignedTo);
                    tasks.add(task);
                }
            }

            workbook.close();
        } catch (IOException e) {
            System.err.println("Ошибка при считывании задач из таблицы: " + e.getMessage());
        }
    }
}
