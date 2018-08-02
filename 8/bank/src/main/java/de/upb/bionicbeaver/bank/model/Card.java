package de.upb.bionicbeaver.bank.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Card {
    private final String cardName;
    private final String cardContent;
}
