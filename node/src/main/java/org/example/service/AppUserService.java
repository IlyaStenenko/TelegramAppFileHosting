package org.example.service;

import org.example.entity.AppUser;

public interface AppUserService {
    String registerUser(AppUser appUser);
    String setEmail(AppUser appUser, String email);

    String myFiles(AppUser appUser);

    String publishedFile(AppUser appUser);

    String publishedFile(AppUser appUser, String fileName);

    String topDownloadsPhoto();

    String topDownloadsDocument();

    String findByKeywords();

    String findByKeywords(String keywords);

    String subscriptions(AppUser appUser);

    String viewSubscribe(AppUser appUser);

    String viewSubscribe(AppUser appUser, String subcribe);

    String addSubscribe();

    String addSubscribe(AppUser appUser, String nameSubcribe);

    String expectInvitationToSubscribe(AppUser appUser);

    String expectInvitationToSubscribe(AppUser appUser, String userToAddSubscribe);

    String deleteSubscribe(AppUser appUser);

    String deleteSubscribe(AppUser appUser, String userNameToDelete);
}