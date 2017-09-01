package io.openems.impl.device.simulator;

/**
 * Created by maxo2 on 30.08.2017.
 */
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSVLoadGenerator implements LoadGenerator {

    private String filepath = "";
    private String columnKey = "";
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private List<String> values = new ArrayList<>(0);
    private int count = 1;
    private int columnPart = 0;
    private String separator = "";

    public CSVLoadGenerator(JsonObject config) {
        super();
        this.filepath = config.get("filepath").getAsString();
        this.columnKey = config.get("columnKey").getAsString();
        log.error(this.filepath);
        log.error(this.columnKey);

        try {
            BufferedReader br = new BufferedReader(new FileReader(this.filepath));
            values = br.lines().collect(Collectors.toList());
            String[] str = values.get(0).split("=");
            separator = str[str.length - 1];
            str = values.get(1).split(separator);
            for (int i = 0; i < str.length; i++){
                if (str[i].equals(columnKey)){
                    columnPart = i;
                }
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }

    public CSVLoadGenerator() {}


    public long getLoad() {
        long value = 0;
        try {
            count++;
            String[] str = values.get(count).split(separator);
            value = (long) Double.parseDouble(str[columnPart]);
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return value;
    }

}

