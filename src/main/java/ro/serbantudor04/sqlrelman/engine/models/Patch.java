package ro.serbantudor04.sqlrelman.engine.models;

import ro.serbantudor04.sqlrelman.config.AppConfig;
import ro.serbantudor04.sqlrelman.config.ConfigManager;

import java.io.File;

public class Patch {
    private String name;
    private String formattedName;
    private String Description;
    private String upSQL;
    private String downSQL;
    private ConfigManager configManager;
    private File releaseDirectory;
    private File patchDirectory;

    public Patch(String name, String description, String upSQL, String downSQL) {
        this.name = name;
        this.Description = description;
        this.upSQL = upSQL;
        this.downSQL = downSQL;
        this.configManager = ConfigManager.getInstance();
    }

    private void formatName(){
        this.formattedName = name.replace(" ", "_").toUpperCase();
    }




}
