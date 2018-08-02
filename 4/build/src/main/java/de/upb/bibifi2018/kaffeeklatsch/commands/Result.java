package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;

public abstract class Result {
  public abstract JsonObject toJson();
}
