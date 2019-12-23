import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Mean;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

public class DataValidation {
	
  private static final Counter totalRecords = Metrics.counter(ExtractRecordsFn.class, "totalRecords");
  private static final Counter emptyLines = Metrics.counter(ExtractRecordsFn.class, "emptyLines");

  static class ExtractRecordsFn extends DoFn<String, TransactionDetails> {
	private static final long serialVersionUID = 1098768783L;

    @ProcessElement
    public void processElement(@Element String element, OutputReceiver<TransactionDetails> receiver) {
      if (element.trim().isEmpty()) {
        emptyLines.inc();
      } else {
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
    		  t.printStackTrace();
    	  }
      }
    }
  }

  static class ValidateRecordsFn extends DoFn<TransactionDetails, TransactionDetails> {
	private static final long serialVersionUID = 8781739189L;

    @ProcessElement
    public void processElement(@Element TransactionDetails element, OutputReceiver<TransactionDetails> receiver) {
    	// leave out records where Area Code is invalid (i.e. contains a space)
    	if(!element.getAreaCode().contains(" ")) {
    		// remove special characters form Description text
    		TransactionDetails newElement = new TransactionDetails();
    		newElement.setEntityName(element.getEntityName());
    		newElement.setUserID(element.getUserID());
    		newElement.setTransactionRefNo(element.getTransactionRefNo());
    		newElement.setAreaCode(element.getAreaCode());
    		newElement.setTransactionAmount(element.getTransactionAmount());
    		newElement.setDescription(new String(element.getDescription().replaceAll("[^a-zA-Z0-9]", "")));
    		receiver.output(newElement);
    	}
    }
  }

  public static class FormatAsTextFn extends SimpleFunction<TransactionDetails, String> {
	  @Override
	  public String apply(TransactionDetails input) {
	    return input.getEntityName() + ","
    		+ input.getUserID() + ","
    		+ input.getTransactionRefNo() + ","
    		+ input.getAreaCode() + ","
    		+ input.getTransactionAmount() + ","
    		+ input.getDescription();
	  }
  }

  public static class ValidateData
      extends PTransform<PCollection<String>, PCollection<TransactionDetails>> {

	private static final long serialVersionUID = 345367678L;

	@Override
    public PCollection<TransactionDetails> expand(PCollection<String> lines) {

      // Convert lines of text into individual records.
      PCollection<TransactionDetails> records = lines.apply(ParDo.of(new ExtractRecordsFn()));

      // Aggregate all the Average Covered Charges for each state.
      PCollection<TransactionDetails> validatedRecords = records.apply(ParDo.of(new ValidateRecordsFn()));

      return validatedRecords;
    }
  }

  public interface DataValidationOptions extends PipelineOptions {

    @Description("Path of the file to read from")
    @Default.String("gs://apache-beam-samples/shakespeare/kinglear.txt")
    String getInputFile();

    void setInputFile(String value);

    /** Set this required option to specify where to write the output. */
    @Description("Path of the file to write to")
    @Required
    String getOutput();

    void setOutput(String value);
  }

  static void runDataValidation(DataValidationOptions options) {
    Pipeline p = Pipeline.create(options);

    p.apply("ReadLines", TextIO.read().from(options.getInputFile()))
        .apply(new ValidateData())
        .apply(MapElements.via(new FormatAsTextFn()))
        .apply("WriteValidatedData", TextIO.write().to(options.getOutput()));
    	//.apply("WriteSummaryData", TextIO.write().to(options.getOutput()));

    p.run().waitUntilFinish();
  }

  public static void main(String[] args) {
	  DataValidationOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(DataValidationOptions.class);

    runDataValidation(options);
  }
}
