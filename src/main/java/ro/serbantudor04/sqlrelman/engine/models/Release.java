package ro.serbantudor04.sqlrelman.engine.models;

import org.apache.commons.io.FileUtils;
import ro.serbantudor04.sqlrelman.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class Release extends AbstractMigration{
    private String version;
    private String description;
    private Date date;

    public Release(String version, String description, Date date) {
        this.version = version;
        this.description = description;
        this.date = date;
    }


    @Override
    public void create() {
        System.out.println("Creating release " + version);
        this.workingDir = new File(AppConfig.getInstance().releasesDirectory, version);
        if(!this.workingDir.mkdirs()){
            System.err.println("Failed to create release directory");
            return;
        }
        super.saveInfoFile();
        System.out.println("Release " + version + " created successfully.");

    }

    @Override
    public void migrate() {
        System.out.println("Migrating release " + version);
        //TODO: Implement migration
    }

    @Override
    public void delete() {
        System.out.println("Deleting release " + version);
        try {
            FileUtils.deleteDirectory(workingDir);
        }catch (IOException e){
            System.err.println("Failed to delete release directory");
        }

    }

    @Override
    public void change() {

    }
}
