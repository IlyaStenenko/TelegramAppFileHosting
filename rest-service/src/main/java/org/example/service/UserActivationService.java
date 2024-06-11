package org.example.service;

public interface UserActivationService {
    boolean activation(String cryptoUserId);

    boolean login(String cryptoUserId);
}
