import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

public class Bot extends TelegramLongPollingBot {

    // имя бота
    String botUsername = "chrono_keeper_bot";

    // токен бота
    String BOT_TOKEN = "8018787420:AAFvwuJbk9N4gCxImHs7bmsoXSoQzL5sa04";

    // метод, возвращающий имя бота
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    // метод, возвращающий токен бота
    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    // основной метод для обработки входящих сообщений
    @Override
    public void onUpdateReceived(Update update) {

        //проверяем, что сообщение содержит текст
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // обработка команды /start
            if (text.equals("/start")) {
                String message = """
                        Отправьте команду:
                        *`/timer X текст`*
                        где `X` — время *в секундах*.
                        Через указанное время я пришлю напоминание!
                        
                        Пример:
                        `/timer 300 Позвонить маме`
                        (напомнит через 5 минут)
                        """;

                sendMessage(chatId, message);

            // обработка команды /timer
            } else if (text.startsWith("/timer")) {

                // создаем новый поток таймера, чтобы не блокировать основной поток бота
                new Thread(() -> {
                    try {
                        // разбираем аргументы команды
                        String[] args = text.substring(7).split(" ");

                        // должен быть хотя бы один аргумент - время
                        if (args.length >= 1) {
                            Iterator<String> iterator = Arrays.stream(args).iterator();

                            // первый аргумент - время в секундах
                            int time = Integer.parseInt(iterator.next());

                            // остальные аргументы - текст напоминания
                            String reminder = "";
                            while (iterator.hasNext()) {
                                reminder += iterator.next() + " ";
                            }

                            // убираем лишние пробелы
                            reminder.trim();

                            // подтверждаем установку таймера
                            sendMessage(chatId, "Таймер установлен на " + time + " секунд!");

                            // засекаем время
                            Thread.sleep(time * 1000L);

                            // отправляем напоминание
                            sendMessage(chatId, "Время вышло: " + reminder);
                        } else {
                            // если аргументов недостаточно, отправляем подсказку
                            sendMessage(chatId, "Используйте: /timer <секунды> <текст>");
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

            }
        }
    }

    // метод для отправки сообщений
    private void sendMessage(long chatId, String message) {
        // создаем объект сообщения
        SendMessage sendMessage = new SendMessage();

        // устанавливаем id чата
        sendMessage.setChatId(chatId);

        // устанавливаем текст сообщения
        sendMessage.setText(message);

        // включаем поддержку markdown
        sendMessage.enableMarkdown(true);

        try {
            // отправляем сообщение
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}
