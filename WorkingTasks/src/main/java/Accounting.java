import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Accounting {
    public static void calculateEfficiency() {
        try (FileInputStream fis = new FileInputStream(Excel.getFilepath())) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);

            int sheetIndex = workbook.getSheetIndex("efficiency");
            workbook.removeSheetAt(sheetIndex);

            Sheet sheet = workbook.createSheet("efficiency");

            Row row = sheet.createRow(0);

            row.createCell(0).setCellValue("employee_id");
            row.createCell(1).setCellValue("name");
            row.createCell(2).setCellValue("kpi");
            row.createCell(3).setCellValue("completedTasks");

            List<Employee> employees = Employee.getEmployees();

            Iterator<Employee> employeeIterator = employees.iterator();
            while (employeeIterator.hasNext()) {
                Employee employee = employeeIterator.next();
                row = sheet.createRow(sheet.getLastRowNum() + 1);

                row.createCell(0).setCellValue(employee.getId());
                row.createCell(1).setCellValue(employee.getName());

                int workedHours = employee.getWorkedHoursToday();
                int totalWorkingHours = employee.getTotalWorkingHours();

                row.createCell(2).setCellValue(((double) workedHours/totalWorkingHours) * 100);
                row.createCell(3).setCellValue(countTasks(employee.getId()));
            }

            try (FileOutputStream fos = new FileOutputStream(Excel.getFilepath())) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int countTasks(int employee_id) {
        List<Task> tasks = Task.getTasks();

        int count = 0;

        Iterator<Task> taskIterator = tasks.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();

            if (task.getAssignedTo() == employee_id && task.getCompletedToday()) {
                count++;
            }
        }

        return count;
    }
}
