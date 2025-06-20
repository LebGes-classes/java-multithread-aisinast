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
import java.util.concurrent.TimeUnit;

public class Employee implements DataLoader, Runnable {
    private int id;
    private String name;
    private int workedHoursToday;
    private int totalWorkingHours;

    private static List<Employee> employees = new ArrayList<>();

    // монитор для потокобезопасности
    private static final Object lock = new Object();

    public Employee(int id, String name, int workedHoursToday, int idleHoursToday) {
        this.id = id;
        this.name = name;
        this.workedHoursToday = workedHoursToday;
        this.totalWorkingHours = idleHoursToday;
    }

    // геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public int getWorkedHoursToday() { return workedHoursToday; }
    public int getTotalWorkingHours() { return totalWorkingHours; }

    public static List<Employee> getEmployees() {
        synchronized (lock) {
            return new ArrayList<>(employees);
        }
    }

    public static void addEmployee(Employee employee) {
        synchronized (lock) {
            employees.add(employee);
        }
    }

    @Override
    public void run() {
        try {
            workDay();
        } catch (InterruptedException e) {
            System.err.println("Ошибка в рабочем дне для " + getName());
        }
    }

    // метод, который симулирует рабочий день конкретного работника
    private void workDay() throws InterruptedException {
        System.out.println(getName() + " начал рабочий день");

        // получаем список задач работника
        List<Task> employeeTask = getTasksList(getId());

        int countHours = 0;

        // проходимся по задачам, пока еще есть задачи и работник работал меньше 8 часов
        Iterator<Task> taskIterator = employeeTask.iterator();
        while (taskIterator.hasNext() && countHours < 8) {
            Task task = taskIterator.next();

            // время задачи - минимум из оставшегося на выполнение время задачи и оставшегося на работу времени
            int taskTime = Math.min(task.getTotalHours() - task.getCompletedHours(), 8 - countHours);
            countHours += taskTime;

            System.out.println(getName() + " работает над задачей \"" + task.getTitle() + "\" (" + taskTime + "ч)");

            // поток "засыпает" на время задачи в секундах, потом completedHours задачи увеличиваются
            TimeUnit.SECONDS.sleep(taskTime);

            synchronized (Task.class) {
                task.setCompletedHours(task.getCompletedHours() + taskTime);

                // если задача выполнена, меняем статус и ставим taskCompletedToday = true
                if (task.getCompletedHours() == task.getTotalHours()) {
                    task.setStatus(Task.TaskStatus.COMPLETED);
                    task.setCompletedToday(true);

                    System.out.println(getName() + " завершил задачу \"" + task.getTitle() + "\"");
                }
                // иначе ставим статус "в процессе выполнения"
                else {
                    task.setStatus(Task.TaskStatus.IN_PROGRESS);
                }
            }
        }

        synchronized (this) {
            this.workedHoursToday = countHours;
        }

        System.out.println(getName() + " завершил рабочий день (отработано " + countHours + " часов)");
    }

    // метод для получения списка задач конкретного работника
    private List<Task> getTasksList(int workerId) {
        // allTasks - список задач всех работников, employeeTask - задачи конкретного работника
        List<Task> allTasks = Task.getTasks();
        List<Task> employeeTasks = new ArrayList<>();

        // проходимся по задачам всех работников
        Iterator<Task> taskIterator = allTasks.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();

            // если задача назначена конкретному работнику и ее статус не "закончен"
            if (task.getAssignedTo() == workerId && Task.TaskStatus.COMPLETED != task.getStatus()) {
                employeeTasks.add(task);
            }
        }

        // возвращаем список задач работника
        return employeeTasks;
    }

    // метод для загрузки данных из экселя в список
    public static void loadFromExcel() {
        try (FileInputStream fis = new FileInputStream(Excel.getFilepath())) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            XSSFSheet sheet = workbook.getSheet("employees");

            Iterator<Row> rowIterator = sheet.iterator();

            // проходимся итератором по строкам таблицы
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // пропускаем строку с заголовками
                if (row.getRowNum() != 0) {
                    // считываем данные
                    int id = (int) row.getCell(0).getNumericCellValue();
                    String name = row.getCell(1).getStringCellValue();
                    int workedHoursToday = (int) row.getCell(2).getNumericCellValue();
                    int idleHoursToday = (int) row.getCell(3).getNumericCellValue();

                    // создаем экземпляр класса и добавляем его в список
                    Employee employee = new Employee(id, name, workedHoursToday, idleHoursToday);
                    addEmployee(employee);
                }
            }

            workbook.close();
        } catch (IOException e) {
            System.err.println("Ошибка при считывании работников из таблицы: " + e.getMessage());
        }
    }

    // метод для получения работника по его id
    private static Employee getById(int id) {
        Iterator<Employee> employeeIterator = employees.iterator();
        while (employeeIterator.hasNext()) {
            Employee employee = employeeIterator.next();

            if (employee.getId() == id) {
                return employee;
            }
        }

        return null;
    }

    public static void rewriteChanges() {
        try (FileInputStream fis = new FileInputStream(Excel.getFilepath())) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet("employees");

            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (row.getRowNum() == 0) {
                    continue;
                }

                Employee employee = getById((int) row.getCell(0).getNumericCellValue());

                row.getCell(2).setCellValue(employee.getWorkedHoursToday());
            }

            try (FileOutputStream fos = new FileOutputStream(Excel.getFilepath())) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
