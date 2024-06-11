package org.example.service.Impl;

import lombok.extern.log4j.Log4j;
import org.example.dao.AppDocumentDAO;
import org.example.dao.AppPhotoDAO;
import org.example.entity.AppDocument;
import org.example.entity.AppPhoto;
import org.example.service.FileServiceTelegram;
import org.example.service.enums.LinkType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.dao.AppUserDAO;
import org.example.dto.MailParams;
import org.example.entity.AppUser;
import org.example.service.AppUserService;
import org.example.utils.CryptoTool;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;
import static org.example.entity.enums.UserState.BASIC_STATE;
import static org.example.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;

@Log4j
@Service
public class AppUserServiceImpl implements AppUserService {
    private final AppUserDAO appUserDAO;
    private final AppPhotoDAO appPhotoDAO;
    private final AppDocumentDAO appDocDAO;
    private final CryptoTool cryptoTool;
    @Value("${service.mail.uri}")
    private String mailServiceUri;

    private final FileServiceTelegram fileServiceTelegram;

    public AppUserServiceImpl(AppUserDAO appUserDAO, AppPhotoDAO appPhotoDAO, AppDocumentDAO appDocDAO, CryptoTool cryptoTool, FileServiceTelegram fileServiceTelegram) {
        this.appUserDAO = appUserDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.appDocDAO = appDocDAO;
        this.cryptoTool = cryptoTool;
        this.fileServiceTelegram = fileServiceTelegram;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()) {
            return "Вы уже зарегистрированы!";
        } else if (appUser.getEmail() != null) {
            return "Вам на почту уже было отправлено письмо. "
                    + "Перейдите по ссылке в письме для подтверждения регистрации.";
        }
        appUser.setState(WAIT_FOR_EMAIL_STATE);
        appUserDAO.save(appUser);
        return "Введите, пожалуйста, ваш email:";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            return "Введите, пожалуйста, корректный email. Для отмены команды введите /cancel";
        }
        var optional = appUserDAO.findByEmail(email);
        if (optional.isEmpty()) {
            appUser.setEmail(email);
            appUser.setState(BASIC_STATE);
            appUser = appUserDAO.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var msg = String.format("Отправка эл. письма на почту %s не удалась.", email);
                log.error(msg);
                appUser.setEmail(null);
                appUserDAO.save(appUser);
                return msg;
            }
            return "Вам на почту было отправлено письмо."
                    + "Перейдите по ссылке в письме для подтверждения регистрации.";
        } else {
            return "Этот email уже используется. Введите корректный email."
                    + " Для отмены команды введите /cancel";
        }
    }

    @Override
    public String myFiles(AppUser appUser) {
        String answer = "Ваши файлы: \n";
        List<AppPhoto> userPhotos = appPhotoDAO.findAppPhotoByUserId(appUser.getTelegramUserId());
        List<AppDocument> userDocument = appDocDAO.findAppDocumentByUserId(appUser.getTelegramUserId());
        answer += "Ваши фото: \n";
        if (userPhotos.size() != 0) {
            for (int i = 0;i < userPhotos.size(); i++) {
                answer += Integer.toString(i + 1) + ". ";
                if (userPhotos.get(i).getPhotoName() != null)
                    answer += userPhotos.get(i).getPhotoName() + "\n";
                else answer += userPhotos.get(i).getTelegramFileId() + "\n";
                answer += fileServiceTelegram.generateLink(userPhotos.get(i).getId(), LinkType.GET_PHOTO) + "\n";
            }
        } else {
            answer += "Пусто \n";
        }
        answer += "Ваши документы: \n";
        if (userDocument.size() != 0) {
            for (int i = 0; i < userDocument.size(); i++) {
                answer += i + 1 + ". ";
                answer += userDocument.get(i).getDocName() + "\n";
                answer += fileServiceTelegram.generateLink(userDocument.get(i).getId(), LinkType.GET_DOC) + "\n";
            }
        } else {
            answer += "Пусто \n";
        }
        return answer;
    }

    @Override
    public String publishedFile(AppUser appUser) {
        return "Какой файл вы хотели бы опубликовать в общий доступ?:\n";
    }

    public String topDownloadsPhoto() {
        String answer = "Топ картинок по скачиваниям:\n";
        List<AppPhoto> topPhotos = appPhotoDAO.findTop5ByPublishedTrueOrderByDownloadsDesc();
        if (topPhotos.size() == 0)
            return answer + " Пока никаких картинок в общем доступе в боте нету.";
        else {
            for (int i = 0; i < topPhotos.size(); i++){
                answer += i + 1 + ". ";
                if (topPhotos.get(i).getPhotoName() != null)
                    answer += topPhotos.get(i).getPhotoName() + " Скачано: " + topPhotos.get(i).getDownloads() + " раз." + "\n";
                else answer += topPhotos.get(i).getTelegramFileId() + " Скачано: " + topPhotos.get(i).getDownloads() + " раз." + "\n";
                answer += fileServiceTelegram.generateLink(topPhotos.get(i).getId(), LinkType.GET_PHOTO) + "\n";
            }
        }
        return answer;
    }

    public String topDownloadsDocument() {
        String answer = "Топ документов по скачиваниям:\n";
        List<AppDocument> topDocuments = appDocDAO.findTop5ByPublishedTrueOrderByDownloadsDesc();
        if (topDocuments.size() == 0)
            return answer + " Пока никаких документов в общем доступе нету.";
        else {
            for (int i = 0; i < topDocuments.size(); i++) {
                answer += i + 1 + ". ";
                answer += topDocuments.get(i).getDocName() + " Скачано: " + topDocuments.get(i).getDownloads() + " раз." + "\n";
                answer += fileServiceTelegram.generateLink(topDocuments.get(i).getId(), LinkType.GET_DOC) + "\n";
            }
        }
        return answer;
    }

    public String findByKeywords() {
        return "Введите ключевые слова через пробел для поиска:";
    }

    @Override
    public String findByKeywords(String keywords) {
        String answer = "По вашему запросу было найдено:\n";
        String[] keywordM = keywords.split(" ");
        List<AppPhoto> foundPhotos = new ArrayList<>();
        List<AppDocument> foundDocuments = new ArrayList<>();

        for (String keyword:keywordM){
            List<AppPhoto> currentFoundPhotos = appPhotoDAO.findByPhotoNameContainingKeyword(keyword);
            List<AppDocument> currentFoundDocuments = appDocDAO.findByDocNameContainingKeyword(keyword);
            foundPhotos.addAll(currentFoundPhotos);
            foundDocuments.addAll(currentFoundDocuments);
        }

        if (foundPhotos.size() == 0 && foundDocuments.size() == 0) {
            return "По вашим ключевым словам ничего не найдено.";
        } else {
            if (foundPhotos.size() != 0) {
                answer += "Картинки по вашему запросу:\n";
                for (int i = 0; i < foundPhotos.size();i++){
                    answer += i + 1 + foundPhotos.get(i).getPhotoName() + "\n";
                    answer += fileServiceTelegram.generateLink(foundPhotos.get(i).getId(),LinkType.GET_PHOTO) + "\n";
                }
            }
            if (foundDocuments.size() != 0) {
                answer += "Документы по вашему запросу:\n";
                for (int i = 0; i < foundDocuments.size(); i++) {
                    answer += i + 1 + foundDocuments.get(i).getDocName() + "\n";
                    answer += fileServiceTelegram.generateLink(foundDocuments.get(i).getId(),LinkType.GET_DOC) + "\n";
                }
            }
        }
        return answer;
    }

    @Override
    public String subscriptions(AppUser appUser) {
        if (appUser.getSubscriptions() == null) {
            return "У вас пока нет подписок, вы можете их добавить с помощью команды /addSubscribe";
        }

        String userSubcribe = appUser.getSubscriptions();

        String answer = "Ваши подписки:\n";
        String[] subscribes = userSubcribe.split(";");
        for (int i = 0; i < subscribes.length; i++) {
            answer += i + 1 + ". "+ subscribes[i] + "\n";
        }

        return answer;
    }

    @Override
    public String viewSubscribe(AppUser appUser) {
        String subscribe = appUser.getSubscriptions();
        if (subscribe == null)
            return "У вас нет подписок.";
        String answer = "Ваши подписки:\n";
        String[] subscribes = subscribe.split(";");
        for (int i = 0; i < subscribes.length; i++) {
            answer += i + 1 + ". " + subscribes[i] + "\n";
        }
        answer += "Напишите имя пользователя, чьи файлы вы хотите посмотреть.\n";
        answer += "(Иначе используйте команду /cancel)";
        return answer;
    }

    @Override
    public String viewSubscribe(AppUser appUser, String subscribe) {
        boolean itUserSubscribe = false;
        String userSubscriptions = appUser.getSubscriptions();
        String[] subscribes = userSubscriptions.split(";");
        for (String currentSubscribe:subscribes) {
            if (currentSubscribe.equals(subscribe))
                itUserSubscribe = true;
        }
        String answer = "Файлы пользователя " + subscribe + ":\n";
        if (itUserSubscribe) {
            AppUser user = appUserDAO.findAppUserByUsername(subscribe).get();
            List<AppPhoto> userPhotos = appPhotoDAO.findAppPhotoByUserId(user.getTelegramUserId());
            List<AppDocument> userDocuments = appDocDAO.findAppDocumentByUserId(user.getTelegramUserId());
            if (userPhotos.size() == 0 && userDocuments.size() == 0) {
                return answer + "У пользователя нет файлов.";
            }
            answer += "Картинки пользователя:\n";
            if (userPhotos.size() != 0) {
                for (int i = 0;i < userPhotos.size(); i++) {
                    answer += Integer.toString(i + 1) + ". ";
                    if (userPhotos.get(i).getPhotoName() != null)
                        answer += userPhotos.get(i).getPhotoName() + "\n";
                    else answer += userPhotos.get(i).getTelegramFileId() + "\n";
                    answer += fileServiceTelegram.generateLink(userPhotos.get(i).getId(), LinkType.GET_PHOTO) + "\n";
                }
            } else {
                answer += "Пусто \n";
            }
            answer += "Документы пользователя: \n";
            if (userDocuments.size() != 0) {
                for (int i = 0; i < userDocuments.size(); i++) {
                    answer += i + 1 + ". ";
                    answer += userDocuments.get(i).getDocName() + "\n";
                    answer += fileServiceTelegram.generateLink(userDocuments.get(i).getId(), LinkType.GET_DOC) + "\n";
                }
            } else {
                answer += "Пусто \n";
            }
        }
        else return "Пользаватель с таким именем в ваших подписках не найден.\n";
        return answer;
    }


    @Override
    public String addSubscribe() {
        return "Укажите имя пользователя для добавлние подписки:\n";
    }

    @Override
    public String addSubscribe(AppUser appUser, String nameSubcribe) {

        if (appUserDAO.findAppUserByUsername(nameSubcribe).isEmpty())
            return "Такого пользователя ненайдено!";
        AppUser user = appUserDAO.findAppUserByUsername(nameSubcribe).get();
        String userNameToSubscribe = appUser.getUsername();
        if (user.getExpectInvitationToSubscribe() == null)
            user.setExpectInvitationToSubscribe(userNameToSubscribe+ ";");
        else user.setExpectInvitationToSubscribe(user.getExpectInvitationToSubscribe() +
                userNameToSubscribe + ";");
        appUserDAO.save(user);
        return "Пользователю отправлен запрос на добавление подписки";
    }



    @Override
    public String expectInvitationToSubscribe(AppUser appUser) {
        String userToSubcsribe = appUser.getExpectInvitationToSubscribe();
        if (userToSubcsribe == null)
            return "У вас не запросов на добавление подписки";

        String answer = "Запросы на добавление подписки на ваши файлы от пользователей:\n";
        String[] subscribe = userToSubcsribe.split(";");
        for (int i = 0; i < subscribe.length; i++) {
            answer += i + 1 + ". " + subscribe[i] + "\n";
        }
        answer += "Укажите имена пользователей через пробел, которым хотите дать доступ к вашим файлам:\n";
        answer += "(если хотите очистить список некого не добавив, напишите \"Очистить\".)\n";
        return answer;
    }

    @Override
    public String expectInvitationToSubscribe(AppUser appUser, String userToAddSubscribe) {
        String userToSubcsribe = appUser.getExpectInvitationToSubscribe();
        String[] userExpect = userToSubcsribe.split(";");
        String[] toSuscribe = userToAddSubscribe.split(" ");
        if (toSuscribe[0].equals("Очистить")) {
            appUser.setExpectInvitationToSubscribe(null);
            appUserDAO.save(appUser);
            return "Список на добавление очищен.";
        }
        String answer = "";
        for (String userNameToSubscribe:toSuscribe) {
            for (int i = 0; i < userExpect.length; i++)
                if (userExpect[i].equals(userNameToSubscribe))
                    userExpect[i] = "null";
            if (appUserDAO.findAppUserByUsername(userNameToSubscribe).isEmpty())
                answer += "Пользователь с именем " + userNameToSubscribe
                        + " не найден, убедитесь в правильности написания имени.\n";
            else {
                AppUser user = appUserDAO.findAppUserByUsername(userNameToSubscribe).get();
                if (user.getSubscriptions() == null)
                    user.setSubscriptions(appUser.getUsername() + ";");
                else user.setSubscriptions(user.getSubscriptions() + appUser.getUsername() + ";");
                appUserDAO.save(user);
                answer += "Пользователь с именем " + userNameToSubscribe + " успешно добавлен\n";

            }
        }
        String newExpect = "";
        for (int i = 0; i < userExpect.length; i++) {
            if (!userExpect[i].equals("null"))
                newExpect += userExpect[i] + ";";
        }
        appUser.setExpectInvitationToSubscribe(newExpect);
        appUserDAO.save(appUser);
        return answer;
    }

    @Override
    public String deleteSubscribe(AppUser appUser) {
        if (appUser.getSubscriptions() == null)
            return "У вас нет подписок на пользователей!\n" +
                    "(используйте /cancel для отмены команды)";
        return "Введите имя пользователя на которого вы хотите удалить подписку:\n" +
                "(если не хотите никого удалять используйте команду /cancel)";
    }

    @Override
    public String deleteSubscribe(AppUser appUser, String userNameToDelete) {
        if (appUser.getSubscriptions() == null)
            return "Извините, но все же у вас нет подписок, операция невозможна!";
        if (appUserDAO.findAppUserByUsername(userNameToDelete).isEmpty())
            return "Такого пользователя ненайдено!";
        String[] appUserSubscribe = appUser.getSubscriptions().split(";");
        boolean isUserSubscribe = false;
        for (int i = 0; i < appUserSubscribe.length; i++) {
            if (appUserSubscribe[i].equals(userNameToDelete)) {
                isUserSubscribe = true;
                appUserSubscribe[i] = "null";
            }
        }
        if (isUserSubscribe) {
            String newUserSubscribe = "";
            for (int i = 0; i < appUserSubscribe.length; i++) {
                if (!appUserSubscribe[i].equals("null"))
                    newUserSubscribe += appUserSubscribe[i] + ";";
            }
            appUser.setSubscriptions(newUserSubscribe);
            appUserDAO.save(appUser);
            return "Подписка на пользователя удалена!";
        } else return "Такого пользователя нет в ваших подписках";
    }

    @Override
    public String publishedFile(AppUser appUser, String fileName) {
        AppPhoto appPhoto = appPhotoDAO.findAppPhotoByPhotoNameAndUserId(fileName,appUser.getTelegramUserId());
        AppDocument appDocument = appDocDAO.findAppDocumentByDocNameAndUserId(fileName,appUser.getTelegramUserId());
        if (appPhoto != null) {
            appPhoto.setPublished(true);
            appPhotoDAO.save(appPhoto);
        } else if (appDocument != null) {
            appDocument.setPublished(true);
            appDocDAO.save(appDocument);
        } else return "Файл с таким именем не найден ! \n";
        return "Файл " + fileName + " опубликован в общий доступ! \n";
    }

    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri,
                HttpMethod.POST,
                request,
                String.class);
    }
}