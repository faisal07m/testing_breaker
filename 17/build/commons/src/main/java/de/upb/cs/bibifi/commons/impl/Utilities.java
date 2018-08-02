package de.upb.cs.bibifi.commons.impl;

import com.google.gson.Gson;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;


public class Utilities {
    public static TransmissionPacket deserializer(String text) {
        Gson gson = new Gson();
        return gson.fromJson(text, TransmissionPacket.class);
    }

    //Json Serializer
    public static String serializer (TransmissionPacket transmissionPacket){
        Gson gson = new Gson();
        return gson.toJson(transmissionPacket);
    }
}
