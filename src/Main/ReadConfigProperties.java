package Main;

import java.io.IOException;
import java.util.Properties;

public class ReadConfigProperties {
  private java.io.FileReader fr = null;
  private java.io.BufferedReader br = null;
  private Properties config = new Properties();

  public ReadConfigProperties(String path) {
    // TODO Auto-generated constructor stub
    try {

      // log.info((new java.io.File("mail.properties")).getAbsoluteFile());
      System.out
          .println("Get config from :" + (new java.io.File(path)).getAbsoluteFile());
      fr = new java.io.FileReader(path);
      br = new java.io.BufferedReader(fr);
      String str;
      while ((str = br.readLine()) != null) {
        if (str.indexOf("=") < 1) {
          continue;
        }
        String[] tmp = str.split("=");
        config.setProperty(tmp[0], tmp[1]);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      config = null;
    } finally {

      try {
        br.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      try {
        fr.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  public Properties get() {
    return this.config;
  }
}
