import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySqlIntegration {
  
  private static final Logger LOG = LoggerFactory.getLogger(MySqlIntegration.class);
	
  static class ExtractRecordsFn extends DoFn<String, TransactionDetails> {
	private static final long serialVersionUID = 1098768783L;
	
    @ProcessElement
    public void processElement(@Element String element, OutputReceiver<TransactionDetails> receiver) {
      if (!element.trim().isEmpty()) {
    	  try{
	    	  //System.out.println(element);
	    	  // Split the line into columns.
	    	  String[] tokens = element.split(ExampleUtils.TOKENIZER_CSV_COMMA, -1);
	    	  
	    	  // Output each word encountered into the output PCollection.
	    	  TransactionDetails obj = new TransactionDetails();
	    	  obj.setEntityName(tokens[0]);
	    	  obj.setUserID(Integer.parseInt(tokens[1]));
	    	  obj.setTransactionRefNo(tokens[2]);
	    	  obj.setAreaCode(tokens[3]);
	    	  obj.setTransactionAmount(Double.parseDouble(tokens[4]));
	    	  obj.setDescription(tokens[5]);
	    	  receiver.output(obj);
    	  }catch(Throwable t) {
    		  LOG.error("ERROR: ", t);
    	  }
      }
    }
  }

  static class WriteToMySqlFn extends DoFn<TransactionDetails, TransactionDetails> {
	private static final long serialVersionUID = 8781739189L;
	private static Connection con;

	private String dbURL;
	private String dbUser;
	private String dbPassword;
	
	public WriteToMySqlFn(String dbURL, String dbUser, String dbPassword) {
		this.dbURL = dbURL;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}
	
	private void initializeConnection() {
		if(con == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection(dbURL, dbUser, dbPassword);
			} catch (Exception e) {
				LOG.error("ERROR: ", e);
			}  
		}
	}

    @ProcessElement
    public void processElement(@Element TransactionDetails element, OutputReceiver<TransactionDetails> receiver) {
		initializeConnection();
    	// insert record into database
		try{  
			String sql = "insert into TRANSACTION_DETAILS(ENTITY_NAME, USER_ID, TX_REF_NO, AREA_CODE, TX_AMOUNT, DESCRIPTION) "
					+ "values ('" +element.getEntityName()+ "',"+element.getUserID()+",'" +element.getTransactionRefNo()+ "','"+element.getAreaCode()+"',"+element.getTransactionAmount()+",'"+element.getDescription()+"')";
  		    LOG.info(sql);
  		    Statement stmt = con.createStatement();  
			stmt.executeUpdate(sql);  
		}catch(Exception e){ 
			LOG.error("ERROR: ", e);
		}
    }
  }

  public static class WriteToMySql
      extends PTransform<PCollection<String>, PCollection<TransactionDetails>> {
		
	private static final long serialVersionUID = 345367678L;

	private String dbURL;
	private String dbUser;
	private String dbPassword;
	
	public WriteToMySql(String dbURL, String dbUser, String dbPassword) {
		this.dbURL = dbURL;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}

	@Override
    public PCollection<TransactionDetails> expand(PCollection<String> lines) {

      // Convert lines of text into individual records.
      PCollection<TransactionDetails> records = lines.apply(ParDo.of(new ExtractRecordsFn()));

      // Aggregate all the Average Covered Charges for each state.
      PCollection<TransactionDetails> validatedRecords = records.apply(
    		  ParDo.of(
    				  new WriteToMySqlFn(dbURL, dbUser, dbPassword)));

      return validatedRecords;
    }
  }

  public interface MySqlIntegrationOptions extends PipelineOptions {

    @Description("Path of the file to read from")
    @Default.String("gs://apache-beam-samples/shakespeare/kinglear.txt")
    String getInputFile();
    void setInputFile(String value);

    void setDBURL(String value);
    String getDBURL();

    void setDBUser(String value);
    String getDBUser();

    void setDBPassword(String value);
    String getDBPassword();
  }

  static void runMySqlIntegration(MySqlIntegrationOptions options) {
    Pipeline p = Pipeline.create(options);

    p.apply("ReadLines", TextIO.read().from(options.getInputFile()))
        .apply(new WriteToMySql(options.getDBURL(), options.getDBUser(), options.getDBPassword()));

    p.run().waitUntilFinish();
  }

  public static void main(String[] args) {
	  MySqlIntegrationOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(MySqlIntegrationOptions.class);

    runMySqlIntegration(options);
  }
}

