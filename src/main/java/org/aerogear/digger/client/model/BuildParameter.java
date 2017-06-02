package org.aerogear.digger.client.model;

public class BuildParameter {
  public String name;
  public String description;
  public String defaultValue;

  public BuildParameter(String name, String description, String defaultValue) {
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
  }
}
