package com.redhat.digkins;

import com.redhat.digkins.services.JobService;
import com.redhat.digkins.services.TriggerBuildService;
import com.redhat.digkins.util.DiggerClientException;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 */
public class Main {

  public static void main(String[] args) throws DiggerClientException, IOException {
    DiggerClient client = DiggerClient.builder()
      .createJobService(new JobService())
      .triggerBuildService(new TriggerBuildService(10000, 100))
      .withAuth("https://jenkins-digger2.osm3.feedhenry.net", "admin", "Vu8ysYH5f2dJiLgL")
      .build();
    //client.createJob("wtr-java-tests2", "https://github.com/wtrocki/helloworld-android-gradle", "master");
    //client.build("wtr-java-tests2");
    InputStream inputStream = client.fetchArtifact("wtr-java-tests2", 1, "app-debug.apk");
    if(inputStream != null){
      File targetFile = new File("artifact.tmp");
      OutputStream outStream = new FileOutputStream(targetFile);

      byte[] buffer = new byte[8 * 1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outStream.write(buffer, 0, bytesRead);
      }
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(outStream);
    }
  }
}
