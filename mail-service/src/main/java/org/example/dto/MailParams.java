package org.example.dto;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
public class MailParams {
    private String id;
    private String emailTo;
}