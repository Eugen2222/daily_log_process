package Main;

/**
 * 
 */
import java.util.Date;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.util.MailSSLSocketFactory;

/**
 * @author user01
 *
 */
public class EmailModel {

  /**
   * 
   */

  // TODO Auto-generated constructor stub


  private String content;


  private String subject;
  private ArrayList<InternetAddress> to;
  private Properties p;
  private String attachmentName;
  private String attachmentLocat;

  public EmailModel(String subject, String content, Properties p) {
    this.to = new ArrayList<InternetAddress>();
    this.content = content;
    this.p = p;
    this.attachmentName = attachmentName;
    this.attachmentLocat = attachmentLocat;

    String[] emailList = p.getProperty("emailList").split(",");
    for (String s : emailList) {
      try {
        this.to.add(new InternetAddress(s.trim()));
      } catch (AddressException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }



    this.subject = subject;
  }

  public EmailModel(String subject, String content, Properties p, String attachmentLocat,
      String attachmentName) {
    this.to = new ArrayList<InternetAddress>();
    this.content = content;
    this.p = p;
    this.attachmentName = attachmentName;
    this.attachmentLocat = attachmentLocat;

    String[] emailList = p.getProperty("emailList").split(",");
    for (String s : emailList) {
      try {
        this.to.add(new InternetAddress(s.trim()));
      } catch (AddressException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }



    this.subject = subject;
  }


  public void run()
      throws GeneralSecurityException, UnsupportedEncodingException, MessagingException {

    // 設定傳送基本資訊
    String host = p.getProperty("host");

    String from = p.getProperty("from");


    String user = p.getProperty("user");
    String pwd = p.getProperty("pwd");
    String port = p.getProperty("port");
    String encoding = p.getProperty("emailEncoding");
    String enableAuth = p.getProperty("auth");
    Properties props = System.getProperties();



    // setup SMTP server

    props.put("mail.smtp.host", host);
    props.put("mail.smtp.auth", enableAuth);
    props.put("mail.smtp.port", port);
    // props.put("mail.smtp.starttls.enable", true);
    // use ssl or tls
    Authenticator auth = null;
    Session mailSession = null;
    if (enableAuth.equals("true")) {
      MailSSLSocketFactory sf = new MailSSLSocketFactory();
      sf.setTrustAllHosts(true);
      props.put("mail.imap.ssl.trust", "*");
      props.put("mail.imap.ssl.socketFactory", sf);
      // auth
      auth = new SMTPAuthenticator(user, pwd);
      mailSession = Session.getInstance(props, auth);
    }
    // setup session
    else {
      mailSession = Session.getInstance(props);
    }


    // set a message

    /*
     * Message mailMessage = new MimeMessage(mailSession); // Set from email address
     * mailMessage.setFrom(new InternetAddress(from)); // Set to mail address
     * mailMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to)); // set title
     * mailMessage.setSubject("Hello JavaMail"); // set content
     * mailMessage.setText("Wellcome to  JavaMail...加油!!");
     */


    MimeMessage message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress(from, p.getProperty("fromName", encoding)));
    // System.out.println(p.getProperty("fromName","utf-8"));
    /*
     * new InternetAddress( to)
     */
    for (int i = 0; i < to.size(); i++)
      message.addRecipient(Message.RecipientType.TO, to.get(i));

    message.setSubject(this.subject);

    message.setSentDate(new Date());
    // multipart add a body
    Multipart multipart = new MimeMultipart();

    // 設定內容本文
    BodyPart contentPart = new MimeBodyPart();
    // Multi
//    contentPart.setContent(content,"text/plain;charset="+encoding);// 給BodyPart對像設置內容和格式/編碼方式
    contentPart.setText(content);
    // contentPart.setText(content);
    multipart.addBodyPart(contentPart);

    // add attachment
    if (attachmentLocat != null) {
      BodyPart attachment = new MimeBodyPart();
      DataSource source = new FileDataSource(this.attachmentLocat);
      // add attachment content
      // messageBodyPart.setDataHandler(new DataHandler(source));
      // 附件標題
      // use base64
      attachment.setDataHandler(new DataHandler(source));
      attachment.setFileName(this.attachmentName);
      multipart.addBodyPart(attachment);
    }
    // put multipart into message
    message.setContent(multipart);
    // save message
    message.saveChanges();


    Transport transport = mailSession.getTransport("smtp");
    // 傳送
    transport.connect(host, user, pwd);
    transport.sendMessage(message, message.getAllRecipients());
    transport.close();

    // Transport.send(mailMessage);

    // System.out.println("\n Mail was sent successfully.");


  }

  private class SMTPAuthenticator extends javax.mail.Authenticator {
    private String SMTP_AUTH_PWD;
    private String SMTP_AUTH_USER;

    public SMTPAuthenticator(String SMTP_AUTH_USER, String SMTP_AUTH_PWD) {
      super();
      this.SMTP_AUTH_USER = SMTP_AUTH_USER;
      this.SMTP_AUTH_PWD = SMTP_AUTH_PWD;
    }

    public PasswordAuthentication getPasswordAuthentication() {
      String username = SMTP_AUTH_USER;
      String password = SMTP_AUTH_PWD;
      System.out.println(SMTP_AUTH_USER + ";" + SMTP_AUTH_PWD);
      return new PasswordAuthentication(username, password);
    }
  }


}


