import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class Excel {
    final static private String FILEPATH = "/Users/mac/code/Java/ИиП/java-multithread-aisinast/" +
            "WorkingTasks/multithread.xlsx";

    public static String getFilepath() {
        return FILEPATH;
    }

    // метод для изменения значения ячейки
    public static <T> void changeCellValue(String sheetName, int dataID, int cellNumber, T newValue) {
        try (FileInputStream fis = new FileInputStream(FILEPATH)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet(sheetName);

            Iterator<Row> rowIterator = sheet.iterator();

            // проходимся итератором по строкам
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // первую строку пропускаем
                if (row.getRowNum() == 0) {
                    continue;
                }

                // когда находим строку с нужным id, меняем значение нужной ячейки
                if (dataID == row.getCell(0).getNumericCellValue()) {
                    if (newValue instanceof Integer) {
                        row.getCell(cellNumber - 1).setCellValue((Integer) newValue);
                    } else if (newValue instanceof String) {
                        row.getCell(cellNumber - 1).setCellValue((String) newValue);
                    } else if (newValue instanceof Task.TaskStatus) {
                        row.getCell(cellNumber - 1).setCellValue(newValue.toString());
                    }
                }
            }

            // записываем изменения
            FileOutputStream fos = new FileOutputStream(FILEPATH);
            workbook.write(fos);

            fos.close();
            workbook.close();
        } catch (IOException e) {
            System.err.println("Ошибка при изменении значения ячейки: " + e.getMessage());
        }
    }
}
