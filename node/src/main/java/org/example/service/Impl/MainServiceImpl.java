package org.example.service.Impl;

import lombok.extern.log4j.Log4j;
import org.example.service.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.example.dao.AppUserDAO;
import org.example.dao.RawDataDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.entity.AppUser;
import org.example.entity.RawData;
import org.example.exceptions.UploadFileException;
import org.example.service.enums.LinkType;
import org.example.service.enums.ServiceCommand;

import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static org.example.service.enums.ServiceCommand.*;

@Log4j
@Service
public class MainServiceImpl implements MainService {
    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileServiceTelegram fileServiceTelegram;
    private final AppUserService appUserService;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO,
                           FileServiceTelegram fileServiceTelegram, AppUserService appUserService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileServiceTelegram = fileServiceTelegram;
        this.appUserService = appUserService;
    }

    @Override
    public void processTextMessage(Update update) {
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if (CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            output = appUserService.setEmail(appUser, text);
        } else {
            log.error("Unknown user state: " + userState);
            output = "Неизвестная ошибка! Введите /cancel и попробуйте снова!";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
        saveRawData(update);
    }

    @Override
    public void processDocMessageTelegram(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try {
            AppDocument doc = fileServiceTelegram.processDoc(update.getMessage());
            String link = fileServiceTelegram.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "Документ успешно загружен! "
                    + "Ссылка для скачивания: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error(ex);
            String error = "К сожалению, загрузка файла не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    @Override
    public void processPhotoMessageTelegram(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);

        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {

        }

        try {
            AppPhoto photo = fileServiceTelegram.processPhoto(update.getMessage());
            String link = fileServiceTelegram.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "Фото успешно загружено! "
                    + "Ссылка для скачивания: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException ex) {
            log.error(ex);
            String error = "К сожалению, загрузка фото не удалась. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Зарегистрируйтесь или активируйте "
                    + "свою учетную запись чтобы иметь доступ к своему аккаунту";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Отмените текущую команду с помощью /cancel для отправки файлов.";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    private void sendAnswer(String output, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd){
        var serviceCommand = ServiceCommand.fromValue(cmd);
        String lastUserMessage = rawDataDAO.findLastEventTextByChatId(appUser.getTelegramUserId());
        if (REGISTRATION.equals(serviceCommand)) {
            return appUserService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Приветствую! Чтобы посмотреть список доступных команд введите /help";
        } else if (MYFILES.equals(serviceCommand)) {
            return appUserService.myFiles(appUser);
        } else if (PUBLISHFILE.equals(serviceCommand)) {
            return appUserService.publishedFile(appUser);
        } else if (TOPDOWNLOADSPHOTO.equals(serviceCommand)){
            return appUserService.topDownloadsPhoto();
        } else if (TOPDOWNLOADSDOCUMENT.equals(serviceCommand)) {
            return appUserService.topDownloadsDocument();
        } else if (FINDBYKEYWORDS.equals(serviceCommand)) {
            return appUserService.findByKeywords();
        } else if (MYSUBSCRIBE.equals(serviceCommand)) {
            return appUserService.viewSubscribe(appUser);
        } else if (ADDSUBSCRIBE.equals(serviceCommand)) {
            return appUserService.addSubscribe();
        } else if (EXPECTEDSUBSCRIBE.equals(serviceCommand)) {
            return appUserService.expectInvitationToSubscribe(appUser);
        } else if (DELETESUBSCRIBE.equals(serviceCommand)) {
            return appUserService.deleteSubscribe(appUser);
        } else if (lastUserMessage.equals("/DeleteSubscribe")) {
            return appUserService.deleteSubscribe(appUser,cmd);
        } else if (lastUserMessage.equals("/PublishFile")) {
            return  appUserService.publishedFile(appUser,cmd);
        } else if (lastUserMessage.equals("/FindByKeywords")){
            return appUserService.findByKeywords(cmd);
        } else if (lastUserMessage.equals("/MySubscribe")) {
            return appUserService.viewSubscribe(appUser, cmd);
        } else if (lastUserMessage.equals("/AddSubscribe")) {
            return appUserService.addSubscribe(appUser, cmd);
        } else if (lastUserMessage.equals("/ExpectedSubscribe")) {
            return appUserService.expectInvitationToSubscribe(appUser, cmd);
        } else {
            return "Неизвестная команда! Чтобы посмотреть список доступных команд введите /help";
        }
    }

    private String help() {
        return "Список доступных команд:\n"
                + "/cancel - отмена выполнения текущей команды;\n"
                + "/registration - регистрация пользователя;\n"
                + "/ListOfMyFiles - показать мои файлы;\n"
                + "/PublishFile - опубликовать один из ваших файлов;\n"
                + "/TopDownloadsPhoto - посмотреть топ опубликованных картинок;\n"
                + "/TopDownloadsDocument - посмотреть топ опубликованных документов;\n"
                + "/FindByKeywords - найти опубликованные файлы по ключевым словам;\n"
                + "/MySubscribe - посмотреть ваши подписки на пользователей;\n"
                + "/AddSubscribe - добавить подписку на пользователя;\n"
                + "/DeleteSubscribe - удалить подписку на пользователя\n"
                + "/ExpectedSubscribe - посмотреть ожидающие на вас подписки.";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Команда отменена!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();
        var optional = appUserDAO.findFirstByTelegramUserId(telegramUser.getId());
        if (optional.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(transientAppUser);
        }
        return optional.get();
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDAO.save(rawData);
    }

}
