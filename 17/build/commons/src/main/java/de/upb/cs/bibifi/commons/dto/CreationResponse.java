package de.upb.cs.bibifi.commons.dto;

public class CreationResponse  extends  Response{

    private final String pin;

    public CreationResponse(String message,int code, String pin){
        super(message, code);
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }
}
