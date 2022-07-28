package Main;


import org.apache.log4j.Logger;



public class ETLlogger extends Logger {



  protected ETLlogger(String name) {
    super(name);
    // TODO Auto-generated constructor stub
  }

  private String logFullPath = "";
  private String stage = null;


  private String OKMessage = "Completed.";



  public void putStage(String s) {


    stage = s;
  }

  public void NWrite(String s) {
    String output = stage + ":" + s;

    this.info(output);
    // System.out.println(output);
  }

  public void setOKMessage(String s) {
    OKMessage = s;
  }

  public void NWriteOK() {

    String output = stage + ":" + OKMessage;
    this.info(output);
    // System.out.println(output);
  }

  public void WWrite(String s) {
    String output = stage + ":" + s;

    this.warn(output);
    // System.out.println(output);
  }

  public void EWrite(String s) {
    String output = stage + ":" + s;

    this.error(output);
    // System.out.println(output);
  }

  public void debugWrite(String s) {
    String output = stage + ":" + s;

    this.debug(output);
    // System.out.println(output);
  }



}
