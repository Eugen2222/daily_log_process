
package Main;

import java.io.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import java.sql.DatabaseMetaData;

import java.sql.PreparedStatement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Properties;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;

import java.util.Map;

import javax.mail.MessagingException;

/**
 ************* DB AP Log to cub log file ************** 1.DB AP log to txt file (overwrite)
 ***********************************************
 **/

public class DailyLogProducer {

  public static boolean tableExists(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();
    ResultSet resultSet =
        meta.getTables(null, null, tableName.toUpperCase(), new String[] {"TABLE"});

    return resultSet.next();
  }



  // private String temContent="";
  // private String []tableSchemaCols;
  public static void exitProgram() {
    // logger.close();
    System.exit(0);
  }

  static {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    System.setProperty("log4jFileName", dateFormat.format(new Date()));
  }

  static ETLlogger logger = new ETLlogger("ETL");

  public static void main(String[] args) {
    logger.info("==========================Start===============================");

    DateFormat emailDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSS");
    Calendar cal = Calendar.getInstance();
    String jobStartTimeString = emailDateFormat.format(cal.getTime());


    Properties config = null;
    logger.info("Try to get config file");

    config = new ReadConfigProperties("config.properties").get();
    if (config == null) {
      logger.error("Failed to open config file.");
      exitProgram();

    } else {

    }



    int rowsOfSource = 0;
    int rowsOfSaving = 0;
    // Setup database connections pool
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    try {
      pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
      pds.setURL(config.getProperty("DB_URL"));
      pds.setUser(config.getProperty("DB_USER"));
      pds.setPassword(config.getProperty("DB_PASSWORD"));
      pds.setInitialPoolSize(5);

      String encodingMode = "";
      String sourceDbTableName = null;
      int housekeepingGap = -1;
      String housekeepingTestingMod = "false";
      String gapTestingMod = "false";
      String targetFileName = null;


      int gap = -1;

      // Init ETL inform and variables
      logger.info("Init ETL inform and variables");
      sourceDbTableName = config.getProperty("sourceDbTableName");
      encodingMode = config.getProperty("encoding");
      targetFileName = config.getProperty("targetFileName");
      gap = Integer.parseInt(config.getProperty("gap"));
      gapTestingMod = config.getProperty("gapTestingMod");
      housekeepingGap = Integer.parseInt(config.getProperty("housekeepingGap"));
      housekeepingTestingMod = config.getProperty("housekeepingTestingMod");
      String flgFileName = config.getProperty("flgFileName");
      String filePath = config.getProperty("filePath");
      // Setup statusTableSchema

      // mapSchemaArray = colsName.split(", ");



      logger.info("Try to connect to db :" + config.getProperty("DB_URL"));
      logger.info("Try to login with :" + config.getProperty("DB_USER"));

      // With AutoCloseable, the connection is closed automatically.
      try (Connection connection = (Connection) pds.getConnection()) {
        // Get the JDBC driver name and version
        DatabaseMetaData dbmd = connection.getMetaData();
        System.out.println("Driver Name: " + dbmd.getDriverName());
        System.out.println("Driver Version: " + dbmd.getDriverVersion());
        if (gapTestingMod.equals("true")) {
          logger.info("Testing===");
          logger.info("Try to insert testing data ");
          gapTestingData(connection, sourceDbTableName, -1);
        }
        logger.info("Check if source db table:"+sourceDbTableName+" exists.");
        rowsOfSource = getCountsInDBTable(connection, sourceDbTableName);
        logger.info("Find " + rowsOfSource + " records in source table");
        logger.info("Try to update target file:" + filePath + targetFileName + ".");
        rowsOfSaving =
            saveGapData(connection, sourceDbTableName, gap, targetFileName, encodingMode);


        logger.info("Totally read and write " + rowsOfSaving + " records.");
        logger.info("Try to create flg file:" + filePath + flgFileName + ".");
        createFlgfile(targetFileName, filePath + flgFileName, "big5");

        logger.info("Completed.");
        logger.info("Try to housekeep.");
        if (housekeepingTestingMod.equals("true")) {
          logger.info("Testing===");
          logger.info("Try to insert testing data ");
          insertHousekeepingTestingData(connection, sourceDbTableName, housekeepingGap);
        }
        int nums_delete = housekeeping(connection, sourceDbTableName, housekeepingGap);
        logger.info("Delete " + nums_delete + " rows.");
        logger.info("Completed.");
      }


      try {
        String jobEndTimeString = emailDateFormat.format(cal.getTime());
        logger.info("Try to send successful email.");
        sendEmail(config, rowsOfSource, rowsOfSaving, jobStartTimeString, jobEndTimeString,
            "Success", ""

        );
        logger.info("Completed.");
      } catch (Exception e2) {
        logger.error(e2.getMessage(),e2);

      }
    } catch (Exception e) {
      logger.error(e.getMessage(),e);

      try {
        String jobEndTimeString = emailDateFormat.format(cal.getTime());
        logger.info("Try to send error email.");
        sendEmail(config, rowsOfSource, rowsOfSaving, jobStartTimeString, jobEndTimeString,
            "Failure", e.toString()

        );
        logger.info("Completed.");
      } catch (Exception e2) {
        logger.error(e2);
      }
    }


    logger.info("All tasks have been completed.");
  }



  // static p p = new p();
  //
  public static void sendEmail(Properties p, int source_row_count, int process_row_count,
      String job_Start_Time, String job_End_Time, String job_Status, String error

  )



      throws UnsupportedEncodingException, GeneralSecurityException, MessagingException {


    ArrayList<String> contentList = new ArrayList<String>();

    contentList.add("Job Name: " + p.getProperty("sourceDbTableName"));

    contentList.add("Source Row Count: " + source_row_count);

    contentList.add("Process Row Count: " + process_row_count);

    contentList.add("Job Start Time: " + job_Start_Time);

    contentList.add("Job End Time: " + job_End_Time);

    contentList.add("Job Status: " + job_Status);

    if (error.length() > 0) {
      contentList.add("Error Message: " + error);

    }

    String type="plain";
    String content = "";
    if(type.equals("html")) {
      
      if (!contentList.isEmpty()) {
        content += "<table><td>";
        for (int i = 0; i < contentList.size(); i++) {
          content += "<tr>";
          content += contentList.get(i);

          content += "</tr>";


        }
        content += "</table></td>";
      }
   
    }else {
      for (int i = 0; i < contentList.size(); i++) {
        content += contentList.get(i);
        if(i< contentList.size()-1)
        content += "\r\n";

      }
      
    }
    
    System.out.println(content);
    EmailModel sender = new EmailModel(p.getProperty("emailSubject"), content, p);
    sender.run();

  }

  /*
   * Displays first_name and last_name from the employees table.
   */
  public static ResultSet getDBTable(Connection connection, String table) throws SQLException {

    try {


      String sql = "Select * " + "From " + table.toUpperCase() + "";

      PreparedStatement ps = connection.prepareStatement(sql);


      ResultSet resultSet = ps.executeQuery();



      return resultSet;
    } finally {

    }
  }

  public static int saveGapData(Connection connection, String table, int gap, String targetFileName,
      String encoding) throws Exception {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = Calendar.getInstance();
    String todayString = dateFormat.format(cal.getTime());
    cal.add(Calendar.DATE, gap);
    String yesterdayString = dateFormat.format(cal.getTime());
    String yesterdayStartString = yesterdayString + "T00:00:00";
    String yesterdayEndString = todayString + "T00:00:00";
    String whereClause = "where DATE_TM >=\'" + yesterdayStartString + "\' AND DATE_TM<\'"
        + yesterdayEndString + "\'";
    logger.debug("Interval:" + gap);



    int batch_rows = 0;
    int num_read_rows = 0;
    int num_write_rows = 0;

    int batch_threshold = 150;
    int size = 0;
    String sql = "Select * " + "From " + table.toUpperCase() + " " + whereClause;
    logger.debug(sql);

    try (PreparedStatement ps = connection.prepareStatement(sql);) {
      ResultSet resultSet = ps.executeQuery();
      ResultSetMetaData meta = resultSet.getMetaData();

      int num_cols = meta.getColumnCount();


      try (BufferedWriter br = new BufferedWriter(new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(targetFileName), encoding)));) {
        while (resultSet.next()) {
          String file_string = "";

          for (int i = 1; i <= num_cols; i++) {
            file_string += resultSet.getString(i);
            if (i < num_cols) {
              file_string += "$^$";
            }
          }
          br.write(file_string);
          br.newLine();

          batch_rows++;
          if (batch_rows >= batch_threshold) {
            br.flush();
            logger.debug("Write " + batch_rows + " records.");
            num_write_rows += batch_rows;
            batch_rows = 0;

          }
        }
        if (batch_rows > 0) {
          logger.debug("Write " + batch_rows + " records.");
          br.flush();
          num_write_rows += batch_rows;
        }

      }
    } catch (Exception e) {
      logger.error(e.getMessage(),e);
      throw e;

    }



    return num_write_rows;



  }

  public static void createFlgfile(String targetFileName, String flgFileName, String encoding)
      throws Exception {



    try (BufferedWriter br = new BufferedWriter(
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(flgFileName), encoding)));) {


      br.write(targetFileName);
      br.newLine();

    } catch (Exception e) {

      throw e;

    }



  }

  public static int housekeeping(Connection connection, String table, int housekeeping_gap)
      throws SQLException {

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = Calendar.getInstance();

    cal.add(Calendar.YEAR, housekeeping_gap);
    cal.add(Calendar.DATE, -1);
    String housekeepingDayString = dateFormat.format(cal.getTime());
    housekeepingDayString = housekeepingDayString + "T23:59:59";

    String whereClause = "where DATE_TM <=\'" + housekeepingDayString + "\'";
    logger.debug("Housekeeping Interval:" + housekeeping_gap + " years.");

    int nums_delete = deleteRowsFromDbTable(connection, table, whereClause);
    return nums_delete;

  }



  public static ResultSet getDBTable(Connection connection, String table, String whereClause)
      throws SQLException {

    String sql = "Select * " + "From " + table.toUpperCase() + " " + whereClause;
    logger.debug(sql);
    try (PreparedStatement ps = connection.prepareStatement(sql);) {
      ResultSet resultSet = ps.executeQuery();
      return resultSet;
    }
  }


  public static int getCountsInDBTable(Connection connection, String table) throws SQLException {
    String sql = "Select count(*) " + "From " + table.toUpperCase() + "";

    try (PreparedStatement ps = connection.prepareStatement(sql);) {
      ResultSet rs = ps.executeQuery();
      rs.next();
      int count = rs.getInt(1);

      return count;
    }
  }

  public static int deleteRowsFromDbTable(Connection connection, String dbTableName,
      String whereClause) throws SQLException {

    String sqlQuery = " DELETE FROM " + dbTableName + " " + whereClause;
    int num_delete = 0;
    logger.debug(sqlQuery);
    try (PreparedStatement pstmt = connection.prepareStatement(sqlQuery);) {
      num_delete = pstmt.executeUpdate(sqlQuery);

    } catch (Exception e) {
      e.printStackTrace();
      connection.rollback();
      throw e;
    }
    return num_delete;

  }


  public static int gapTestingData(Connection connection, String dbTableName, int gap)
      throws Exception {


    String sqlQuery = "SELECT * FROM " + dbTableName;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = Calendar.getInstance();

    cal.add(Calendar.DATE, gap);
    String housekeepingDayString = dateFormat.format(cal.getTime());
    housekeepingDayString = housekeepingDayString + "T23:59:59";
    int nums_row = 100;
    try (PreparedStatement pstmt = connection.prepareStatement(sqlQuery);
        ResultSet rs = pstmt.executeQuery(sqlQuery);) {

      sqlQuery = "insert into " + dbTableName + " VALUES ";
      ResultSetMetaData rsmd = rs.getMetaData();
      int nums_col = rsmd.getColumnCount();


      sqlQuery += "(";
      for (int i = 0; i < nums_col; i++) {
        if (i == 2) {
          sqlQuery += "\'" + housekeepingDayString + "\'";
        } else if (i == 12) {
          sqlQuery += 0;

        } else {
          sqlQuery += "\' \'";
        }
        if (i < nums_col - 1) {
          sqlQuery += ", ";
        }

      }
      sqlQuery += ")";



    }

    int nums_insert = 0;

    logger.debug(sqlQuery);

    try (PreparedStatement pstmt = connection.prepareStatement(sqlQuery);) {

      connection.setAutoCommit(false);
      for (int j = 0; j < nums_row; j++) {
        pstmt.addBatch();
      }
      nums_insert = pstmt.executeBatch().length;

      connection.commit();
      connection.setAutoCommit(true);
      logger.info("Insert " + nums_insert + " rows with datetime " + housekeepingDayString);
    } catch (Exception e) {
      logger.error(e.getMessage(),e);
      try {
        connection.rollback();
        connection.setAutoCommit(true);
      } catch (Exception e2) {
        logger.error(e2.getMessage(),e2);
        throw e2;

      }

    }
    return nums_insert;
  }



  public static int insertHousekeepingTestingData(Connection connection, String dbTableName,
      int housekeeping_gap) throws Exception {


    String sqlQuery = "SELECT * FROM " + dbTableName;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = Calendar.getInstance();

    cal.add(Calendar.YEAR, housekeeping_gap);
    cal.add(Calendar.DATE, -1);
    String housekeepingDayString = dateFormat.format(cal.getTime());
    housekeepingDayString = housekeepingDayString + "T23:59:59";
    int nums_row = 100;
    try (PreparedStatement pstmt = connection.prepareStatement(sqlQuery);
        ResultSet rs = pstmt.executeQuery(sqlQuery);) {

      sqlQuery = "insert into " + dbTableName + " VALUES ";
      ResultSetMetaData rsmd = rs.getMetaData();
      int nums_col = rsmd.getColumnCount();


      sqlQuery += "(";
      for (int i = 0; i < nums_col; i++) {
        if (i == 2) {
          sqlQuery += "\'" + housekeepingDayString + "\'";
        } else if (i == 12) {
          sqlQuery += 0;

        } else {
          sqlQuery += "\' \'";
        }
        if (i < nums_col - 1) {
          sqlQuery += ", ";
        }

      }
      sqlQuery += ")";



    }

    int nums_insert = 0;

    logger.debug(sqlQuery);

    try (PreparedStatement pstmt = connection.prepareStatement(sqlQuery);) {

      connection.setAutoCommit(false);
      for (int j = 0; j < nums_row; j++) {
        pstmt.addBatch();
      }
      nums_insert = pstmt.executeBatch().length;

      connection.commit();
      connection.setAutoCommit(true);
      logger.info("Insert " + nums_insert + " rows with datetime " + housekeepingDayString);
    } catch (Exception e) {
      logger.error(e.getMessage(),e);
      try {
        connection.rollback();
        connection.setAutoCommit(true);
      } catch (Exception e2) {
        logger.error(e2.getMessage(),e2);
        throw e2;

      }

    }
    return nums_insert;
  }

}

