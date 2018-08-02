package de.upb.bionicbeaver.atm.validation;

public enum ModeOfOperation {

    CREATE_ACCOUNT("n"),
    DEPOSIT("d"),
    WITHDRAW("w"),
    GET_BALANCE("g");

    private final String option;

    ModeOfOperation(final String o) {
        this.option = o;
    }

    public String toString() {
        return option;
    }

}
