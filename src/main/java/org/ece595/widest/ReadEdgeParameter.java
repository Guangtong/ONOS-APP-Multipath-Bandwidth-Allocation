package org.ece595.widest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by mininet on 4/6/17.
 */
public class ReadEdgeParameter {
    String fileName;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public ReadEdgeParameter(String fileName) {
        this.fileName = fileName;
    }

    public void readBw() throws IOException {
        String line = null;
        BufferedReader bufferedReader = null;
        try {
            FileReader fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            //read number of switch
            line = bufferedReader.readLine();
            AppComponent.matrixLen = Integer.parseInt(line) + 1;  //id = 1 to matrixLen - 1
            AppComponent.edgeCapacity = new double[AppComponent.matrixLen][AppComponent.matrixLen];

            while((line = bufferedReader.readLine()) != null) {
                String[] splited = line.split("\\s+");
                int id1 = Integer.parseInt(splited[0]);
                int id2 = Integer.parseInt(splited[1]);
                int bw = Integer.parseInt(splited[2]);
                int delay = Integer.parseInt(splited[3]);
                AppComponent.edgeCapacity[id1][id2] = AppComponent.edgeCapacity[id2][id1] = bw;
                log.info("\n=============\nedgeCapacity[{}][{}] = \n{}\n=============\n", id1, id2, bw);

            }

            // Always close files.
            bufferedReader.close();
            fileReader.close();
        }
        catch(IOException e) {
            //log.errPrintln("Error reading file :" + fileName);
            log.warn("Error reading file :" + fileName);
            throw e;
        }
    }
}
