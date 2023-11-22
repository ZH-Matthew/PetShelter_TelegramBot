package com.example.petshelterg2.controller;

import com.example.petshelterg2.config.BotConfig;
import com.example.petshelterg2.model.CatOwners;
import com.example.petshelterg2.model.DogOwners;
import com.example.petshelterg2.model.Probation;
import com.example.petshelterg2.model.Selection;
import com.example.petshelterg2.repository.CatOwnersRepository;
import com.example.petshelterg2.repository.DogOwnersRepository;
import com.example.petshelterg2.repository.SelectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Autowired
    private SelectionRepository selectionRepository;

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
     *
     * @return String (chatID админа/волонтёра)
     */
    public String getBotOwnerId() {
        return config.getOwnerId();
    }


    //реализация основного метода общения с пользователем (главный метод приложения)
    @Override
    public void onUpdateReceived(Update update) {
        //проверка наличия телефона (если он есть проверка на то какую сторону выбрал пользователь, и далее сохранение в БД
        if (update.getMessage().getContact() != null) {         //проверяет есть ли у пользователя контакт, если есть, сохраняет его.
            Boolean selection = selectionRepository.findById(update.getMessage().getChatId()).get().getSelection();
            if (selection) {
                saveDogOwner(update);                           //вызывает метод сохранения пользователя в БД к владельцам собак
                prepareAndSendMessage(update.getMessage().getChatId(), DATA_SAVED);
            } else {
                saveCatOwner(update);                           //вызывает метод сохранения пользователя в БД к владельцам кошек
                prepareAndSendMessage(update.getMessage().getChatId(), DATA_SAVED);
            }
        }
        if (update.getMessage().getPhoto() != null) {
            boolean counter = selectionRepository.findById(update.getMessage().getChatId()).get().getCounter() == 1;
            boolean selection = selectionRepository.findById(update.getMessage().getChatId()).get().getSelection();
            if (counter && !selection) {
                photoShelterThirdCat(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Integer counter = selectionRepository.findById(update.getMessage().getChatId()).get().getCounter();
            boolean selection = selectionRepository.findById(update.getMessage().getChatId()).get().getSelection();
            if (counter != null && !selection) {
                switch (counter) {
                    case 2:
                        dietShelterThirdCat(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    case 3:
                        changesBehaviorShelterThirdCat(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    case 4:
                        saveSelection(update.getMessage().getChatId(), false, 0);
                        mainMenu(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                }
            }
        }
        if (update.getMessage().getPhoto() != null) {
            boolean counter = selectionRepository.findById(update.getMessage().getChatId()).get().getCounter() == 1;
            boolean selection = selectionRepository.findById(update.getMessage().getChatId()).get().getSelection();
            if (counter && selection) {
                photoShelterThirdDog(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
            }
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            Integer counter = selectionRepository.findById(update.getMessage().getChatId()).get().getCounter();
            boolean selection = selectionRepository.findById(update.getMessage().getChatId()).get().getSelection();
            if (counter != null && selection) {
                switch (counter) {
                    case 2:
                        dietShelterThirdDog(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    case 3:
                        changesBehaviorShelterThirdDog(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                    case 4:
                        saveSelection(update.getMessage().getChatId(), true, 0);
                        mainMenu(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
                        break;
                }
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
                case SHELTER_SCHEDULE_BUTTON_CAT:
                    catShelterWork(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case TIPS_DOG_HANDLER_AND_WHY_THEY_MAY_REFUSE_TAKE_ANIMAL:
                    tipsFromDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SHELTER_SCHEDULE_BUTTON_DOG:
                    dogShelterWork(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SECURITY_CONTACTS_BUTTON_CAT:
                    catShelterSecurityContacts(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SECURITY_CONTACTS_BUTTON_DOG:
                    dogShelterSecurityContacts(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RULES_FOR_GETTING_KNOW_CAT:
                    safetyNotesRulesForFirstMetCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RULES_FOR_GETTING_KNOW_DOG:
                    safetyNotesRulesForFirstMetDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case LIST_DOCUMENTS_TAKE_ANIMAL_DOG:
                    listOfDocumentsForAdoption(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case LIST_DOCUMENTS_TAKE_ANIMAL_CAT:
                    listOfDocumentsForAdoption(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_TRANSPORTATION_CAT:
                    transportingRecommendationsCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_TRANSPORTATION_DOG:
                    transportingRecommendationsDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_KITTY:
                    arrangingHomeRecommendationsKitty(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_PUPPY:
                    arrangingHomeRecommendationsPuppy(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_BUTTON2_CAT:
                    arrangingHomeRecommendationsCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_BUTTON2_DOG:
                    arrangingHomeRecommendationsDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_CAT_WITH_DISABILITIES:
                    arrangingHomeRecommendationsDisabledCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_HOME_DOG_WITH_DISABILITIES:
                    arrangingHomeRecommendationsDisabledDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SAFETY_NOTES_BUTTON_CAT:
                    safetyNotesCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SAFETY_NOTES_BUTTON_DOG:
                    safetyNotesDog(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case TIPS_DOG_HANDLER_COMMUNICATE_WITH_DOG:
                    initialDogHandlerAdvice(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case RECOMMENDATIONS_FURTHER_REFERENCE_THEM:
                    dogHandlerRecommendation(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case LIST_OF_REASONS_WHY_THEY_MAY_REFUSE_DOG:
                    refusalReasonsList(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SHELTER_THIRD_STEP_BUTTON_CAT:
                    shelterThirdCat(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case SHELTER_THIRD_STEP_BUTTON_DOG:
                    shelterThirdDog(chatId, update.getMessage().getChat().getFirstName());
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
//        log.info(update.getMessage().getPhoto().toString());
    }

    /**
     * Метод обрабатывающий команду <b>/start</b>
     * <p>
     * Собирает текст ответа и отправляет его в метод: {@link TelegramBot#prepareAndSendMessageAndKeyboard(long, String, ReplyKeyboardMarkup)}
     *
     * @param chatId (ID чата пользователя)
     * @param name   (имя пользователя)
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
     *
     * @param chatId         (ID чата пользователя)
     * @param textToSend     (текст для отправки пользователю)
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
     *
     * @param chatId     (ID чата пользователя)
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
     *
     * @param message (заранее собранный message с chatID пользователя и текстом сообщения)
     *                {@link TelegramApiException} обрабатывается через try/catch внутри метода
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
        saveSelection(chatId, true, 0);                      //сохранили клиента в БД Selection с выбором собак
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void cat(long chatId, String name) {//метод для перехода в кошачий приют, с клавиатурой
        prepareAndSendMessageAndKeyboard(chatId, CAT_SHELTER_SELECT_TEXT, catShelterKeyboard());
        saveSelection(chatId, false, 0);                      //сохранили клиента в БД Selection с выбором кошек
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
        prepareAndSendMessageAndKeyboard(chatId, CAT_TAKING_ANIMAL_FROM_SHELTER, takeAnCatShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void takeAnDog(long chatId, String name) { //переход в меню как взять собаку из приюта
        prepareAndSendMessageAndKeyboard(chatId, DOG_TAKING_ANIMAL_FROM_SHELTER, takeAnDogShelterKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void recommendationsHomeDog(long chatId, String name) { //переход в меню обустройство дома для собаки
        prepareAndSendMessageAndKeyboard(chatId, ARRANGING_HOME_RECOMMENDATIONS, recommendationsHomeDogKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void recommendationsHomeCat(long chatId, String name) { //переход в меню обустройство дома кошки
        prepareAndSendMessageAndKeyboard(chatId, ARRANGING_HOME_RECOMMENDATIONS, recommendationsHomeCatKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void tipsFromDog(long chatId, String name) { //переход в меню советы кинолога и почему могут отказать забрать собаку из приюта
        prepareAndSendMessageAndKeyboard(chatId, TIPS_DOG_HANDLER, tipsFromDogKeyboard());
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void catShelterWork(long chatId, String name) {
        prepareAndSendMessage(chatId, CAT_SHELTER_WORK_SCHEDULE);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void dogShelterWork(long chatId, String name) {
        prepareAndSendMessage(chatId, DOG_SHELTER_WORK_SCHEDULE);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void catShelterSecurityContacts(long chatId, String name) {
        prepareAndSendMessage(chatId, CAT_SHELTER_SECURITY_CONTACTS);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void dogShelterSecurityContacts(long chatId, String name) {
        prepareAndSendMessage(chatId, DOG_SHELTER_SECURITY_CONTACTS);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void safetyNotesDog(long chatId, String name) {
        prepareAndSendMessage(chatId, SAFETY_NOTES_DOG);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void safetyNotesCat(long chatId, String name) {
        prepareAndSendMessage(chatId, SAFETY_NOTES_CAT);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void safetyNotesRulesForFirstMetCat(long chatId, String name) {
        prepareAndSendMessage(chatId, RULES_FOR_FIRST_MET_CAT);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void safetyNotesRulesForFirstMetDog(long chatId, String name) {
        prepareAndSendMessage(chatId, RULES_FOR_FIRST_MET_DOG);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void listOfDocumentsForAdoption(long chatId, String name) {
        prepareAndSendMessage(chatId, LIST_OF_DOCUMENTS_FOR_ADOPTION);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void transportingRecommendationsCat(long chatId, String name) {
        prepareAndSendMessage(chatId, TRANSPORTING_RECOMMENDATIONS_CAT);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void transportingRecommendationsDog(long chatId, String name) {
        prepareAndSendMessage(chatId, TRANSPORTING_RECOMMENDATIONS_DOG);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsKitty(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_KITTY);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsPuppy(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_PUPPY);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsCat(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_CAT);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsDog(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_DOG);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsDisabledCat(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_DISABLED_CAT);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void arrangingHomeRecommendationsDisabledDog(long chatId, String name) {
        prepareAndSendMessage(chatId, ARRANGING_HOME_RECOMMENDATIONS_DISABLED_DOG);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void initialDogHandlerAdvice(long chatId, String name) {
        prepareAndSendMessage(chatId, INITIAL_DOG_HANDLER_ADVICE);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void dogHandlerRecommendation(long chatId, String name) {
        prepareAndSendMessage(chatId, DOG_HANDLER_RECOMMENDATION);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void refusalReasonsList(long chatId, String name) {
        prepareAndSendMessage(chatId, REFUSAL_REASONS_LIST);
        log.info("Replied to user " + name);                     //лог о том что мы ответили пользователю
    }

    private void shelterThirdCat(long chatId, String name) {
        prepareAndSendMessage(chatId, SHELTER_THIRD_STEP_CAT);
        saveSelection(chatId, false, 1);
        log.info("Replied to user " + name);
    }

    private void photoShelterThirdCat(long chatId, String name) {
        prepareAndSendMessage(chatId, DIET_CAT);
        saveSelection(chatId, false, 2);
        log.info("Replied to user " + name);
    }

    private void dietShelterThirdCat(long chatId, String name) {
        prepareAndSendMessage(chatId, WELL_BEING_AND_ADAPTATION_CAT);
        saveSelection(chatId, false, 3);
        log.info("Replied to user " + name);
    }

    private void changesBehaviorShelterThirdCat(long chatId, String name) {
        prepareAndSendMessage(chatId, CHANGES_BEHAVIOR_CAT);
        saveSelection(chatId, false, 4);
        log.info("Replied to user " + name);
    }

    private void shelterThirdDog(long chatId, String name) {
        prepareAndSendMessage(chatId, SHELTER_THIRD_STEP_CAT);
        saveSelection(chatId, true, 1);
        log.info("Replied to user " + name);
    }

    private void photoShelterThirdDog(long chatId, String name) {
        prepareAndSendMessage(chatId, DIET_CAT);
        saveSelection(chatId, true, 2);
        log.info("Replied to user " + name);
    }

    private void dietShelterThirdDog(long chatId, String name) {
        prepareAndSendMessage(chatId, WELL_BEING_AND_ADAPTATION_CAT);
        saveSelection(chatId, true, 3);
        log.info("Replied to user " + name);
    }

    private void changesBehaviorShelterThirdDog(long chatId, String name) {
        prepareAndSendMessage(chatId, CHANGES_BEHAVIOR_CAT);
        saveSelection(chatId, true, 4);
        log.info("Replied to user " + name);
    }

    /**
     * Метод собирает стартовую клавиатуру <p>
     * Реализуя две кнопки на основе: <p>
     * {@link com.example.petshelterg2.constants.Constants#CAT_SHELTER_BUTTON} <p>
     * {@link com.example.petshelterg2.constants.Constants#DOG_SHELTER_BUTTON} <p>
     *
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
     * {@value  com.example.petshelterg2.constants.Constants#CONTACT_WITH_ME_BUTTON_DOG} <p>
     * Является функциональной и запрашивает контакт у пользователя
     *
     * @return <b>ReplyKeyboardMarkup</b>
     */
    private ReplyKeyboardMarkup dogShelterKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(ABOUT_SHELTER_BUTTON_DOG);
        row.add(SHELTER_SECOND_STEP_BUTTON_DOG);
        row.add(SHELTER_THIRD_STEP_BUTTON_DOG);
        keyboardRows.add(row);

        row = new KeyboardRow();

        KeyboardButton keyboardButtonDog = new KeyboardButton();   //создал функциональную кнопку
        keyboardButtonDog.setText(CONTACT_WITH_ME_BUTTON_DOG);                   //добавил в кнопку отображаемый текст
        keyboardButtonDog.setRequestContact(true);                 //добавил в кнопку запрос контакта у пользователя

        row.add(keyboardButtonDog);
        row.add(CALL_VOLUNTEER_BUTTON);
        row.add(MAIN_MAIN);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;

    }

    /**
     * Метод создает клавиатуру для кошачьего приюта <p>
     * Кнопка: <p>
     * {@value  com.example.petshelterg2.constants.Constants#CONTACT_WITH_ME_BUTTON_CAT} <p>
     * Является функциональной и запрашивает контакт у пользователя
     *
     * @return <b>ReplyKeyboardMarkup</b>
     */
    private ReplyKeyboardMarkup catShelterKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(ABOUT_SHELTER_BUTTON_CAT);
        row.add(SHELTER_SECOND_STEP_BUTTON_CAT);
        row.add(SHELTER_THIRD_STEP_BUTTON_CAT);
        keyboardRows.add(row);

        row = new KeyboardRow();

        KeyboardButton keyboardButtonCat = new KeyboardButton();   //создал функциональную кнопку
        keyboardButtonCat.setText(CONTACT_WITH_ME_BUTTON_CAT);                   //добавил в кнопку отображаемый текст
        keyboardButtonCat.setRequestContact(true);                 //добавил в кнопку запрос контакта у пользователя

        row.add(keyboardButtonCat);
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
     * Суть метода: отправить волонтёру в личку ссылку на пользователя чтобы волонтёр законнектил чаты и начал общение)<p>
     * Метод дергает {@link #getBotOwnerId()}
     * Отправляет два сообщения: <p>
     * Одно волонтёру со ссылкой на чат клиента <p>
     * Второе клиенту, с уведомлением о том, что ему скоро напишут
     *
     * @param chatId   (chatID пользователя)
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
     *
     * @param update
     */
    private void showAdminChatId(Update update) {
        Long chatId = update.getMessage().getChatId();
        log.info("ADMIN CHAT_ID: " + chatId);
    }

    /**
     * Метод сохранения пользователя в БД (с кошками):<p>
     * {@link CatOwners}
     *
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
        catOwner.setProbation(Probation.NOT_ASSIGNED);          // указали поле "не назначен" чтобы там не было null
        catOwnersRepository.save(catOwner);
        log.info("contact saved " + catOwner);
    }

    /**
     * Метод сохранения пользователя в БД (с собаками):<p>
     * {@link DogOwners}
     *
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
        dogOwner.setProbation(Probation.NOT_ASSIGNED);              // указали поле "не назначен" чтобы там не было null
        dogOwnersRepository.save(dogOwner);
        log.info("contact saved " + dogOwner);
    }

    private void saveSelection(long chatId, Boolean selection, Integer counter) {
        Selection newSelection = new Selection();
        newSelection.setSelection(selection);
        newSelection.setChatId(chatId);
        newSelection.setCounter(counter);
        selectionRepository.save(newSelection);
    }

    //cron = ("0 0/1 * * * ?") - каждую минуту (для теста)
    //cron = "@daily" - в полночь (для работы)
    //подумать над оптимизацией кода (есть ли смысл и возможность наследования и использования полиморфизма, есть ли смысл сократить количество классов)
    @Scheduled(cron = "@daily")
    private void findProbation() {
        log.info("daily search for probation statuses has begun");

        List<CatOwners> catOwners = catOwnersRepository.findAll(); //собрали всех пользователей по двум БД
        List<DogOwners> dogOwners = dogOwnersRepository.findAll();

        catOwners.forEach(catOwner -> {                                 //прошлись во всем пользователям CAT
            Long chatId = catOwner.getChatId();
            switch (catOwner.getProbation()) {                           //проверили на совпадение статуса испытательного срока
                case FAILED:                                            //не прошел - уведомили, сменили статус на завершенный с провалом
                    prepareAndSendMessage(chatId, FAILED);
                    CatOwners owner1 = catOwnersRepository.findById(chatId).get(); //взяли готового клиента, сменили статус испытательного срока и пересохранили
                    owner1.setProbation(Probation.COMPLETED_FAILED);
                    catOwnersRepository.save(owner1);
                    break;
                case PASSED:                                            //прошел - уведомили, сменили статус на завершенный с успехом
                    prepareAndSendMessage(chatId, PROBATION_PASSED);
                    CatOwners owner2 = catOwnersRepository.findById(chatId).get();
                    owner2.setProbation(Probation.COMPLETED_SUCCESS);
                    catOwnersRepository.save(owner2);
                    break;
                case EXTENDED_14:                                       //уведомили о продлении и сменили статус на "в процессе)
                    prepareAndSendMessage(chatId, EXTENDED_14);
                    CatOwners owner3 = catOwnersRepository.findById(chatId).get();
                    owner3.setProbation(Probation.IN_PROGRESS);
                    catOwnersRepository.save(owner3);
                    //тут нужно поставить логику по добавлению +14 дней к полю времени испытательного срока
                    break;
                case EXTENDED_30:
                    prepareAndSendMessage(chatId, EXTENDED_30);
                    CatOwners owner4 = catOwnersRepository.findById(chatId).get();
                    owner4.setProbation(Probation.IN_PROGRESS);
                    catOwnersRepository.save(owner4);
                    catOwner.setProbation(Probation.IN_PROGRESS);       //тут нужно поставить логику по добавлению +30 дней к полю времени испытательного срока
                    break;
            }
        });

        dogOwners.forEach(dogOwner -> {
            Long chatId = dogOwner.getChatId();
            switch (dogOwner.getProbation()) {
                case FAILED:
                    prepareAndSendMessage(chatId, FAILED);
                    DogOwners owner1 = dogOwnersRepository.findById(chatId).get();
                    owner1.setProbation(Probation.COMPLETED_FAILED);
                    dogOwnersRepository.save(owner1);
                    break;
                case PASSED:
                    prepareAndSendMessage(chatId, PROBATION_PASSED);
                    DogOwners owner2 = dogOwnersRepository.findById(chatId).get();
                    owner2.setProbation(Probation.COMPLETED_SUCCESS);
                    dogOwnersRepository.save(owner2);
                    break;
                case EXTENDED_14:
                    prepareAndSendMessage(chatId, EXTENDED_14);
                    DogOwners owner3 = dogOwnersRepository.findById(chatId).get();
                    owner3.setProbation(Probation.IN_PROGRESS);
                    dogOwnersRepository.save(owner3);
                    break;
                case EXTENDED_30:
                    prepareAndSendMessage(chatId, EXTENDED_30);
                    DogOwners owner4 = dogOwnersRepository.findById(chatId).get();
                    owner4.setProbation(Probation.IN_PROGRESS);
                    dogOwnersRepository.save(owner4);
                    break;
            }
        });
    }
}

