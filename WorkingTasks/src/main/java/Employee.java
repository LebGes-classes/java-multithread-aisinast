import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Employee implements DataLoader {
    private int id;
    private String name;
    private String position;
    private int workHours;

    private static List<Employee> employees = new ArrayList<>();

    // конструктор
    public Employee(int id, String name, String position, int workHours) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.workHours = workHours;
    }

    // геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public int getWorkHours() { return workHours; }

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
                    String position = row.getCell(2).getStringCellValue();
                    int workHours = (int) row.getCell(3).getNumericCellValue();

                    // создаем экземпляр класса и добавляем его в список
                    Employee employee = new Employee(id, name, position, workHours);
                    employees.add(employee);
                }
            }

            workbook.close();
        } catch (IOException e) {
            System.err.println("Ошибка при считывании работников из таблицы: " + e.getMessage());
        }
    }
}
