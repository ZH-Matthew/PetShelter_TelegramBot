package com.example.petshelterg2.controller;

import com.example.petshelterg2.config.BotConfig;
import com.example.petshelterg2.model.CatOwners;
import com.example.petshelterg2.model.DogOwners;
import com.example.petshelterg2.repository.CatOwnersRepository;
import com.example.petshelterg2.repository.DogOwnersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import static com.example.petshelterg2.constants.Constants.*;

@Slf4j //из библиотеки lombok реализует логирование через переменную log.
@Component //аннотация позволяет автоматически создать экземпляр
public class TelegramBot extends TelegramLongPollingBot {  //есть еще класс WebHookBot (разница в том что WebHook уведомляет нас каждый раз при написании сообщения пользователе, LongPolling сам проверяет не написали ли ему (он более простой)

    @Autowired
    final BotConfig config;

    @Autowired
    private DogOwnersRepository dogOwnersRepository;

    @Autowired
    private CatOwnersRepository catOwnersRepository;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    //реализация метода LongPooling
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    //реализация метода LongPooling
    @Override
    public String getBotToken() {
        return config.getToken();
    }

    /**
     * Единственная задача метода: <p>
     * Запросить из {@link BotConfig} значение <b>ownerId</b>
     * @return String (chatID админа/волонтёра)
     */
    public String getBotOwnerId() {
        return config.getOwnerId();
    }

    public boolean choosingAShelter; //переменная выбора приюта кошки - false (0) , собаки true (1)

    //реализация основного метода общения с пользователем (главный метод приложения)
    @Override
    public void onUpdateReceived(Update update) {
        //проверка наличия телефона (если он есть проверка на то какую сторону выбрал пользователь, и далее сохранение в БД
        if (update.getMessage().getContact() != null) {         //проверяет есть ли у пользователя контакт, если есть, сохраняет его.
            if (choosingAShelter) {
                saveDogOwner(update);                           //вызывает метод сохранения пользователя в БД к владельцам собак
            } else {
                saveCatOwner(update);                           //вызывает метод сохранения пользователя в БД к владельцам кошек
            }
        }

        if (update.hasMessage() && update.getMessage().hasText()) { //проверяем что сообщение пришло и там есть текст
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommand(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case MAIN_MAIN:
                    mainMenu(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case CAT_SHELTER_BUTTON:
                    cat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case DOG_SHELTER_BUTTON:
                    dog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case ABOUT_SHELTER_BUTTON_CAT:
                    informationCatShelter(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case ABOUT_SHELTER_BUTTON_DOG:
                    informationDogShelter(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SHELTER_SECOND_STEP_BUTTON_CAT:
                    takeAnCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SHELTER_SECOND_STEP_BUTTON_DOG:
                    takeAnDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_BUTTON1_CAT:
                    recommendationsHomeCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_BUTTON1_DOG:
                    recommendationsHomeDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case TIPS_DOG_HANDLER_AND_WHY_THEY_MAY_REFUSE_TAKE_ANIMAL:
                    tipsFromDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case CALL_VOLUNTEER_BUTTON:
                    callAVolunteer(chatId, update.getMessage().getChat().getUserName());
                    break;
                case SAVE_ADMIN: //показывает CHAT_ID в логи консоли (никуда не сохраняет данные)
                    showAdminChatId(update);
                    break;
                default:
                    prepareAndSendMessage(chatId, "Я пока не знаю как на это ответить!");
            }
        }
    }

    /**
     * Метод обрабатывающий команду <b>/start</b>
     * <p>
     * Собирает текст ответа и отправляет его в метод: {@link TelegramBot#prepareAndSendMessageAndKeyboard(long, String, ReplyKeyboardMarkup)}
     * @param chatId (ID чата пользователя)
     * @param name (имя пользователя)
     */
    private void startCommand(long chatId, String name) {
        // добавление смайликов в строку (на сайте эмоджипедиа, либо можно зайти в телегу и навести на смайлик, он выдаст код)
        String answer = String.format(GREETING_PLUS_SELECT_SHELTER_TEXT_START, name);
        prepareAndSendMessageAndKeyboard(chatId, answer, startKeyboard());                    // вызываем метод подготовки сообщения
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }


    /**
     * Метод подготовки и отправки сообщения пользователю
     * <b><u>Вместе с клавиатурой!</u></b> <p>
     * Собирает сообщение вместе с клавиатурой и дергает метод отправки: {@link TelegramBot#executeMessage(SendMessage)}
     * @param chatId (ID чата пользователя)
     * @param textToSend (текст для отправки пользователю)
     * @param keyboardMarkup (клавиатура)
     */
    private void prepareAndSendMessageAndKeyboard(long chatId, String textToSend, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId)); //!!! chatID на входе всегда Long, а на выходе всегда String
        message.setText(textToSend);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message); //вызываем метод отправки сообщения
    }


    /**
     * Метод подготовки и отправки сообщения пользователю <p>
     * <b><u>Без клавиатуры!</u></b> <p>
     * Собирает сообщение и дергает метод отправки: {@link TelegramBot#executeMessage(SendMessage)}
     * @param chatId (ID чата пользователя)
     * @param textToSend (текст для отправки пользователю)
     */
    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId)); //!!! chatID на входе всегда Long, а на выходе всегда String
        message.setText(textToSend);
        executeMessage(message); //вызываем метод отправки сообщения
    }

    /**
     * Точечный метод отправки сообщения <p>
     * Главная задача метода: принять собранный message и отправить его клиенту
     * @param message (заранее собранный message с chatID пользователя и текстом сообщения)
     * {@link TelegramApiException} обрабатывается через try/catch внутри метода
     */
    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void mainMenu(long chatId, String name) { //метод отправки главного меню
        String answer = String.format(GREETING_PLUS_SELECT_SHELTER_TEXT, name);
        prepareAndSendMessageAndKeyboard(chatId, answer, startKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void dog(long chatId, String name) {//метод для перехода в собачий приют, с клавиатурой
        prepareAndSendMessageAndKeyboard(chatId, DOG_SHELTER_SELECT_TEXT, dogShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void cat(long chatId, String name) {//метод для перехода в кошачий приют, с клавиатурой
        prepareAndSendMessageAndKeyboard(chatId, CAT_SHELTER_SELECT_TEXT, catShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void informationCatShelter(long chatId, String name) {//метод для перехода в информацию о кошачьем приюте, с клавиатурой
        prepareAndSendMessageAndKeyboard(chatId, ABOUT_CAT_SHELTER_TEXT, informationCatShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void informationDogShelter(long chatId, String name) {//метод для перехода в информацию о собачьем приюте, с клавиатурой
        prepareAndSendMessageAndKeyboard(chatId, ABOUT_DOG_SHELTER_TEXT, informationDogShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void takeAnCat(long chatId, String name) { //переход в меню как взять кошку из приюта
        prepareAndSendMessageAndKeyboard(chatId, SHELTER_SECOND_STEP_BUTTON_CAT, takeAnCatShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void takeAnDog(long chatId, String name) { //переход в меню как взять собаку из приюта
        prepareAndSendMessageAndKeyboard(chatId, SHELTER_SECOND_STEP_BUTTON_DOG, takeAnDogShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void recommendationsHomeDog(long chatId, String name) { //переход в меню обустройство дома для собаки
        prepareAndSendMessageAndKeyboard(chatId, RECOMMENDATIONS_HOME_BUTTON2_DOG, recommendationsHomeDogKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void recommendationsHomeCat(long chatId, String name) { //переход в меню обустройство дома кошки
        prepareAndSendMessageAndKeyboard(chatId, RECOMMENDATIONS_HOME_BUTTON2_CAT, recommendationsHomeCatKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void tipsFromDog(long chatId, String name) { //переход в меню советы кинолога и почему могут отказать забрать собаку из приюта
        prepareAndSendMessageAndKeyboard(chatId, TIPS_DOG_HANDLER_AND_WHY_THEY_MAY_REFUSE_TAKE_ANIMAL, tipsFromDogKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    /**
     * Метод собирает стартовую клавиатуру <p>
     * Реализуя две кнопки на основе: <p>
     * {@link com.example.petshelterg2.constants.Constants#CAT_SHELTER_BUTTON} <p>
     * {@link com.example.petshelterg2.constants.Constants#DOG_SHELTER_BUTTON} <p>
     * @return <b>ReplyKeyboardMarkup</b> (собранная клавиатура)
     */
    private ReplyKeyboardMarkup startKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(); //создание клавиатуры
        List<KeyboardRow> keyboardRows = new ArrayList<>();             //создание рядов в клавиатуре

        KeyboardRow row = new KeyboardRow();                            //первый ряд клавиатуры
        row.add(CAT_SHELTER_BUTTON);                                    //добавление кнопок (слева будут первые созданные)
        row.add(DOG_SHELTER_BUTTON);
        keyboardRows.add(row);                                          //добавляем в клавиатуру ряд
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    /**
     * Метод создает клавиатуру для собачено приюта <p>
     * Кнопка: <p>
     * {@value  com.example.petshelterg2.constants.Constants#CONTACT_WITH_ME_BUTTON} <p>
     * Является функциональной и запрашивает контакт у пользователя
     * @return <b>ReplyKeyboardMarkup</b>
     */
    private ReplyKeyboardMarkup dogShelterKeyboard() {
        choosingAShelter = true;
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(ABOUT_SHELTER_BUTTON_DOG);
        row.add(SHELTER_SECOND_STEP_BUTTON_DOG);
        row.add(SHELTER_THIRD_STEP_BUTTON_DOG);
        keyboardRows.add(row);

        row = new KeyboardRow();
        KeyboardButton keyboardButtonDog = new KeyboardButton();   //создал функциональную кнопку
        keyboardButtonDog.setText(CONTACT_WITH_ME_BUTTON);         //добавил в кнопку отображаемый текст
        keyboardButtonDog.setRequestContact(true);                 //добавил в кнопку запрос контакта у пользователя
        row.add(keyboardButtonDog);                                //добавил кнопку в клавиатуру
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    /**
     * Метод создает клавиатуру для кошачьего приюта <p>
     * Кнопка: <p>
     * {@value  com.example.petshelterg2.constants.Constants#CONTACT_WITH_ME_BUTTON} <p>
     * Является функциональной и запрашивает контакт у пользователя
     * @return <b>ReplyKeyboardMarkup</b>
     */
    private ReplyKeyboardMarkup catShelterKeyboard() {
        choosingAShelter = false;
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(ABOUT_SHELTER_BUTTON_CAT);
        row.add(SHELTER_SECOND_STEP_BUTTON_CAT);
        row.add(SHELTER_THIRD_STEP_BUTTON_CAT);
        keyboardRows.add(row);

        row = new KeyboardRow();
        KeyboardButton keyboardButtonCat = new KeyboardButton();   //создал функциональную кнопку
        keyboardButtonCat.setText(CONTACT_WITH_ME_BUTTON);         //добавил в кнопку отображаемый текст
        keyboardButtonCat.setRequestContact(true);                 //добавил в кнопку запрос контакта у пользователя
        row.add(keyboardButtonCat);                                //добавил кнопку в клавиатуру
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup informationCatShelterKeyboard() {//клавиатура с информацией о кошачьем приюте
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(SHELTER_SCHEDULE_BUTTON_CAT);
        row.add(SECURITY_CONTACTS_BUTTON_CAT);
        row.add(SAFETY_NOTES_BUTTON_CAT);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup informationDogShelterKeyboard() {//клавиатура с информацией о собачьем приюте
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(SHELTER_SCHEDULE_BUTTON_DOG);
        row.add(SECURITY_CONTACTS_BUTTON_DOG);
        row.add(SAFETY_NOTES_BUTTON_DOG);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup takeAnDogShelterKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(RULES_FOR_GETTING_KNOW_DOG);
        row.add(LIST_DOCUMENTS_TAKE_ANIMAL_DOG);
        row.add(RECOMMENDATIONS_TRANSPORTATION_DOG);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(RECOMMENDATIONS_HOME_BUTTON1_DOG);
        row.add(TIPS_DOG_HANDLER_AND_WHY_THEY_MAY_REFUSE_TAKE_ANIMAL);
        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup takeAnCatShelterKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(RULES_FOR_GETTING_KNOW_CAT);
        row.add(LIST_DOCUMENTS_TAKE_ANIMAL_CAT);
        row.add(RECOMMENDATIONS_TRANSPORTATION_CAT);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(RECOMMENDATIONS_HOME_BUTTON1_CAT);
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup recommendationsHomeDogKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(RECOMMENDATIONS_HOME_BUTTON2_DOG);
        row.add(RECOMMENDATIONS_HOME_PUPPY);
        row.add(RECOMMENDATIONS_HOME_DOG_WITH_DISABILITIES);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup recommendationsHomeCatKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(RECOMMENDATIONS_HOME_BUTTON2_CAT);
        row.add(RECOMMENDATIONS_HOME_KITTY);
        row.add(RECOMMENDATIONS_HOME_CAT_WITH_DISABILITIES);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private ReplyKeyboardMarkup tipsFromDogKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(TIPS_DOG_HANDLER_COMMUNICATE_WITH_DOG);
        row.add(RECOMMENDATIONS_FURTHER_REFERENCE_THEM);
        row.add(LIST_OF_REASONS_WHY_THEY_MAY_REFUSE_DOG);
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    /**
     * Метод для вызова волонтера <p>
     * Cуть метода: отправить волонтёру в личку ссылку на пользователя чтобы волонтёр законнектил чаты и начал общение)<p>
     * Метод дергает {@link #getBotOwnerId()}
     * Отправляет два сообщения: <p>
     * Одно волонтёру со ссылкой на чат клиента <p>
     * Второе клиенту, с уведомлением о том , что ему скоро напишут
     * @param chatId (chatID пользователя)
     * @param userName (никнейм пользователя)
     */
    private void callAVolunteer(long chatId, String userName) {
        SendMessage messageVolunteer = new SendMessage();
        SendMessage messageUser = new SendMessage();                    //создаёт два сообщения, одно волонтеру, другое пользователю

        messageVolunteer.setChatId(getBotOwnerId());
        messageVolunteer.setText(VOLUNTEER_MESSAGE + userName);         //формируем сообщение для волонтёра
        messageUser.setChatId(String.valueOf(chatId));
        messageUser.setText(VOLUNTEER_WILL_WRITE_TO_YOU);               //заполняю сообщение пользователю (чтобы он был вкурсе что его сообщение обработано)

        executeMessage(messageVolunteer);                               //отправляем сообщение контактными данными пользователя в личку волонтёру
        executeMessage(messageUser);
    }

    /**
     * Технический метод не для пользователей <p>
     * Метод выводит в лог консоли ChatId админа, если была написана команда "сохранить админа" <p>
     * После этого из лога можно сохранить ChatId в application.properties
     * @param update
     */
    private void showAdminChatId(Update update) {
        Long chatId = update.getMessage().getChatId();
        log.info("ADMIN CHAT_ID: " + chatId);
    }

    /**
     * Метод сохранения пользователя в БД (с кошками):<p>
     * {@link CatOwners}
     * @param update
     */
    private void saveCatOwner(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();
        String lastName = update.getMessage().getChat().getLastName();
        String userName = update.getMessage().getChat().getUserName();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();
        java.time.LocalDateTime currentDateTime = java.time.LocalDateTime.now();
        String status = "необходимо связаться";

        CatOwners catOwner = new CatOwners();
        catOwner.setUserName(userName);
        catOwner.setChatId(chatId);
        catOwner.setFirstName(firstName);
        catOwner.setLastName(lastName);
        catOwner.setPhoneNumber(phoneNumber);
        catOwner.setDateTime(currentDateTime);
        catOwner.setStatus(status);
        catOwnersRepository.save(catOwner);
        log.info("contact saved " + catOwner);
    }

    /**
     * Метод сохранения пользователя в БД (с собаками):<p>
     * {@link DogOwners}
     * @param update
     */
    private void saveDogOwner(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();
        String lastName = update.getMessage().getChat().getLastName();
        String userName = update.getMessage().getChat().getUserName();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();
        java.time.LocalDateTime currentDateTime = java.time.LocalDateTime.now();
        String status = "необходимо связаться";

        DogOwners dogOwner = new DogOwners();
        dogOwner.setUserName(userName);
        dogOwner.setChatId(chatId);
        dogOwner.setFirstName(firstName);
        dogOwner.setLastName(lastName);
        dogOwner.setPhoneNumber(phoneNumber);
        dogOwner.setDateTime(currentDateTime);
        dogOwner.setStatus(status);
        dogOwnersRepository.save(dogOwner);
        log.info("contact saved " + dogOwner);
    }
}
