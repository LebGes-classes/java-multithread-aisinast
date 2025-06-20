import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Task implements DataLoader {
    private final int id;
    private final String title;
    private final int totalHours;
    private int completedHours;
    private TaskStatus status;
    private final int assignedTo;
    private boolean completedToday;

    private static final Object lock = new Object();

    private static List<Task> tasks = new ArrayList<>();

    // перечисление возможных статусов задачи
    public enum TaskStatus {
        PENDING,        // Задача ожидает назначения
        IN_PROGRESS,    // Задача в работе
        COMPLETED,      // Задача завершена
    }

    // конструкторы
    public Task(int id, String title, int totalHours, int completedHours, TaskStatus status, int assignedTo,
                boolean completedToday) {
        this.id = id;
        this.title = title;
        this.totalHours = totalHours;
        this.completedHours = completedHours;
        this.status = status;
        this.assignedTo = assignedTo;
        this.completedToday = completedToday;
    }

    // геттеры
    public int getId() { return id;}
    public String getTitle() { return title; }
    public int getTotalHours() { return totalHours; }
    public int getCompletedHours() { return completedHours; }
    public TaskStatus getStatus() { return status; }
    public int getAssignedTo() { return assignedTo; }
    public boolean getCompletedToday() { return completedToday; }

    public void setCompletedHours(int completedHours) {
        this.completedHours = completedHours;
    }
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    public void setCompletedToday(boolean completedToday) {
        this.completedToday = completedToday;
    }

    public static ArrayList<Task> getTasks() {
        synchronized (lock) {
            return new ArrayList<>(tasks);
        }
    }

    public static void addTask(Task task) {
        synchronized (lock) {
            tasks.add(task);
        }
    }

    public static void rewriteTasks() {
        try (FileInputStream fis = new FileInputStream(Excel.getFilepath())) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);

            int sheetIndex = workbook.getSheetIndex("tasks");
            workbook.removeSheetAt(sheetIndex);

            Sheet sheet = workbook.createSheet("tasks");

            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("task_id");
            row.createCell(1).setCellValue("title");
            row.createCell(2).setCellValue("total_hours");
            row.createCell(3).setCellValue("completed_hours");
            row.createCell(4).setCellValue("status");
            row.createCell(5).setCellValue("assigned_to");
            row.createCell(6).setCellValue("completed_today");

            Iterator<Task> taskIterator = tasks.iterator();
            while (taskIterator.hasNext()) {
                Task task = taskIterator.next();

                if (task.getStatus() == TaskStatus.COMPLETED && !task.getCompletedToday()) {
                    continue;
                }

                row = sheet.createRow(sheet.getLastRowNum() + 1);

                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getTotalHours());
                row.createCell(3).setCellValue(task.getCompletedHours());
                row.createCell(4).setCellValue(task.getStatus().toString());
                row.createCell(5).setCellValue(task.getAssignedTo());
                row.createCell(6).setCellValue(task.getCompletedToday());
            }

            try (FileOutputStream fos = new FileOutputStream(Excel.getFilepath())) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при перезаписи таблицы задач: " + e);
        }
    }

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
                    /*
                        запуск программы - симуляция одного рабочего дня, поэтому, если в таблице completedToday - ложь,
                        значит задача не выполнялась полностью никогда, оставляем false, если истина, то задача была
                        выполнена днем ранее => данные completedToday уже не актуальны - тоже ставим false
                     */
                    Task task = new Task(id, title, totalHours, completedHours, status, assignedTo, false);
                    addTask(task);
                }
            }

            workbook.close();
        } catch (IOException e) {
            System.err.println("Ошибка при считывании задач из таблицы: " + e.getMessage());
        }
    }
}
