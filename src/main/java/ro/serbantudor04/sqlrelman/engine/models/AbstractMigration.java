package ro.serbantudor04.sqlrelman.engine.models;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;

public abstract class AbstractMigration {
    protected File workingDir;
    public static final String INFO_FILE_NAME = "info.json";

    protected boolean saveInfoFile(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        try {
            mapper.writeValue(new File(workingDir, INFO_FILE_NAME), this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public abstract void create();
    public abstract void migrate();
    public abstract void delete();
    public abstract void change();


}
