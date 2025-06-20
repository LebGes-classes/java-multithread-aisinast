import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Task.loadFromExcel();
        Employee.loadFromExcel();

        // создаем пул потоков
        List<Employee> employees = Employee.getEmployees();
        ExecutorService executor = Executors.newFixedThreadPool(employees.size());

        for (Employee employee : employees) {
            executor.execute(employee);
        }

        // ожидание завершения потоков сотрудников
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("Ошибка при ожидании завершения потоков: " + e);
        }

        Task.rewriteTasks();
        Employee.rewriteChanges();
        Accounting.calculateEfficiency();
    }
}
