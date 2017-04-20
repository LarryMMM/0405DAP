package test;

import com.ezshare.server.FileList;
import com.ezshare.server.ServerList;
import com.ezshare.server.WorkerThread;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Wenhao Zhao
 */
public class WorkerThreadTest {

    private WorkerThread w;
    private List<String> outputJsons = null;
    
    public WorkerThreadTest() {
        try {
            w = new WorkerThread(new Socket(), new FileList(), new ServerList());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void publishSuccess() throws IOException {
        try {
            outputJsons = receptionTest("PublishSuccess");
            checkIndexContentAndDisplay(0, "success");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } 
    }

    @Test
    public void publishRulesBroken() {
        try {
            receptionTest("PublishSuccess");
            /* Same channel and URI but different owner */
            outputJsons = receptionTest("PublishRulesBroken");
            checkIndexContentAndDisplay(0, "cannot publish resource");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } 
    }

    private List<String> receptionTest(String inputFileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/src/test/java/test/jsons/" + inputFileName));
        String json = "";
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            json += line;
            json += '\n';
        }
        
        return w.reception(json);
    }
    
    private void checkIndexContentAndDisplay(int index, String subMessageContent) {
        System.out.println(outputJsons.get(index));
        Assert.assertEquals(true, outputJsons.get(index).contains(subMessageContent));
    }
}
