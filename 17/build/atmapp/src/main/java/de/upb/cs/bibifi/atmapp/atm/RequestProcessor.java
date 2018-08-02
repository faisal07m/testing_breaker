package de.upb.cs.bibifi.atmapp.atm;

import de.upb.cs.bibifi.commons.constants.AppConstants;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;
import de.upb.cs.bibifi.commons.enums.RequestType;

public class RequestProcessor {
    public static TransmissionPacket generateRequest(RequestType type, String account, double balance, String pin) {
        TransmissionPacket packet = new TransmissionPacket();
        switch (type) {
            case CREATE:
                packet.setRequestType(type);
                packet.setProperty(AppConstants.KEY_ACCOUNT_NAME, account);
                packet.setProperty(AppConstants.KEY_BALANCE, String.valueOf(balance));
                break;
            case DEPOSIT:
                packet.setRequestType(type);
                packet.setProperty(AppConstants.KEY_ACCOUNT_NAME, account);
                packet.setProperty(AppConstants.KEY_DEPOSITE, String.valueOf(balance));
                packet.setProperty(AppConstants.KEY_PIN, pin);
                break;
            case WITHDRAW:
                packet.setRequestType(type);
                packet.setProperty(AppConstants.KEY_ACCOUNT_NAME, account);
                packet.setProperty(AppConstants.KEY_WIHTDRAW, String.valueOf(balance));
                packet.setProperty(AppConstants.KEY_PIN, pin);
                break;
            case CHECKBALANCE:
                packet.setRequestType(type);
                packet.setProperty(AppConstants.KEY_ACCOUNT_NAME, account);
                packet.setProperty(AppConstants.KEY_PIN, pin);
                break;
        }
        return packet;
    }
}
